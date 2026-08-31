**代码审查修复说明 · 2026-08-31**

对应[原审查报告](./docs/CODE-REVIEW-2026-08-31.md)的 27 项问题。本轮已修改代码、配置模板和回归测试；没有连接生产数据库、发送邮件、拉取转换镜像或执行部署。原报告描述修复前的状态，不应当作当前代码的缺陷清单。

**修复范围**

| 编号 | 修复结果 | 主要位置 |
|---|---|---|
| 01 | Markdown 编辑器升级到 6.5.6；编辑、文章详情、关于页统一清洗最终 HTML 与图表 SVG。实际浏览器 XSS 负载回归通过。 | `markdownSecurity.js`、`ArticleEdit.vue`、`ArticleDetail.vue`、`About.vue` |
| 02 | 公开查询统一过滤未发布和带口令文章，使用白名单 VO，实体密码不序列化；拒绝新增非空文章口令。迁移将历史口令文章保留为草稿并清除明文密码，需管理员审阅后重新发布。 | `ArticleVisibility`、`ArticlePublicVO`、`ArticleDTO`、V2 迁移 |
| 03 | 改密统一进入 AuthService；JWT 绑定当前凭据，每次认证校验账号状态与当前角色。退出调用后端，撤销记录持久化并定时清理，支持重启与多实例。 | `JwtUtil`、`JwtAuthFilter`、`JwtBlacklist`、`AuthService`、`stores/user.js` |
| 04 | 先检查图片元数据，再采样解码；限制边长、总像素、GIF 帧数和解码并发，释放图像资源。公开上传补充全局/IP 限额。 | `SafeImageProcessor`、两个上传控制器 |
| 05 | 工具公开 ID 查询及点击计数统一校验发布状态，不可见对象返回 404。 | `ToolService`、`PublicToolController` |
| 06 | 只开放确切的匿名点赞、举报、工具点击 POST 路径，并保留/增加限流；公开接口错误不再强制跳后台登录。 | `SecurityConfig`、`CommentController`、`request.js` |
| 07 | 读评论要求文章公开，写评论还必须允许评论；前端关闭表单和投稿入口。 | `CommentService`、`CommentSection.vue` |
| 08 | 使用 ARTICLE/GUESTBOOK 对象类型隔离文章评论与留言，审核、统计、回复、通知和前端提交同步更新。历史 article_id=1 数据有歧义，保留为文章评论，见迁移注意事项。 | `Comment`、`CommentDTO`、`CommentService`、留言控制器及表单 |
| 09 | 使用当前祖先链检测循环，正常回复不再提升为根；已审核孤儿回复仍可访问。 | `CommentService.buildTree/pagedTree` |
| 10 | 点赞/举报在事务内锁定目标评论，只原子更新计数字段；并发测试验证记录与计数一致。工具每日点击也改为原子 upsert。 | `CommentMapper`、`CommentService`、`ToolDailyClickMapper` |
| 11 | 限流器保存每个 key 的真实时间窗口；清理与获取通过同一并发原语协调，不再提前释放小时/日配额。 | `RateLimiter` |
| 12 | SMTP 关闭时拒绝公开订阅；确认令牌独立、24 小时有效且一次性消费，退订清除确认凭据；使用配置的可信站点地址。 | `SubscriptionService`、`EmailSubscriptionMapper` |
| 13 | 持久化逐内容/收件人的投递状态、租约、退避及最多 5 次尝试；成功者不会因其他地址失败而整批重发。 | `NotificationDeliveryService`、`NotifyService` |
| 14 | 独立超时控制与有界输出读取；超时终止子进程并清理容器，转换并发限制为 2。 | `BoundedProcessRunner`、`PandocRunner` |
| 15 | DOCX/PDF 强制在禁网、低权限、只读根文件系统、受 CPU/内存/进程/单文件大小限制的容器中转换；不再回退到不受隔离的本机 Pandoc。 | `PandocRunner`、配置模板 |
| 16 | 首次发布补齐发布时间，普通更新保留已有时间；迁移修复历史已发布但无时间的数据。 | `ArticleService`、V2 迁移 |
| 17 | 公共列表用摘要投影，数据库聚合评论计数，归档分页；评论根节点分页，初始树最多 1000 节点，超出回复通过游标接口继续加载，不会静默丢失。 | `ArticleService`、`CommentMapper`、`CommentService`、归档/评论组件 |
| 18 | 日志不再调用有副作用的去重判断；并发去重原子化，访问记录与邮件使用独立线程池。 | `VisitLogFilter`、`AsyncConfig` |
| 19 | 用 Flyway 版本迁移替代启动时吞异常建表；兼容已有库，增加 province 和相关索引，初始化缺失表及新安全状态表。 | `db/migration`、`application-example.yml` |
| 20 | springdoc 调整到适配 Boot 3.5.x 的 2.8.17；完整生成 /v3/api-docs 的集成测试通过。 | `pom.xml` |
| 21 | 保存/更新统一执行 DTO 与引用校验，导入先校验再创建分类标签；限制文件/元数据大小、标签数量，导出文件名去除路径字符并限制批量规模。 | `ArticleService.validate`、`ArticleImportExportService` |
| 22 | 所有可编辑开关纳入脏状态，统一 Vue Router 离开/更新守卫；复用编辑组件切换文章时重新加载，防止旧表单覆盖另一篇文章。 | `ArticleEdit.vue` |
| 23 | **安全缓解**：停止更换头像时删除共享文件，后端拒绝按 URL 直接物理删除。避免破坏其他引用，但暂不自动清理孤立文件；完整媒体引用台账/GC 留作独立功能。 | `LocalStorageService.deleteByUrl`、`Profile.vue` |
| 24 | 升级受影响前端依赖；YAML 使用受限 schema、元数据限额和可终止 Worker。新增前后端构建、回归与 npm audit CI。 | `package-lock.json`、`markdownImport*`、`.github/workflows/verify.yml` |
| 25 | 本地忽略脚本的数据库/JWT 凭据默认值已移除，缺失时失败；没有读取这些值用于登录，也没有执行实际环境密钥轮换。 | `start-backend.local.bat`（本地文件） |
| 26 | 统一 PNG/JPEG/GIF 支持，公开头像限 2MB、后台图片限 10MB；合法 GIF 可上传，动画受帧数/像素约束。不声称支持没有解码器的 WebP。 | `SafeImageProcessor`、上传接口、评论/留言表单 |
| 27 | 宽松 front matter 的 EOF 判断改用当前换行位置；每次解析独立创建受限 YAML 实例，避免并发共享解析器。 | `ArticleImportExportService` |

