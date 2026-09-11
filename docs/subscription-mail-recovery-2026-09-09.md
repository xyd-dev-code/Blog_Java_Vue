# 订阅邮件恢复记录（2026-09-09）

## 线上只读核查结果

- 服务 blog 正常运行；BLOG_MAIL_ENABLED=true，通知站点地址已配置。
- 目前有 2 位已确认订阅者，另有 1 位待确认订阅者。
- 文章 18、19 对订阅者 1、2 的 4 条 ARTICLE 投递记录均为 FAILED，attempts=5。
- 2026-09-08 发信日志为 MailAuthenticationException。
- 服务器连接 smtp.qq.com:465、TLS、EHLO 均成功；AUTH LOGIN 返回 535。
- QQ 返回的可能原因包括账号异常、SMTP 未开通、授权码错误、登录频率限制或服务繁忙，不能仅凭 535 断定授权码过期。
- V2 迁移确实于 2026-08-31 执行，但订阅者 1、2 已于 2026-09-03 再次确认，因此历史迁移不是这 4 条通知失败的直接原因。

## 恢复顺序

1. 在发件 QQ 邮箱检查 SMTP 是否启用，重新获取有效授权码。
2. 在服务器 /home/blog/blog/.env 中更新 BLOG_MAIL_PASSWORD（使用 SMTP 授权码）。不要把授权码提交到 Git 或粘贴到对话中。
3. 使用更新后的配置做 SMTP 登录验证，不发送邮件。若仍返回 535，先解决账号侧问题，不重置投递次数。
4. 经确认需要补发后，在暂停 blog 服务并备份相关数据库记录的情况下执行下面的限定修复。先运行 ROLLBACK 版本核对结果；确认恰好是预期的失败记录后，才改为 COMMIT 重跑。
5. 启动 blog 服务，使新环境变量生效。默认启动 60 秒后扫描，后续每 5 分钟扫描一次。
6. 核查这 4 条投递记录变为 SENT，并由收件人确认收件。SENT 仅表示 SMTP 接受，不保证进入收件箱。

## 限定补发 SQL（默认回滚预演）

仅处理本次确认的 4 条失败投递；保留成功记录，不恢复待确认或退订用户。

```sql
START TRANSACTION;

SELECT content_type, content_id, subscription_id, status, attempts
FROM notification_delivery
WHERE content_type = 'ARTICLE' AND content_id IN (18, 19)
  AND subscription_id IN (1, 2)
FOR UPDATE;

UPDATE notification_delivery d
JOIN email_subscription s ON s.id = d.subscription_id
SET d.status = 'RETRY', d.attempts = 0, d.next_attempt_at = NULL,
    d.lease_until = NULL, d.claim_token = NULL
WHERE d.content_type = 'ARTICLE' AND d.content_id IN (18, 19)
  AND d.subscription_id IN (1, 2) AND d.status = 'FAILED'
  AND s.status = 1 AND s.deleted = 0;
SELECT ROW_COUNT() AS reset_deliveries;

UPDATE article a
SET a.notified = 0
WHERE a.id IN (18, 19) AND a.status = 1 AND a.deleted = 0
  AND (a.password IS NULL OR a.password = '')
  AND EXISTS (
    SELECT 1 FROM notification_delivery d
    WHERE d.content_type = 'ARTICLE' AND d.content_id = a.id
      AND d.subscription_id IN (1, 2) AND d.status = 'RETRY'
  );
SELECT ROW_COUNT() AS reopened_articles;

ROLLBACK;
```

执行前需再次核对接收名单仍为本次的两位用户：现有调度器会为所有当前已确认订阅者处理重新打开的文章。如果名单已变化，应重新制定补发范围。

## 本地代码修复

NotifyService 在没有已确认订阅者时保留待通知内容，不再将其标记为已处理；增加回归测试验证订阅恢复后的后续扫描可以发信。这也意味着恢复订阅后可能补推积压内容，部署前应核对积压范围。

初次排查阶段未部署代码、未更改线上配置或订阅状态、未重置投递记录、未发送邮件。

## 用户授权后的实际恢复结果

- 用户更新服务器授权码后，SMTP AUTH LOGIN 返回 235，认证通过。
- 经用户明确授权重启和补发，2026-09-09 19:48 暂停 blog 服务，重新核对接收名单仅为订阅者 1、2，以及 4 条 FAILED 记录。
- 将两篇文章及 4 条投递记录备份到服务器 `/home/blog/blog/backups/mail-recovery-20260909-194828`，目录权限 700、文件权限 600。
- 事务内重置恰好 4 条失败投递和 2 篇文章的通知标记，然后启动服务。
- 已确认运行进程加载了新授权码，服务状态 active。
- 首次调度后，文章 18、19 对订阅者 1、2 的 4 条投递全部为 SENT，attempts=1；两篇文章 notified=1。
- 未改变订阅者状态，未部署本地代码。SMTP 已接受邮件，最终收件箱到达情况仍需收件人确认。
