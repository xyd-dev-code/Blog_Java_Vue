package com.blog.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务 — 所有评论/留言通知的入口。
 *
 * <p>设计原则:</p>
 * <ul>
 *   <li>异步(@Async("mailTaskExecutor")),不阻塞业务线程</li>
 *   <li>失败仅 WARN 日志,绝不抛异常(评论通知非关键路径)</li>
 *   <li>SMTP 没配 / host 不可达时,Javamail 在 transport 时抛 — catch 住兜底</li>
 *   <li>模板用 String.format 拼接纯文本 + 简易 HTML(后期可换 Thymeleaf)</li>
 * </ul>
 */
@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;
    private final boolean enabled;

    public MailService(JavaMailSender mailSender,
                       @Value("${blog.mail.from-email:}") String fromEmail,
                       @Value("${blog.mail.from-name:Blog}") String fromName,
                       @Value("${blog.mail.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.enabled = enabled;
        if (!enabled) {
            log.info("[MailService] 邮件功能未启用(blog.mail.enabled=false 或 SMTP 未配置),发件调用会被跳过");
        }
    }

    /**
     * 异步发邮件(评论/留言等广播通知)。不阻塞业务线程,失败仅 WARN log。
     */
    @Async("mailTaskExecutor")
    public void send(String to, String subject, String textBody, String htmlBody) {
        // 复用同步实现,异步包装一层即可,避免两份 MIME 构建逻辑
        sendSync(to, subject, textBody, htmlBody);
    }

    /**
     * 同步发信,返回是否成功。供关键链路(订阅确认)使用,
     * 以便后端如实反馈"确认邮件是否真的发出",避免前端盲目提示"请查收邮箱"。
     *
     * @return true=已成功提交至 SMTP;false=未启用 / 参数缺失 / 发送异常
     */
    public boolean sendSync(String to, String subject, String textBody, String htmlBody) {
        if (!enabled) {
            log.debug("[MailService] 跳过发送：邮件服务未启用");
            return false;
        }
        if (to == null || to.isBlank() || subject == null || textBody == null) {
            log.warn("[MailService] 参数缺失，跳过发送");
            return false;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            // multipart=true 才能同时设置纯文本 + HTML 两个 alternative part,
            // 否则 helper.setText(text, html) 抛 "Not in multipart mode"
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            if (htmlBody != null) {
                helper.setText(textBody, htmlBody);
            } else {
                helper.setText(textBody, false);
            }
            mailSender.send(msg);
            log.info("[MailService] 邮件发送成功");
            return true;
        } catch (MessagingException e) {
            log.warn("[MailService] MIME 构建失败: {}", e.getClass().getSimpleName());
        } catch (Exception e) {
            // SMTP 不可达 / 鉴权失败 / 超时 — 兜底,不影响业务
            log.warn("[MailService] 邮件发送失败: {}", e.getClass().getSimpleName());
        }
        return false;
    }

    public boolean isEnabled() { return enabled; }
}