浏览器测试还覆盖评论关闭状态、回复游标分页、同组件文章切换、仅改开关后的导航取消/确认、真实前端退出请求。已有天气功能和其他工作区改动保留；天气控制器测试仅将模拟地址换为文档保留地址，便于隐私扫描通过。

**验证记录**

- 后端：`mvn -B -Dspring.config.name=application-example verify`，42 项测试通过，并完成 JAR 打包。
- 关键测试使用 H2 的 MySQL 兼容模式、MockMvc 和真实 Spring Security/Flyway/MyBatis；包括旧库升级、口令文章草稿化、会话撤销、并发点赞、1000 节点后的回复加载、通知退避、过期确认和 DEBUG 访问统计。
- 前端：`npm run build` 通过；`npm run test:review` 在本机无头 Edge 通过，CI 使用 Chromium；测试拦截所有非本地网络请求。
- `npm audit --audit-level=moderate --registry=https://registry.npmjs.org`：0 项告警，仅代表当次 npm 审计结果。
- `scripts/privacy-scan.ps1`：通过；`git diff --check`：通过。
- 未运行真实 MySQL 迁移、真实 SMTP 投递或 Docker/Pandoc 转换端到端测试。本机未安装 Docker；进程期限/输出限制及容器命令约束已有自动化测试，不能代替部署环境的转换验收。

