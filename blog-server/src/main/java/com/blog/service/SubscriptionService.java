package com.blog.service;

import com.blog.common.BizException;
import com.blog.entity.EmailSubscription;
import com.blog.mapper.EmailSubscriptionMapper;
import com.blog.security.ClientIpResolver;
import com.blog.security.RateLimiter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邮箱订阅业务。
 *
 * <p>设计：</p>
 * <ul>
 *   <li>双重确认(double opt-in)：提交 → 落库待确认 + 发确认邮件 → 点击链接置已确认</li>
 *   <li>若 SMTP 未启用(blog.mail.enabled=false)，无法发确认邮件，则提交即直接置已确认，保证功能始终可用</li>
 *   <li>同邮箱重复订阅：已确认 → 提示无需重复；待确认 → 重发确认邮件（或离线时直接确认）</li>
 *   <li>公开接口必须限频，防批量灌水/滥用他人邮箱</li>
 * </ul>
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final EmailSubscriptionMapper mapper;
    private final MailService mailService;
    private final RateLimiter rateLimiter;
    private final ClientIpResolver ipResolver;
    private final boolean mailEnabled;
    private final String confirmBaseUrl;
    private final String siteBaseUrl;
    private final String siteName;

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_UNSUBSCRIBED = 2;

    public SubscriptionService(EmailSubscriptionMapper mapper,
                               MailService mailService,
                               RateLimiter rateLimiter,
                               ClientIpResolver ipResolver,
                               @Value("${blog.mail.enabled:false}") boolean mailEnabled,
                               @Value("${blog.subscribe.confirm-base-url:}") String confirmBaseUrl,
                               @Value("${blog.site-base-url:}") String siteBaseUrl,
                               @Value("${blog.subscribe.site-name:MyBlog}") String siteName) {
        this.mapper = mapper;
        this.mailService = mailService;
        this.rateLimiter = rateLimiter;
        this.ipResolver = ipResolver;
        this.mailEnabled = mailEnabled;
        this.confirmBaseUrl = confirmBaseUrl;
        this.siteBaseUrl = siteBaseUrl;
        this.siteName = siteName;
    }

    public Map<String, Object> subscribe(String rawEmail, String source, HttpServletRequest req) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (email.isEmpty() || !EMAIL.matcher(email).matches()) {
            throw new BizException("请输入有效的邮箱地址");
        }

        // 公开接口限频：同 IP 60 秒内最多 5 次
        rateLimiter.acquireOrThrow("subscribe:ip:" + ipResolver.resolve(req), 5, 60);

        Map<String, Object> result = new HashMap<>();

        // 关键修复：用“绕过逻辑删除”的原始查询做预检。
        // 若用 selectOne，已被退订/删除(deleted=1)的邮箱对其不可见，会落到下方 INSERT
        // 撞唯一索引 uk_email → DuplicateKeyException → 500。
        EmailSubscription existing = mapper.selectRawByEmail(email);
        if (existing != null) {
            handleExisting(email, existing, source, req, result);
            return result;
        }

        // 新订阅
        EmailSubscription sub = new EmailSubscription();
        sub.setEmail(email);
        sub.setSource(source);
        sub.setStatus(mailEnabled ? STATUS_PENDING : STATUS_CONFIRMED);
        sub.setToken(UUID.randomUUID().toString().replace("-", ""));
        sub.setCreateTime(LocalDateTime.now());
        if (!mailEnabled) sub.setConfirmTime(LocalDateTime.now());
        try {
            mapper.insert(sub);
        } catch (DuplicateKeyException e) {
            // 并发竞态：两个相同邮箱请求同时通过上面的预检，其中一个 INSERT 撞唯一索引 uk_email。
            // 当作“已存在”处理，重发确认信 / 直接确认，避免 500。
            EmailSubscription race = mapper.selectRawByEmail(email);
            if (race != null) {
                handleExisting(email, race, source, req, result);
                return result;
            }
            throw e; // 极端兜底：理论上不会到这里
        }

        if (mailEnabled) {
            boolean sent = sendConfirmEmail(sub, req);
            result.put("subscribed", true);
            result.put("pending", true);
            result.put("emailSent", sent);
            result.put("message", sent
                    ? "订阅请求已提交，请查收邮箱中的确认链接以完成订阅"
                    : "确认邮件发送失败，请稍后重试或联系管理员");
        } else {
            // 离线（SMTP 未启用）：直接确认，无需发信。
            // 不设 emailSent（undefined），前端 data.emailSent !== false → 视为成功，
            // 不会误报"确认邮件发送失败"（离线模式本就不需要确认邮件）。
            result.put("subscribed", true);
            result.put("message", "订阅成功");
        }
        return result;
    }

    /**
     * 处理“邮箱已存在”分支：已确认且未删除 → 提示无需重复；
     * 待确认 / 曾被退订删除 → 重新激活并重发确认信（或离线直接确认）。
     */
    private void handleExisting(String email, EmailSubscription existing, String source,
                               HttpServletRequest req, Map<String, Object> result) {
        boolean active = existing.getDeleted() == null || existing.getDeleted() == 0;
        if (active && existing.getStatus() == STATUS_CONFIRMED) {
            result.put("subscribed", true);
            result.put("alreadySubscribed", true);
            result.put("message", "该邮箱已订阅，无需重复订阅");
            return;
        }
        // 待确认、或曾被退订/删除：重新生成令牌并激活，重发确认邮件（或离线直接确认）
        if (mailEnabled) {
            String token = UUID.randomUUID().toString().replace("-", "");
            mapper.updateRawByEmail(email, STATUS_PENDING, token, source, LocalDateTime.now(), null);
            EmailSubscription toSend = mapper.selectRawByEmail(email);
            boolean sent = toSend != null && sendConfirmEmail(toSend, req);
            result.put("subscribed", true);
            result.put("pending", true);
            result.put("emailSent", sent);
            result.put("message", sent
                    ? "确认邮件已重新发送，请查收邮箱完成订阅"
                    : "确认邮件发送失败，请稍后重试或联系管理员");
            return;
        }
        // 离线（SMTP 未启用）：直接置已确认
        mapper.updateRawByEmail(email, STATUS_CONFIRMED, existing.getToken(), source,
                existing.getCreateTime(), LocalDateTime.now());
        result.put("subscribed", true);
        result.put("message", "订阅成功");
    }

    private boolean sendConfirmEmail(EmailSubscription sub, HttpServletRequest req) {
        // 确认链接基础地址：优先用显式配置的 confirm-base-url；其次用服务端固定 site-base-url；
        // 二者都未配置时才退回请求 Host 头（可被客户端伪造，仅作兜底并告警，强烈建议配置前两项之一）。
        String base;
        if (confirmBaseUrl != null && !confirmBaseUrl.isBlank()) {
            base = confirmBaseUrl;
        } else if (siteBaseUrl != null && !siteBaseUrl.isBlank()) {
            base = siteBaseUrl;
        } else {
            log.warn("[Subscription] blog.subscribe.confirm-base-url / blog.site.base-url 均未配置，"
                    + "确认链接退回使用请求 Host 头（可被伪造，存在钓鱼风险），请尽快在配置中设置站点根地址");
            base = req.getScheme() + "://" + req.getHeader("Host");
        }
        base = base.replaceAll("/+$", "");
        String link = base + "/api/v1/subscribe/confirm?token=" + sub.getToken();

        String subject = "【" + escapeHtml(siteName) + "】请确认你的邮箱订阅";
        String text = "感谢订阅 " + siteName + "！\n请点击下面的链接确认订阅：\n" + link
                + "\n如果这不是你本人的操作，忽略此邮件即可。";
        String html = "<div style='font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#0f172a'>"
                + "<h2 style='color:#0369a1'>确认你的邮箱订阅</h2>"
                + "<p>感谢订阅 <b>" + escapeHtml(siteName) + "</b> 的更新。</p>"
                + "<p>点击下面的按钮完成订阅确认：</p>"
                + "<p style='margin:24px 0'><a href='" + link + "' style='background:#0ea5e9;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;display:inline-block'>确认订阅</a></p>"
                + "<p style='color:#64748b;font-size:13px'>如果按钮无法点击，请复制以下链接到浏览器打开：<br>" + link + "</p>"
                + "<p style='color:#94a3b8;font-size:12px'>如果这不是你本人的操作，忽略此邮件即可。</p>"
                + "</div>";
        return mailService.sendSync(sub.getEmail(), subject, text, html);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** 点击邮件链接确认订阅，返回自包含的 HTML 页面（无需前端路由） */
    public String confirm(String token) {
        if (token == null || token.isBlank()) {
            return confirmHtml(false, "无效的确认链接");
        }
        LambdaQueryWrapper<EmailSubscription> q = new LambdaQueryWrapper<>();
        q.eq(EmailSubscription::getToken, token);
        EmailSubscription sub = mapper.selectOne(q);
        if (sub == null) {
            return confirmHtml(false, "无效的确认链接或链接已失效");
        }
        if (sub.getStatus() == STATUS_CONFIRMED) {
            return confirmHtml(true, "该邮箱已确认订阅，无需重复操作");
        }
        sub.setStatus(STATUS_CONFIRMED);
        sub.setConfirmTime(LocalDateTime.now());
        mapper.updateById(sub);
        return confirmHtml(true, "订阅确认成功，感谢你的关注！");
    }

    private String confirmHtml(boolean ok, String msg) {
        String color = ok ? "#0ea5e9" : "#dc2626";
        String icon = ok ? "✓" : "✕";
        String title = ok ? "订阅成功" : "无法确认";
        return "<!doctype html><html lang='zh-CN'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>邮箱订阅确认</title></head>"
                + "<body style='margin:0;background:#f0f9ff;font-family:-apple-system,Segoe UI,Roboto,sans-serif'>"
                + "<div style='max-width:420px;margin:80px auto;background:#fff;border-radius:16px;padding:40px 32px;text-align:center;box-shadow:0 10px 30px rgba(2,132,199,.12)'>"
                + "<div style='width:56px;height:56px;border-radius:50%;background:" + color + ";color:#fff;font-size:28px;line-height:56px;margin:0 auto 16px'>" + icon + "</div>"
                + "<h2 style='color:#0f172a;margin:0 0 8px'>" + title + "</h2>"
                + "<p style='color:#475569;line-height:1.7;margin:0'>" + msg + "</p>"
                + "<p style='color:#94a3b8;font-size:12px;margin-top:24px'>— " + siteName + " —</p>"
                + "</div></body></html>";
    }

    // ============================================================
    //  后台管理（订阅者列表 / 统计 / 增删 / 导出）
    //  安全由 SecurityConfig 统一保障：/api/v1/admin/** 需 ROLE_ADMIN
    // ============================================================

    /** 后台分页列表：支持邮箱关键字模糊搜索 + 状态过滤 */
    public Page<EmailSubscription> adminPage(long page, long size, String keyword, Integer status) {
        Page<EmailSubscription> p = Page.of(page, size);
        LambdaQueryWrapper<EmailSubscription> w = new LambdaQueryWrapper<EmailSubscription>()
                .orderByDesc(EmailSubscription::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            w.like(EmailSubscription::getEmail, keyword.trim());
        }
        if (status != null) w.eq(EmailSubscription::getStatus, status);
        return mapper.selectPage(p, w);
    }

    /** 后台统计：总数 / 已确认 / 待确认 / 今日新增 / 近 7 天新增 */
    public Map<String, Long> adminStats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("total", mapper.selectCount(new LambdaQueryWrapper<>()));
        m.put("confirmed", mapper.selectCount(
                new LambdaQueryWrapper<EmailSubscription>().eq(EmailSubscription::getStatus, STATUS_CONFIRMED)));
        m.put("pending", mapper.selectCount(
                new LambdaQueryWrapper<EmailSubscription>().eq(EmailSubscription::getStatus, STATUS_PENDING)));
        m.put("unsubscribed", mapper.selectCount(
                new LambdaQueryWrapper<EmailSubscription>().eq(EmailSubscription::getStatus, STATUS_UNSUBSCRIBED)));
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        m.put("todayNew", mapper.selectCount(
                new LambdaQueryWrapper<EmailSubscription>().ge(EmailSubscription::getCreateTime, startOfDay)));
        m.put("weekNew", mapper.selectCount(
                new LambdaQueryWrapper<EmailSubscription>().ge(EmailSubscription::getCreateTime, LocalDateTime.now().minusDays(7))));
        return m;
    }

    /** 后台新增订阅：管理员添加视为可信，直接置已确认（不发送确认邮件） */
    public EmailSubscription adminCreate(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (email.isEmpty() || !EMAIL.matcher(email).matches()) {
            throw new BizException("请输入有效的邮箱地址");
        }
        // 用原始查询预检，避免已删除(deleted=1)记录对 selectOne 不可见导致下方 INSERT 撞唯一索引
        EmailSubscription existing = mapper.selectRawByEmail(email);
        if (existing != null) {
            boolean active = existing.getDeleted() == null || existing.getDeleted() == 0;
            if (active && existing.getStatus() == STATUS_CONFIRMED) {
                throw new BizException("该邮箱已订阅");
            }
            // 待确认的、或已删除的：直接转为已确认（管理员后台添加，视为可信）
            activateToConfirmed(email, existing);
            return mapper.selectRawByEmail(email);
        }
        EmailSubscription sub = new EmailSubscription();
        sub.setEmail(email);
        sub.setSource("admin");
        sub.setStatus(STATUS_CONFIRMED);
        sub.setToken(UUID.randomUUID().toString().replace("-", ""));
        sub.setCreateTime(LocalDateTime.now());
        sub.setConfirmTime(LocalDateTime.now());
        try {
            mapper.insert(sub);
        } catch (DuplicateKeyException e) {
            // 并发新增同一邮箱：撞唯一索引 uk_email，当作已存在处理
            EmailSubscription race = mapper.selectRawByEmail(email);
            if (race != null) {
                activateToConfirmed(email, race);
                return mapper.selectRawByEmail(email);
            }
            throw e;
        }
        return sub;
    }

    /** 将一条（待确认 / 已退订 / 已删除）记录激活为已确认，后台添加可信场景复用 */
    private void activateToConfirmed(String email, EmailSubscription existing) {
        String token = existing.getToken();
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString().replace("-", "");
        }
        mapper.updateRawByEmail(email, STATUS_CONFIRMED, token, "admin",
                existing.getCreateTime(), LocalDateTime.now());
    }

    /** 后台删除：彻底移除记录（真删除，区别于“退订”保留记录） */
    public void adminDelete(Long id) {
        mapper.physicalDeleteById(id);
    }

    /** 后台退订：保留记录，仅标记为已退订(status=2)，不再推送邮件 */
    public void adminUnsubscribe(Long id) {
        EmailSubscription sub = mapper.selectById(id);
        if (sub == null) throw new BizException("订阅记录不存在");
        sub.setStatus(STATUS_UNSUBSCRIBED);
        sub.setDeleted(0);
        mapper.updateById(sub);
    }

    /** 后台手动确认订阅 */
    public void adminConfirm(Long id) {
        EmailSubscription sub = mapper.selectById(id);
        if (sub == null) throw new BizException("订阅记录不存在");
        sub.setStatus(STATUS_CONFIRMED);
        sub.setConfirmTime(LocalDateTime.now());
        mapper.updateById(sub);
    }

    /** 退订：点击邮件链接，逻辑删除订阅记录（不再出现在已确认名单） */
    public String unsubscribe(String token) {
        if (token == null || token.isBlank()) {
            return unsubHtml(false, "无效的退订链接");
        }
        LambdaQueryWrapper<EmailSubscription> q = new LambdaQueryWrapper<>();
        q.eq(EmailSubscription::getToken, token);
        EmailSubscription sub = mapper.selectOne(q);
        if (sub == null) {
            return unsubHtml(false, "链接无效，或你已退订");
        }
        if (sub.getStatus() == STATUS_UNSUBSCRIBED) {
            return unsubHtml(true, "你已退订，不会再收到 " + siteName + " 的更新邮件。");
        }
        // 退订：保留记录，仅标记为已退订(status=2)，不再推送；区别于后台“删除”(真删除)
        sub.setStatus(STATUS_UNSUBSCRIBED);
        sub.setDeleted(0);
        mapper.updateById(sub);
        return unsubHtml(true, "已成功退订，你不会再收到 " + siteName + " 的更新邮件。");
    }

    private String unsubHtml(boolean ok, String msg) {
        String color = ok ? "#0ea5e9" : "#dc2626";
        String icon = ok ? "✓" : "✕";
        String title = ok ? "退订成功" : "无法退订";
        return "<!doctype html><html lang='zh-CN'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>邮箱退订</title></head>"
                + "<body style='margin:0;background:#f0f9ff;font-family:-apple-system,Segoe UI,Roboto,sans-serif'>"
                + "<div style='max-width:420px;margin:80px auto;background:#fff;border-radius:16px;padding:40px 32px;text-align:center;box-shadow:0 10px 30px rgba(2,132,199,.12)'>"
                + "<div style='width:56px;height:56px;border-radius:50%;background:" + color + ";color:#fff;font-size:28px;line-height:56px;margin:0 auto 16px'>" + icon + "</div>"
                + "<h2 style='color:#0f172a;margin:0 0 8px'>" + title + "</h2>"
                + "<p style='color:#475569;line-height:1.7;margin:0'>" + msg + "</p>"
                + "<p style='color:#94a3b8;font-size:12px;margin-top:24px'>— " + siteName + " —</p>"
                + "</div></body></html>";
    }

    /** 导出 CSV（UTF-8 BOM，便于 Excel 直接打开） */
    public String exportCsv() {
        List<EmailSubscription> all = mapper.selectList(
                new LambdaQueryWrapper<EmailSubscription>().orderByDesc(EmailSubscription::getCreateTime));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("﻿"); // UTF-8 BOM
        sb.append("email,status,source,create_time,confirm_time\n");
        for (EmailSubscription s : all) {
            // CSV 公式注入防护：以 = + - @ 开头的单元格在 Excel 中会被当作公式执行，
            // 前置单引号强制按文本处理（Excel 会隐藏该引号）。
            String email = s.getEmail() == null ? "" : s.getEmail();
            if (!email.isEmpty() && "+-@=".indexOf(email.charAt(0)) >= 0) email = "'" + email;
            sb.append(escapeCsv(email)).append(',')
              .append(s.getStatus() == STATUS_CONFIRMED ? "已确认"
                    : s.getStatus() == STATUS_UNSUBSCRIBED ? "已退订" : "待确认").append(',')
              .append(escapeCsv(s.getSource())).append(',')
              .append(s.getCreateTime() == null ? "" : s.getCreateTime().format(fmt)).append(',')
              .append(s.getConfirmTime() == null ? "" : s.getConfirmTime().format(fmt)).append('\n');
        }
        return sb.toString();
    }

    private static String escapeCsv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
