package com.blog.config;

import com.blog.service.NotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动化邮件推送调度器。
 * 定期扫描"新文章/项目/工具"并推送给已确认订阅者，无需人工干预。
 * 间隔由 blog.notify.interval-ms 控制(默认 5 分钟)，可被 blog.notify.enabled 关闭。
 */
@Component
public class NotifyScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotifyScheduler.class);

    private final NotifyService notifyService;
    private final boolean enabled;

    public NotifyScheduler(NotifyService notifyService,
                           @Value("${blog.notify.enabled:true}") boolean enabled) {
        this.notifyService = notifyService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${blog.notify.interval-ms:300000}", initialDelay = 60_000)
    public void run() {
        if (!enabled) {
            log.debug("[NotifyScheduler] 自动推送已禁用(blog.notify.enabled=false)");
            return;
        }
        try {
            notifyService.notifyPending();
        } catch (Exception e) {
            // 单次推送异常不应影响下一次调度
            log.error("[NotifyScheduler] 推送任务异常", e);
        }
    }
}