**部署前必须了解的变更**

1. **先备份数据库，在副本上验证迁移。** 将[配置模板](./blog-server/src/main/resources/application-example.yml)中的 `spring.flyway` 与 `blog.export` 段合并到实际配置。V1 是无 DROP、无默认凭据的建表基线；已有库基线为 V1，由 V2 补齐缺失结构。不要对已有库执行旧的 `db/init.sql`。应用第一次启动需要 DDL 权限，迁移失败会阻止启动。
2. **历史密码文章不会自动公开。** V2 保留标题和正文，改为草稿并清除旧明文口令。由管理员确认内容可以公开后再发布；当前版本不提供文章口令解锁功能。
3. **全部旧 JWT 需要重新登录。** 新令牌增加凭据绑定。新库需临时注入 `BLOG_ADMIN_INITIAL_PASSWORD`（至少 12 字符）初始化管理员，完成后移除该变量。不要把真实凭据写回脚本。
4. **旧公开渠道订阅需重新确认。** 无法可靠区分历史 SMTP 关闭时的自动确认与真实确认，因此 V2 将 source 非 admin 的已确认订阅改为待确认，保留邮箱；不会自动发送重确认邮件。管理员添加的订阅保持原状态。启用邮件前配置可信 HTTPS 站点地址、SMTP 和合理的 SMTP 连接/读写超时。
5. **旧留言需要人工核对。** article_id=1 曾同时代表普通文章与留言板，仅凭现有字段无法可靠分类。本轮不猜测归属、不移动历史数据；确认具体留言及其回复 ID 后，统一设置 `target_type='GUESTBOOK'`、`article_id=0`。普通文章评论保持 ARTICLE。迁移前后应核对数量，避免拆散同一回复树。
6. **DOCX/PDF 转换现在依赖 Linux Docker 容器。** 部署时提前准备 `pandoc/latex:3.6.4`，运行时使用 `--pull=never`；可通过 `BLOG_PANDOC_IMAGE` 指向已验证并固定摘要的内部镜像。建议专用/无 root 权限的转换环境，只给临时目录和只读字体模板挂载权限，并对临时磁盘设置配额。准备所需中文字体后分别验收 Markdown→DOCX、Markdown→PDF、DOCX→Markdown；不允许为恢复功能改回无限制本机转换。Markdown 导入/导出不依赖 Docker。
7. **共享图片保留，不自动删除。** 上传接口支持 PNG/JPEG/GIF；禁止通过旧删除接口直接删除共享 URL。现有文件不会清理，需要在有引用台账与宽限期的维护流程中回收，以免误删文章封面、站点 Logo 或历史回复头像。
8. **密钥轮换由实际环境负责人执行。** 本轮移除了本地脚本的字面量；若旧值用于实际服务且曾分享，仍须轮换数据库密码/JWT 密钥。这里没有连接真实服务完成轮换。

**边界与后续运维**

限流窗口已修正，但限流器仍为单进程存储；多实例的整体配额需要网关或共享限流存储。SMTP 没有跨数据库事务的 exactly-once 保证：进程在邮件实际发出后、状态写回前崩溃仍可能重复；投递重试达到上限会记录 FAILED，需监控与人工处置。归档/评论按需加载避免一次拉全表，但并非对生产数据做过负载测试或 EXPLAIN 调优。

技术依据：[Markdown 编辑器安全公告](https://github.com/imzbf/md-editor-v3/security/advisories/GHSA-3rm2-h79c-8qw6)、[springdoc 兼容说明](https://springdoc.org/v2/faq.html)、[Pandoc sandbox](https://pandoc.org/MANUAL.html#option--sandbox)。本次没有将“审计无告警”解释为不存在其他安全风险。
