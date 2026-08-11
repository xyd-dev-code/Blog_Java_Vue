package com.blog.service;

import com.blog.entity.Article;
import com.blog.entity.EmailSubscription;
import com.blog.entity.Project;
import com.blog.entity.Tool;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.EmailSubscriptionMapper;
import com.blog.mapper.ProjectMapper;
import com.blog.mapper.ToolMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自动化邮件推送：检测"新文章发布 / 项目新增 / 工具新增"三类事件，
 * 向所有<b>已确认</b>订阅者发送更新提醒邮件。
 *
 * <p>设计要点:</p>
 * <ul>
 *   <li>去重靠各表 notified 标记：扫描 status=正常/已发布 且 notified=0 的记录，推送后置 1，避免重复推送</li>
 *   <li>仅已确认(status=1)订阅者纳入接收名单（双重确认未完成者不收）</li>
 *   <li>邮件含更新摘要 + 查看链接 + 退订链接</li>
 *   <li>mailEnabled=false 时<b>不标记</b> notified，待 SMTP 启用后自动补推，避免漏发</li>
 *   <li>blog.notify.base-url 未配置则跳过（调度无 request 上下文，必须显式配置站点根地址）</li>
 * </ul>
 */
@Service
public class NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);

    private final ArticleMapper articleMapper;
    private final ProjectMapper projectMapper;
    private final ToolMapper toolMapper;
    private final EmailSubscriptionMapper subMapper;
    private final MailService mailService;

    private final boolean mailEnabled;
    private final String siteBaseUrl;
    private final String siteName;

    /** status=1 表示"已发布/正常"，作为"可推送"的判定值（草稿/下线不推送） */
    private static final int ACTIVE = 1;

    public NotifyService(ArticleMapper articleMapper,
                         ProjectMapper projectMapper,
                         ToolMapper toolMapper,
                         EmailSubscriptionMapper subMapper,
                         MailService mailService,
                         @Value("${blog.mail.enabled:false}") boolean mailEnabled,
                         @Value("${blog.notify.base-url:}") String siteBaseUrl,
                         @Value("${blog.subscribe.site-name:MyBlog}") String siteName) {
        this.articleMapper = articleMapper;
        this.projectMapper = projectMapper;
        this.toolMapper = toolMapper;
        this.subMapper = subMapper;
        this.mailService = mailService;
        this.mailEnabled = mailEnabled;
        this.siteBaseUrl = siteBaseUrl;
        this.siteName = siteName;
    }

    /** 调度入口：检测并推送所有待通知内容 */
    public void notifyPending() {
        if (!mailEnabled) {
            log.debug("[Notify] 邮件未启用(blog.mail.enabled=false),跳过自动推送;不标记 notified,待启用后自动补推");
            return;
        }
        if (!StringUtils.hasText(siteBaseUrl)) {
            log.warn("[Notify] blog.notify.base-url 未配置,无法构建查看/退订链接,跳过本次推送");
            return;
        }
        String base = siteBaseUrl.replaceAll("/+$", "");

        List<EmailSubscription> subs = confirmedSubscribers();
        int a = notifyArticles(base, subs);
        int p = notifyProjects(base, subs);
        int t = notifyTools(base, subs);
        if (a + p + t > 0) {
            int batches = subs.size() == 0 ? 1 : subs.size();
            log.info("[Notify] 推送完成 → 文章{}篇 / 项目{}个 / 工具{}个,向{}位订阅者各发送对应邮件",
                    a / batches, p / batches, t / batches, subs.size());
        } else {
            log.debug("[Notify] 暂无待推送内容");
        }
    }

    /** 仅已确认且未逻辑删除的订阅者 */
    private List<EmailSubscription> confirmedSubscribers() {
        return subMapper.selectList(new LambdaQueryWrapper<EmailSubscription>()
                .eq(EmailSubscription::getStatus, 1)
                .eq(EmailSubscription::getDeleted, 0));
    }

    private int notifyArticles(String base, List<EmailSubscription> subs) {
        List<Article> list = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ACTIVE)
                .eq(Article::getNotified, 0)
                .eq(Article::getDeleted, 0)
                .orderByAsc(Article::getPublishTime));
        if (list.isEmpty()) return 0;
        if (subs.isEmpty()) { markArticles(list); return 0; }
        String unsub = base + "/api/v1/subscribe/unsubscribe";
        boolean allSent = true;
        for (Article a : list) {
            String slug = a.getSlug() != null && !a.getSlug().isBlank() ? a.getSlug() : String.valueOf(a.getId());
            String url = base + "/articles/" + slug;
            String summary = a.getSummary() != null ? a.getSummary() : "";
            for (EmailSubscription s : subs) {
                String subject = "【" + siteName + "】新文章发布：" + a.getTitle();
                String text = "你好，" + siteName + " 发布了新文章《" + a.getTitle() + "》。\n\n"
                        + "摘要：\n" + summary + "\n\n查看：" + url
                        + "\n退订：" + unsub + "?token=" + s.getToken();
                String html = buildHtml("新文章发布", a.getTitle(), summary, url, "阅读全文", s.getToken(), unsub);
                if (!mailService.sendSync(s.getEmail(), subject, text, html)) allSent = false;
            }
        }
        // 仅当全部发送成功才标记 notified=1；否则保留 notified=0,待下次调度重试,避免 SMTP 抖动导致邮件永久丢失
        if (allSent) {
            markArticles(list);
        } else {
            log.warn("[Notify] 文章推送存在发送失败,保留 notified=0 待下次重试(共{}篇)", list.size());
        }
        return list.size() * subs.size();
    }

    private int notifyProjects(String base, List<EmailSubscription> subs) {
        List<Project> list = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getStatus, ACTIVE)
                .eq(Project::getNotified, 0)
                .eq(Project::getDeleted, 0)
                .orderByAsc(Project::getCreateTime));
        if (list.isEmpty()) return 0;
        if (subs.isEmpty()) { markProjects(list); return 0; }
        String unsub = base + "/api/v1/subscribe/unsubscribe";
        String url = base + "/projects";
        boolean allSent = true;
        for (Project p : list) {
            String summary = p.getDescription() != null ? p.getDescription() : "";
            for (EmailSubscription s : subs) {
                String subject = "【" + siteName + "】新项目上线：" + p.getName();
                String text = "你好，" + siteName + " 新增了项目《" + p.getName() + "》。\n\n"
                        + "简介：\n" + summary + "\n\n查看：" + url
                        + "\n退订：" + unsub + "?token=" + s.getToken();
                String html = buildHtml("新项目上线", p.getName(), summary, url, "查看项目", s.getToken(), unsub);
                if (!mailService.sendSync(s.getEmail(), subject, text, html)) allSent = false;
            }
        }
        if (allSent) {
            markProjects(list);
        } else {
            log.warn("[Notify] 项目推送存在发送失败,保留 notified=0 待下次重试(共{}个)", list.size());
        }
        return list.size() * subs.size();
    }

    private int notifyTools(String base, List<EmailSubscription> subs) {
        List<Tool> list = toolMapper.selectList(new LambdaQueryWrapper<Tool>()
                .eq(Tool::getStatus, ACTIVE)
                .eq(Tool::getNotified, 0)
                .eq(Tool::getDeleted, 0)
                .orderByAsc(Tool::getCreateTime));
        if (list.isEmpty()) return 0;
        if (subs.isEmpty()) { markTools(list); return 0; }
        String unsub = base + "/api/v1/subscribe/unsubscribe";
        String url = base + "/tools";
        boolean allSent = true;
        for (Tool tl : list) {
            String summary = tl.getDescription() != null ? tl.getDescription() : "";
            for (EmailSubscription s : subs) {
                String subject = "【" + siteName + "】新工具上线：" + tl.getName();
                String text = "你好，" + siteName + " 工具箱新增了「" + tl.getName() + "」。\n\n"
                        + "简介：\n" + summary + "\n\n查看：" + url
                        + "\n退订：" + unsub + "?token=" + s.getToken();
                String html = buildHtml("新工具上线", tl.getName(), summary, url, "打开工具", s.getToken(), unsub);
                if (!mailService.sendSync(s.getEmail(), subject, text, html)) allSent = false;
            }
        }
        if (allSent) {
            markTools(list);
        } else {
            log.warn("[Notify] 工具推送存在发送失败,保留 notified=0 待下次重试(共{}个)", list.size());
        }
        return list.size() * subs.size();
    }

    private void markArticles(List<Article> list) {
        List<Long> ids = list.stream().map(Article::getId).collect(Collectors.toList());
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .in(Article::getId, ids).set(Article::getNotified, 1));
    }

    private void markProjects(List<Project> list) {
        List<Long> ids = list.stream().map(Project::getId).collect(Collectors.toList());
        projectMapper.update(null, new LambdaUpdateWrapper<Project>()
                .in(Project::getId, ids).set(Project::getNotified, 1));
    }

    private void markTools(List<Tool> list) {
        List<Long> ids = list.stream().map(Tool::getId).collect(Collectors.toList());
        toolMapper.update(null, new LambdaUpdateWrapper<Tool>()
                .in(Tool::getId, ids).set(Tool::getNotified, 1));
    }

    private String buildHtml(String badge, String title, String summary, String url,
                             String btnText, String token, String unsubBase) {
        String link = unsubBase + "?token=" + token;
        return "<div style='font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:520px;margin:0 auto;padding:24px;color:#0f172a'>"
                + "<p style='color:#0ea5e9;font-size:13px;letter-spacing:1px;margin:0 0 8px'>" + escape(badge) + "</p>"
                + "<h2 style='margin:0 0 12px;font-size:20px;line-height:1.4'>" + escape(title) + "</h2>"
                + "<p style='color:#475569;line-height:1.7;margin:0 0 20px;white-space:pre-wrap'>" + escape(summary) + "</p>"
                + "<p style='margin:0 0 24px'><a href='" + url + "' style='background:#0ea5e9;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;display:inline-block'>" + escape(btnText) + "</a></p>"
                + "<hr style='border:none;border-top:1px solid #e2e8f0;margin:20px 0'>"
                + "<p style='color:#94a3b8;font-size:12px;margin:0'>你收到此邮件是因为订阅了 " + escape(siteName) + " 的更新。如需退订，<a href='" + link + "' style='color:#64748b'>点击此处取消订阅</a>。</p>"
                + "</div>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
