package com.blog.service;

import com.blog.config.BlogProperties;
import com.blog.entity.FriendLink;
import com.blog.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 友链申请通知业务。
 *
 * <p>友链申请入库成功后触发 — 给管理员发"有新友链待审核"邮件,
 * 管理员不必主动刷后台就知道有人申请交换友链。</p>
 *
 * <p>防滥用 / 越权(沿用 CommentNotificationService 的思路):</p>
 * <ul>
 *   <li>收件邮箱(站长)格式无效 → 跳过</li>
 *   <li>站点配置 friendLinkNotifyEnabled != "1" → 跳过</li>
 *   <li>邮件开关关闭 / SMTP 不可达 → MailService.send 内部兜底,不抛异常</li>
 *   <li>整个方法 @Async + 内部 try/catch,不阻塞申请接口响应</li>
 * </ul>
 *
 * <p>申请接口本身已带限流(同 IP 3次/小时、同邮箱 1次/天),
 * 因此这里无需再对收件人(站长)做额外限频。</p>
 */
@Service
public class FriendLinkNotificationService {
    private static final Logger log = LoggerFactory.getLogger(FriendLinkNotificationService.class);

    private static final Pattern EMAIL_RE = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** 后台审核入口路径,基于 blog.site.url 拼接(避免源码里硬编码域名)。 */
    private static final String ADMIN_PATH = "/admin/friend-links";

    private final SiteConfigService siteConfigService;
    private final MailService mailService;
    private final String adminEmail;
    private final String adminUrl;

    public FriendLinkNotificationService(SiteConfigService siteConfigService,
                                          MailService mailService,
                                          BlogProperties blogProperties,
                                          @Value("${blog.mail.admin-email:}") String adminEmail) {
        this.siteConfigService = siteConfigService;
        this.mailService = mailService;
        this.adminEmail = adminEmail;
        String base = blogProperties.getSite().getUrl();
        this.adminUrl = (base == null ? "" : base.replaceAll("/+$", "")) + ADMIN_PATH;
    }

    /**
     * 友链申请入库后调用 — 给管理员发"待审核"通知。
     * 整个方法 @Async,不阻塞申请提交响应。
     */
    @Async("mailTaskExecutor")
    public void onApplied(FriendLink f) {
        if (f == null || f.getId() == null) return;
        try {
            if (!"1".equals(siteConfigService.get("friendLinkNotifyEnabled", "1"))) return;
            if (!isValidEmail(adminEmail)) return;

            String subject = "[友链待审核] 收到来自 " + safe(f.getName()) + " 的友链申请";
            String text = String.format(
                    "有人提交了友链申请,等待你审核:\n\n" +
                            "站点名称: %s\n" +
                            "站点链接: %s\n" +
                            "头像链接: %s\n" +
                            "站点简介: %s\n" +
                            "联系邮箱: %s\n" +
                            "申请编号: #%s\n" +
                            "提交时间: %s\n\n" +
                            "去后台审核: %s\n\n" +
                            "— DevCoding Blog",
                    safe(f.getName()),
                    safe(f.getUrl()),
                    f.getAvatar() == null || f.getAvatar().isBlank() ? "（未提供）" : f.getAvatar(),
                    safe(f.getDescription()),
                    f.getEmail() == null || f.getEmail().isBlank() ? "（未提供）" : f.getEmail(),
                    f.getId(),
                    f.getCreateTime() == null ? "（未知）" : f.getCreateTime().toString(),
                    adminUrl);
            mailService.send(adminEmail, subject, text, null);
        } catch (Exception e) {
            log.warn("[FriendLinkNotify] onApplied 失败 id={} err={}", f.getId(), e.toString());
        }
    }

    // ────────────────────────────────────── 工具 ──────────────────────────────────────

    private boolean isValidEmail(String s) {
        return s != null && !s.isBlank() && EMAIL_RE.matcher(s).matches();
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n]", " ");
    }
}
