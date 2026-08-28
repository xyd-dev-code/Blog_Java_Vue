# 公开仓库隐私迁移清单

本文用于把现有部署迁移到“公开 GitHub 仓库”安全基线。任何被提交过的凭据都视为已经泄露；从当前文件删除并不能使旧值重新安全。

## 数据分层

| 数据类型 | 唯一允许位置 | 禁止位置 |
|---|---|---|
| 数据库、Redis、SMTP 密码 | 服务环境变量或密钥管理器 | YAML 实值、启动/部署脚本、前端、`site_config` |
| JWT 签名密钥、第三方 API/PAT | 服务环境变量或密钥管理器 | 数据库公开配置、前端变量、源码、日志 |
| 管理员密码 | 数据库 BCrypt 摘要 | 默认口令、初始化 SQL 可登录 hash、文档 |
| 昵称、头像、简介、履历、公开邮箱/GitHub | `user` / `site_config` | Vue/HTML/Java 常量 |
| 评论邮箱/IP/UA、订阅邮箱、访问 IP/UA | 业务数据库，仅管理员接口可访问 | 公开 API、日志正文、示例数据中的真实值 |

前端展示资料现在使用 `site_config` 的公开白名单；`github.token`、`githubToken` 以及名称含 password / secret / token / API key / credential 的历史字段不会再返回给任何前端，也不能通过站点配置接口写入。

访问与反滥用功能仍会把评论邮箱/IP/UA、订阅邮箱以及访问 IP/UA 写入业务数据库，但公开评论接口已使用脱敏 VO，订阅确认 token 不再参与 JSON 序列化，应用日志也不再输出这些原值。生产库应启用静态加密和最小权限账号，并按业务需要设置保留周期；例如访问统计不需要长期明细时，可定期删除过期 `visit_log`。若不需要按完整 IP 去重，进一步把 IP 改为带服务端 pepper 的不可逆摘要或网段化值。

## 既有数据库迁移

先备份数据库。检查遗留敏感配置时只查询 key，不要把 value 复制到工单或聊天：

```sql
SELECT config_key
FROM site_config
WHERE LOWER(REPLACE(REPLACE(REPLACE(config_key, '.', ''), '_', ''), '-', ''))
      REGEXP '(password|passwd|secret|token|apikey|accesskey|privatekey|credential)';
```

确认这些值已经迁移到服务环境变量后，再删除遗留行：

```sql
DELETE FROM site_config
WHERE LOWER(REPLACE(REPLACE(REPLACE(config_key, '.', ''), '_', ''), '-', ''))
      REGEXP '(password|passwd|secret|token|apikey|accesskey|privatekey|credential)';
```

旧库如果仍使用仓库曾公开过的 admin 默认密码，先准备一次性强密码，然后执行以下操作并重启：

```sql
UPDATE user
SET password = '!BOOTSTRAP_REQUIRED!'
WHERE username = 'admin';
```

重启前临时设置 `BLOG_ADMIN_INITIAL_PASSWORD`；日志出现初始化成功后立即删除该变量。该 SQL 会先禁用原密码，应在可控维护窗口执行。

关于页的新字段可在后台“个人中心 → 站点信息”填写：`roleTitle`、`userBio`、`aboutContent`、`aboutSkills`、`aboutTimeline`。成长轨迹每行格式为 `时间|标题|描述`。

## 必须轮换的凭据

当前 Git 历史曾包含早期后端配置、可推导的默认管理员口令、服务器地址和个人联系信息。公开前至少完成：

1. 修改应用数据库用户密码，并同步更新 `DB_PASSWORD`。
2. 生成新的 `BLOG_JWT_SECRET`；这会让所有旧登录令牌失效。
3. 轮换 SMTP 密码、GitHub PAT、云存储/API key 等曾在本地配置或文档出现过的值。
4. 检查 SSH 密钥；若私钥内容曾进入任何提交，立即撤销旧公钥并重新生成密钥对。
5. 删除服务环境中的 `BLOG_ADMIN_INITIAL_PASSWORD`，确认后续只使用数据库中的 BCrypt 摘要。

不要把新值写入迁移命令历史。生产环境优先使用 systemd `EnvironmentFile`（文件权限 `0600`）、容器 Secret 或云密钥管理器。

## 清理 Git 历史

仓库已有远端且旧提交包含敏感配置；仅新增一次“脱敏提交”仍可从历史恢复旧值。先轮换全部凭据，再在独立备份克隆中使用 `git filter-repo --replace-text`，替换清单文件必须放在仓库外并在完成后安全删除。

如果不需要保留历史，风险最低的公开发布方式是从已经通过扫描的当前目录创建全新的空仓库或孤儿分支，只提交一次干净快照。任何历史重写都会改变提交 ID，并需要与协作者协调后强制推送；执行前务必备份，因此本项目不会自动替你运行。

重写或创建干净仓库后运行：

```powershell
pwsh ./scripts/privacy-scan.ps1 -TrackedOnly
pwsh ./scripts/privacy-scan.ps1 -TrackedOnly -History
```

随后在 GitHub 开启 Secret scanning 与 Push protection，并确认分支、标签、Release 附件、Actions 日志、Issues、PR diff 和 fork 中都没有旧值。历史清理不能代替凭据轮换。

## 每次发布前

```powershell
git status --short
pwsh ./scripts/privacy-scan.ps1
git diff --cached --check
```

扫描器只输出类别、文件和行号，不打印命中的敏感值。CI 会对每次 push 和 pull request 扫描 Git 跟踪文件。
