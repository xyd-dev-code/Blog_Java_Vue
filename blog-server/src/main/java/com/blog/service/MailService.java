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
     * 异步发邮件。返回 void,所有异常在内部吞掉,只 WARN log。
     *
     * @param to        收件人邮箱
     * @param subject   主题
     * @param textBody  纯文本正文(fallback)
     * @param htmlBody  HTML 正文(可选,null 则用 textBody)
     */
    @Async("mailTaskExecutor")
    public void send(String to, String subject, String textBody, String htmlBody) {
        if (!enabled) {
            log.debug("[MailService] 跳过(未启用) → to={}, subject={}", to, subject);
            return;
        }
        if (to == null || to.isBlank() || subject == null || textBody == null) {
            log.warn("[MailService] 参数缺失,跳过发送 to={}", to);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            if (htmlBody != null) {
                helper.setText(textBody, htmlBody);
            } else {
                helper.setText(textBody, false);
            }
            mailSender.send(msg);
            log.info("[MailService] 发送成功 to={} subject={}", to, subject);
        } catch (MessagingException e) {
            log.warn("[MailService] MIME 构建失败 to={} subject={} err={}", to, subject, e.getMessage());
        } catch (Exception e) {
            // SMTP 不可达 / 鉴权失败 / 超时 — 兜底,不影响业务
            log.warn("[MailService] 发送失败 to={} subject={} err={}", to, subject, e.toString());
        }
    }

    public boolean isEnabled() { return enabled; }
}