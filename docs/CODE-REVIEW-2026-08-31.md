**当前项目代码审查报告 · 2026-08-31**

> 本文记录修复前的审查结果。后续代码变更、验证与部署注意事项见[修复说明](./docs/CODE-REVIEW-FIXES-2026-08-31.md)。

审查基于工作区代码，基准提交 `d3aada4`，包含未提交的天气定位变更。项目包含 151 个生产 Java 文件和 110 个前端 JS/Vue 文件；本轮优先检查鉴权、文章、评论/留言、上传、订阅通知、导入导出、访问统计，以及前端编辑流程、配置与依赖。没有修改业务代码、数据库或运行配置，没有发送邮件，也没有对线上系统执行攻击测试。

**结论：发现 27 项问题，其中高 4 项、中 21 项、低 2 项。** 现有构建通过，但存在已隔离复现的脚本执行、会话撤销失效及多项业务缺陷，建议优先修复前四项。这里的严重程度按本项目影响评估，不等同于依赖公告的 CVSS 等级。

证据标记：**复现**表示调用当前实现或实际依赖完成隔离验证；**静态确认**表示代码路径直接支持结论；**条件风险**表示需要文中说明的配置、数据或输入条件，未验证线上是否具备这些条件。

**01 · 高 · Markdown 代码块可触发 XSS【复现】**

位置：[ArticleDetail.vue:91](./blog-web/src/views/front/ArticleDetail.vue:91)、[ArticleEdit.vue:86](./blog-web/src/views/admin/ArticleEdit.vue:86)、[package-lock.json:3473](./blog-web/package-lock.json:3473)。实际危险拼接位于已安装依赖 `md-editor-v3/lib/es/chunks/index.mjs:2259`。

问题与影响：锁定的 `md-editor-v3 4.21.3` 把代码块语言直接拼入 HTML 属性，内置 XSS 过滤未覆盖该渲染产物；页面也没有提供最终 HTML 清洗。隔离浏览器中，恶意代码块成功设置脚本执行标记。管理员导入、保存或预览外部恶意 Markdown 后，可在站点源下执行脚本；管理员令牌存放在 localStorage，因此可能进一步影响后台权限。此处不声称匿名用户能够直接发布文章。

修复：升级到修复该问题的兼容版本，并在 `MdEditor`、`MdPreview` 的最终 HTML 输出处统一使用经过维护的清洗器；如短期保留 4.x，应维护正式依赖补丁，对语言值转义并为属性加引号，不能只手工修改 node_modules。补充代码块语言、链接、图片及 HTML 的 XSS 回归用例。维护者列出的修复版本为 **6.5.4 及以上**，跨主版本需回归验证。[维护者安全公告](https://github.com/imzbf/md-editor-v3/security/advisories/GHSA-3rm2-h79c-8qw6)

**02 · 高 · 文章密码明文进入公开响应，且未用于访问控制【静态确认＋序列化复现】**

位置：[ArticleDTO.java:47](./blog-server/src/main/java/com/blog/dto/ArticleDTO.java:47)、[Article.java:26](./blog-server/src/main/java/com/blog/entity/Article.java:26)、[ArticleService.java:115](./blog-server/src/main/java/com/blog/service/ArticleService.java:115)、同文件 `homePage()`、`save()`。

问题与影响：管理 API 接受 `password`，`BeanUtils.copyProperties` 原样写入；公开列表及详情返回 Article 实体，没有排除密码，也没有文章口令校验。隔离序列化验证确认 `password` 会进入 JSON。若通过管理 API 或历史数据设置了文章密码，正文及密码仍可公开获取；没有设置密码的文章不涉及密码值泄露。

修复：明确是否支持密码文章。不支持则移除/拒绝该字段并清理历史值；支持则仅存密码摘要，建立独立解锁流程，并在所有列表、详情、搜索、归档入口统一检查可见性。公开 API 改为白名单 VO，不能仅隐藏密码字段而继续返回受保护正文。

**03 · 高 · 退出和改密未可靠撤销已有会话【复现＋静态确认】**

位置：[AdminProfileController.java:70](./blog-server/src/main/java/com/blog/controller/AdminProfileController.java:70)、[JwtAuthFilter.java:43](./blog-server/src/main/java/com/blog/security/JwtAuthFilter.java:43)、[AuthService.java:130](./blog-server/src/main/java/com/blog/service/AuthService.java:130)、[AdminLayout.vue:153](./blog-web/src/layouts/AdminLayout.vue:153)、[user.js:24](./blog-web/src/stores/user.js:24)。

问题与影响：前端实际改密走 `/admin/profile/password`，此实现只更新密码；旧 JWT 经真实过滤器仍被接受。另一条 `/auth/change-password` 仅撤销当前 token，其他会话不受影响。前端退出仅清本地状态，没有调用已存在的 `/auth/logout`。JWT 过滤器也不校验账号当前禁用状态或凭据版本。因此已泄露的令牌可能在退出、改密或禁用账号后继续使用至到期。

修复：改密统一下沉到 AuthService；引入用户凭据版本或 `tokensValidAfter`，每次认证检查当前账号状态与版本，改密/禁用时撤销该用户全部旧会话。退出时调用后端撤销接口，再在 finally 清本地状态。黑名单应按到期时间主动淘汰；当前 JwtBlacklist 仅在再次查询相同 key 时删除，不能保证释放过期项。

**04 · 高 · 公开头像上传在尺寸校验前完整解码【静态确认】**

位置：[AvatarUploadController.java:161](./blog-server/src/main/java/com/blog/controller/AvatarUploadController.java:161)、[FileUploadController.java:193](./blog-server/src/main/java/com/blog/controller/FileUploadController.java:193)。

问题与影响：`ImageIO.read()` 先分配原图内存，之后才按 512/1920 像素缩放。2 MB 压缩文件上限和文件签名校验无法限制解码后像素数量；高度压缩的大尺寸图片可消耗大量堆内存。访客接口无需认证，少量并发请求就可能导致 OOM。为避免影响工作环境，本轮未实际分配超大图片触发 OOM。

修复：使用 ImageReader 先读取宽高元数据，校验宽高与 `(long) width * height` 上限，再解码；必要时使用采样解码。设置全局解码并发上限、上传配额与过期孤立文件清理，并在 finally 释放 reader/writer/stream。缩放后的宽高至少为 1，避免极端长宽比产生零尺寸。

**05 · 中 · 未发布工具可通过公开 ID 接口读取【复现】**

位置：[PublicToolController.java:28](./blog-server/src/main/java/com/blog/controller/PublicToolController.java:28)、[ToolService.java:44](./blog-server/src/main/java/com/blog/service/ToolService.java:44)。

问题与影响：`GET /tools/{id}` 调用通用 `getById()`，没有像 slug 接口一样限制 `status=1`。隔离 mapper 返回 status=0 时，公开处理路径仍返回该工具，可能泄露尚未发布的地址和说明。

修复：拆分后台查询与公开查询，公开入口强制 `status=1`，不可见对象统一返回 404；增加匿名访问草稿、下线工具的测试。

**06 · 中 · 匿名点赞、举报和工具点击均被鉴权拦截【复现】**

位置：[SecurityConfig.java:103](./blog-server/src/main/java/com/blog/security/SecurityConfig.java:103)、[CommentController.java:77](./blog-server/src/main/java/com/blog/controller/CommentController.java:77)、[PublicToolController.java:39](./blog-server/src/main/java/com/blog/controller/PublicToolController.java:39)、[front.js:24](./blog-web/src/api/front.js:24)。

问题与影响：POST 白名单只有 `/comments`，未包含 `/comments/{id}/like`、`/comments/{id}/report`、`/tools/{id}/click`。使用真实 SecurityFilterChain 隔离验证，三条路径均返回 401。前台点赞/举报还会被通用响应拦截器重定向到后台登录；工具点击统计则被 silent 模式吞掉。

修复：只为这些确切的方法与路径开放匿名访问，不要放开整个管理或评论路径；为点赞及工具点击补充限频、对象可见性校验，再增加带安全过滤器的 API 契约测试。

**07 · 中 · 评论写入和读取缺少文章状态约束【静态确认】**

位置：[CommentService.java:58](./blog-server/src/main/java/com/blog/service/CommentService.java:58)、同文件 `treeByArticle()`（174 行）；[ArticleDetail.vue:126](./blog-web/src/views/front/ArticleDetail.vue:126)。

问题与影响：新增评论只检查文章是否存在，不检查是否发布以及 `allowComment`。后台关闭评论后，前端仍展示评论表单，直接提交也能写入。公开评论查询完全不检查所属文章状态，文章转为草稿/删除后，其已通过评论仍可能被按 ID 读取。

修复：集中实现公开文章访问策略；写评论必须文章可见且允许评论，读评论必须文章可见。前端按 `allowComment` 隐藏投稿入口，但后端校验仍是安全边界。

**08 · 中 · 留言板与普通文章 ID=1 共用数据【静态确认】**

位置：[CommentService.java:31](./blog-server/src/main/java/com/blog/service/CommentService.java:31)、[GuestbookForm.vue:289](./blog-web/src/components/guestbook/GuestbookForm.vue:289)、[init.sql:306](./blog-server/src/main/resources/db/init.sql:306)。

问题与影响：留言使用 articleId=1，但初始化数据库把第一篇普通文章 `hello-blog` 放在该 ID。文章评论与留言板相互混入，审核、统计及通知类型也混淆；删除该文章后，留言提交会因“文章不存在”而失败。

修复：使用明确的评论对象类型，例如 `target_type=ARTICLE/GUESTBOOK` 与 `target_id`，或独立留言模块；迁移旧数据并同步前端与通知逻辑，不能只替换成另一个容易冲突的魔法 ID。

**09 · 中 · 评论树把正常回复误判为环形节点【复现】**

位置：[CommentService.java:356](./blog-server/src/main/java/com/blog/service/CommentService.java:356)。

问题与影响：`visited` 记录遍历过的所有节点，却被用于判断父子链是否成环。文章评论按创建时间升序查询，父评论通常先被标记；处理回复时即落入“提升为根”分支。隔离输入一条父评论和一条回复，得到两个根节点而非一棵树。

修复：先建立 ID 索引，再建立父子关系；循环检测使用当前祖先链或三色 DFS，不能用全局已遍历集代替。为升序、降序、孤儿、深层回复和真实环分别测试。

**10 · 中 · 点赞/举报存在并发丢更新及部分提交【静态确认】**

位置：[CommentService.java:117](./blog-server/src/main/java/com/blog/service/CommentService.java:117)、同文件 `report()`（156 行）；[init.sql:138](./blog-server/src/main/resources/db/init.sql:138)。

问题与影响：先查询再插入点赞记录、读取计数后加一、最后用整个旧 Comment 更新，且没有事务。并发点赞可能撞唯一约束返回 500，记录与计数可能不一致；旧实体还可能覆盖同时发生的审核状态变更。举报同样有计数丢失风险。

修复：在事务中维护记录与计数，使用数据库原子加减并仅更新计数字段；同一用户操作以唯一键、行锁或版本字段串行化，处理重复键为幂等结果。仅增加 `@Transactional` 不足以解决读改写丢更新。

**11 · 中 · 限流清理提前删除小时/天窗口【复现】**

位置：[RateLimiter.java:56](./blog-server/src/main/java/com/blog/security/RateLimiter.java:56)、[RateLimiterCleanup.java:20](./blog-server/src/main/java/com/blog/config/RateLimiterCleanup.java:20)、[ShareService.java:61](./blog-server/src/main/java/com/blog/service/ShareService.java:61)。

问题与影响：所有 key 只要一分钟没有新请求就被清理，即使其实际窗口是举报 3600 秒、分享 86400 秒。隔离测试将小时窗口的最近记录设为两分钟前，调用清理后即可重新通过本应拒绝的请求。定时清理因此削弱业务宣称的小时/日配额。

修复：每个窗口保存真实窗口长度/过期时间，只在全部时间戳退出对应窗口后删除；清理和 acquire 的并发操作也需协调。多实例上线前再迁移到共享存储。

**12 · 中 · 订阅确认状态转换会绕过确认或恢复已退订用户【静态确认】**

位置：[SubscriptionService.java:98](./blog-server/src/main/java/com/blog/service/SubscriptionService.java:98)、同文件 `handleExisting()`（160 行）、`confirm()`（204 行）、`unsubscribe()`（345 行附近）。

问题与影响：SMTP 关闭时直接把任意提交邮箱标为已确认，日后开启通知即可向未经验证的邮箱发送邮件。退订保留原 token，而 confirm 只排除“已经确认”，可把 status=2 再改为 1；旧确认链接可以撤销退订状态。

修复：邮件关闭时保持待确认或禁用新订阅；确认只允许 `PENDING→CONFIRMED`，设置有效期并消费确认 token；退订应使旧确认 token 失效，确认与退订使用独立令牌。对已退订地址重新订阅必须重新验证。

**13 · 中 · 一位收件人失败会导致整批通知反复重发【静态确认】**

位置：[NotifyService.java:112](./blog-server/src/main/java/com/blog/service/NotifyService.java:112)、同文件 `notifyProjects()`、`notifyTools()`。

问题与影响：`allSent` 覆盖“全部内容×全部订阅者”，其中任意一次发送失败，整批内容都不标记完成。只要一个邮箱持续失败，其余已成功收到的用户每轮调度都会再次收到旧邮件，可能造成 SMTP 限额耗尽或被当作垃圾邮件。

修复：建立逐内容、逐收件人的投递记录和唯一幂等键；只重试失败项，采用退避和重试上限，处理永久失败地址。邮件投递应与访问统计线程池及调度职责分离。

**14 · 中 · Pandoc 超时控制在阻塞读取之后，实际无法限制挂起进程【静态确认】**

位置：[PandocRunner.java:179](./blog-server/src/main/java/com/blog/service/PandocRunner.java:179)、同文件 `docxToMarkdown()`（247 行）。

问题与影响：先 `stdout.readAllBytes()` 等待 EOF，之后才 `waitFor(30, ...)`。子进程卡住且不关闭输出时，代码永远无法到达超时分支；前端请求超时也不会自动终止服务器转换进程。无限日志还可能占满内存。

修复：并行且有界地消费输出，同时独立执行带期限的等待；超时/取消时终止整个子进程树并等待清理。限制并行转换数量、输出字节数及任务总耗时，`Files.walk()` 使用 try-with-resources。

**15 · 中 · 文档转换没有文件系统/网络隔离【条件风险】**

位置：[PandocRunner.java:138](./blog-server/src/main/java/com/blog/service/PandocRunner.java:138)、同文件 `convert()`、`docxToMarkdown()`。

问题与影响：转换直接以应用进程权限运行，没有 `--sandbox`、低权限隔离或出网限制。管理员导入的外部文档并不天然可信；在相应格式/引擎支持的情况下，文件引用、图片和原始标记可能读取本地资源或访问内部网络。本轮未尝试读取真实配置或访问内网。[Pandoc 官方安全说明](https://pandoc.org/demo/example33/22-a-note-on-security.html)

修复：在独立低权限进程/容器中转换，只挂载临时输入输出和必要字体模板，默认禁止出网，限制 CPU/内存；支持时启用 `--sandbox`，限制允许的标记和资源协议。官方明确说明 `--sandbox` 不涵盖 PDF 引擎，不能将其当成完整隔离。

**16 · 中 · 从编辑页发布草稿不会补齐 publishTime【静态确认】**

位置：[ArticleService.java:206](./blog-server/src/main/java/com/blog/service/ArticleService.java:206)、[ArticleEdit.vue:267](./blog-web/src/views/admin/ArticleEdit.vue:267)、[ArticleMapper.java:28](./blog-server/src/main/java/com/blog/mapper/ArticleMapper.java:28)。

问题与影响：新建草稿 publishTime 为空；编辑页发布使用通用 update，该方法不执行 save/updateStatus 中的发布时间逻辑。结果是 status=1 但 publishTime 为空：月归档统计忽略它，前后篇比较和发布日期排序也不正确。

修复：在服务层统一文章状态转换，首次发布且未指定合法时间时填充 publishTime；普通内容编辑不重置时间。测试草稿首次发布、重复保存、重新发布和归档查询。

**17 · 中 · 公开列表返回全量正文，计数也拉明细到内存【静态确认】**

位置：[ArticleService.java:53](./blog-server/src/main/java/com/blog/service/ArticleService.java:53)、同文件 `listAllPublished()`（304 行）、`injectCommentCount()`（383 行）；[HomeController.java:73](./blog-server/src/main/java/com/blog/controller/HomeController.java:73)、[CommentService.java:174](./blog-server/src/main/java/com/blog/service/CommentService.java:174)。

问题与影响：列表、搜索、上一篇/下一篇和归档使用完整 Article 实体，包含 LONGTEXT 正文；归档和评论树没有分页。评论计数虽避免 N+1，却将所有匹配评论的 articleId 拉到 JVM 分组。数据量增长后，网络传输、数据库读取、堆内存及浏览器渲染成本都随总数据量增长。

修复：列表使用仅含展示字段的摘要 VO/投影；归档按月或游标分页；评论按根评论分页再批量加载回复；计数使用 `GROUP BY article_id, COUNT(*)`。为常用查询验证索引和 EXPLAIN；月查询使用时间范围替代列上的 DATE_FORMAT。

**18 · 中 · DEBUG 日志会消费去重状态，导致访问统计不写入【复现】**

位置：[VisitLogFilter.java:122](./blog-server/src/main/java/com/blog/filter/VisitLogFilter.java:122)、同文件 149 行及 `isRecentDuplicate()`、`isSessionDuplicate()`；[application-dev.yml:8](./blog-server/src/main/resources/application-dev.yml:8)。

问题与影响：用于调试输出的 if 链调用了会写缓存的去重方法；随后业务 if 再调用时已被判为重复。开发配置默认 DEBUG，隔离验证一条合法访问最终对 recordAsync 调用次数为 0。打开日志不应改变业务结果。

修复：每个请求只计算一次去重结果，用同一结果做日志和控制流；或将纯查询与写缓存分开，并用原子操作处理去重竞争。测试 DEBUG/INFO 两种级别下统计结果一致。

**19 · 中 · 自动建表与实际实体不一致，失败又被静默吞掉【静态确认】**

位置：[StartupTableInitializer.java:35](./blog-server/src/main/java/com/blog/config/StartupTableInitializer.java:35)、[VisitLogService.java:47](./blog-server/src/main/java/com/blog/service/VisitLogService.java:47)、[init.sql:413](./blog-server/src/main/resources/db/init.sql:413)。

问题与影响：自动创建 visit_log 的 DDL 没有 `province`，实体、insert 和统计查询却依赖它。完整 init.sql 建库不会触发此差异，但在缺表时走自动建表或旧库缺列时，记录/统计会失败；recordAsync 的空 catch 又掩盖错误。

修复：采用版本化增量迁移，统一 DDL 来源；添加 province 迁移及索引，不靠“表存在就跳过”处理升级。对关键表结构启动校验；旁路写入失败保留脱敏日志和指标告警。

**20 · 中 · Springdoc 与 Spring Framework 存在实际二进制不兼容【字节码验证】**

位置：[pom.xml:10](./blog-server/pom.xml:10)、[pom.xml:24](./blog-server/pom.xml:24)。

问题与影响：Boot 3.5.6 搭配 springdoc 2.6.0。当前依赖的 GenericResponseService 字节码调用 `ControllerAdviceBean(Object)`，而实际 Spring 类已不存在该构造器。项目存在 RestControllerAdvice，因此生成包含通用异常响应的 OpenAPI 文档时有 NoSuchMethodError 风险；普通编译和现有单测不会覆盖它。

修复：按官方兼容矩阵选择适配 Boot 3.5.x 的 springdoc 2.8.x 系列修复版本，并测试 `/v3/api-docs` 的完整生成过程。不要仅通过关闭 generic response 功能掩盖版本问题。[官方兼容矩阵](https://springdoc.org/v2/faq.html)

**21 · 中 · 导入入口绕过 ArticleDTO 的约束校验【静态确认】**

位置：[AdminArticleController.java:147](./blog-server/src/main/java/com/blog/controller/AdminArticleController.java:147)、[ArticleImportExportService.java:253](./blog-server/src/main/java/com/blog/service/ArticleImportExportService.java:253)、同文件 `upsertWithFrontMatter()`（289 行）；[ArticleDTO.java:17](./blog-server/src/main/java/com/blog/dto/ArticleDTO.java:17)。

问题与影响：普通 JSON 创建使用 `@Valid`，文件导入则手工构造 DTO 后直接调用 service；注解不会自动生效。超长标题、非法 slug、越界状态及重复 tag 可进入持久化流程，导致数据库错误或生成前端无法访问的路由。非法 slug 还会原样成为导出 ZIP 条目名。

修复：在统一应用服务入口显式 Bean Validation，并校验标签去重及引用存在性；输入文件类型、字节大小、解析后文本长度均需限制。ZIP 文件名独立使用安全文件名规则，拒绝路径分隔符和 `..`。导入与 JSON API 使用同一业务约束。

**22 · 中 · 编辑页离开保护不覆盖路由切换和开关变更【静态确认】**

位置：[ArticleEdit.vue:183](./blog-web/src/views/admin/ArticleEdit.vue:183)、同文件 `confirmLeave()`（213 行）、`goBack()`（222 行）。

问题与影响：只在页面自己的返回按钮调用 confirmLeave，没有 Vue Router 的离开守卫。点击后台侧栏即可无提示丢失未保存内容。dirty 快照和 watch 也遗漏 isTop/isFeatured/allowComment，单独修改这些字段连关闭页签保护都不会触发。

修复：使用 `onBeforeRouteLeave` 统一拦截应用内离开，同时保留 beforeunload；快照覆盖全部可编辑字段。明确保存成功后解除脏状态，测试侧栏、浏览器前进后退及仅修改开关的场景。

**23 · 中 · 去重图床与删除旧头像逻辑冲突，可能删除共享图片【静态确认】**

位置：[LocalStorageService.java:51](./blog-server/src/main/java/com/blog/service/LocalStorageService.java:51)、同文件 `deleteByUrl()`（69 行）；[Profile.vue:309](./blog-web/src/views/admin/Profile.vue:309)。

问题与影响：同月相同图片按内容哈希共用文件 URL，但更新头像后由前端立即请求删除旧 URL，后端不检查引用。同一图片用于文章封面、站点 logo 或其他记录时，会一起失效；同步历史管理员回复并不能覆盖其他引用。

修复：由后端管理媒体资源及引用关系，删除只解除引用，后台任务在宽限期后清理零引用文件；或者上传不跨业务共享物理对象。前端不应负责判断文件是否可以物理删除。

**24 · 中 · 依赖安全告警缺少自动检查；导入 YAML 路径可达【审计结果＋静态确认】**

位置：[package-lock.json:3286](./blog-web/package-lock.json:3286)、[ArticleImportDialog.vue:190](./blog-web/src/views/admin/components/ArticleImportDialog.vue:190)、[privacy-scan.yml:1](./.github/workflows/privacy-scan.yml:1)。

问题与影响：本次官方 npm registry 审计报告 4 个受影响包：js-yaml 4.3.0、nanoid 3.3.15、postcss 8.5.16（工具等级 high）及 echarts 5.5.1（moderate）。其中 js-yaml 的 `!!omap` 二次复杂度问题可到达后台导入预览的同步 yaml.load，恶意导入文件可卡住管理页面。其余告警未证明能从本项目公开入口利用，不能按数量直接认定为四个线上高危。Markdown XSS 是额外人工查出的事项，未出现在此次 npm 汇总中。

修复：先升级 js-yaml 至至少 4.3.1，限制文件大小/解析条目并将较重解析移到 Worker；其余依赖按审计建议逐项更新并回归，ECharts 的升级涉及主版本。CI 增加前后端构建、关键业务测试、依赖审计；现有工作流只做隐私扫描。[js-yaml 维护者公告](https://github.com/nodeca/js-yaml/security/advisories/GHSA-5p4m-2wfm-xmqj)

**25 · 中 · 本地启动脚本仍包含凭据字面量【条件风险，未入 Git】**

位置：[start-backend.local.bat:5](./start-backend.local.bat:5)、同文件 6 行。

问题与影响：本地脚本为 DB_PASSWORD 和 BLOG_JWT_SECRET 设置了非空字面量默认值。文件已被 .gitignore 排除，所以不能据此认定公开仓库泄密；但共享工作区、备份或复制部署脚本仍可能扩散凭据。未尝试使用这些值，也未在报告或工具输出中展示原值。

修复：移除脚本内字面量，从运行环境或凭据存储注入，缺失时明确失败。如这些值曾用于实际环境且被分享过，应轮换；针对本地私密配置的检查与 Git 已追踪文件扫描分开进行。

**26 · 低 · GIF 头像分支不可达，前后端大小约束也不一致【GIF 已复现】**

位置：[AvatarUploadController.java:90](./blog-server/src/main/java/com/blog/controller/AvatarUploadController.java:90)、同文件 94 行；[CommentSection.vue:188](./blog-web/src/components/CommentSection.vue:188)。

问题与影响：扩展名校验声称支持 GIF，后续真实格式白名单却不含 gif，导致合法 1×1 GIF 在保存分支之前被拒绝。评论表单允许 5 MB，但后端头像上限为 2 MB，用户需上传后才发现失败。

修复：统一格式与体积约定；若支持 GIF，将其纳入白名单并验证动画帧/总像素资源上限，否则前端明确不支持。WebP 也应基于实际 ImageIO reader 能力提供支持，不能只看扩展名。

**27 · 低 · 无结尾换行的宽松 front matter 会解析越界【复现】**

位置：[ArticleImportExportService.java:579](./blog-server/src/main/java/com/blog/service/ArticleImportExportService.java:579)。

问题与影响：循环使用新变量 eol 查找当前换行，却用旧变量 nl 判断 EOF。输入 `title: demo\nbody with no final newline` 时，实际实现抛出 StringIndexOutOfBoundsException，合法导入内容失败。

修复：条件使用当前 eol，并为单行、多行无末尾换行、仅 front matter、空正文和不同换行符添加解析测试。

**代码质量与架构评价**

已有基础值得保留：Controller/Service/Mapper 分层清楚，主要使用构造器注入和参数绑定；评论公开 VO、站点配置白名单、BCrypt、JWT 密钥长度校验及通用异常脱敏已实现。没有从已审查的活跃查询路径确认用户可控 SQL 拼接；不能把数值 LIMIT 或固定内部列名直接报告为 SQL 注入。未确认可达的不安全 Java 原生反序列化入口，弃用而未调用的 DOCX XML 解析器也未算作已利用漏洞。

维护问题主要集中在“同一规则多处实现”：两条改密链路、前后端导入解析、多个排序/上传实现，以及 Entity 直接作为公开响应。这些重复已产生上述实际差异，建议先抽取统一鉴权、文章可见性、状态转换、媒体和导入应用服务；暂不需要为该规模拆分微服务。

注释多但部分与行为相反，例如 GIF 白名单、访问日志“失败仅 WARN”实际空 catch、客户端 IP 文档称 X-Real-IP 优先而实现 XFF 优先。应以可测试的接口契约替代解释性保证。低优先级可逐步统一状态枚举、格式化及错误处理，避免只做机械改名而放过行为缺陷。

**验证结果与边界**

| 检查 | 结果 |
|---|---|
| `mvn -B test` | 16 个测试通过，0 失败/错误 |
| `npm run build` | 成功；构建日志有依赖 PURE 注解警告，不阻止构建 |
| `privacy-scan.ps1 -TrackedOnly` | 工具报告检查 1007 个已追踪路径并通过；不等于审查了 Git 全历史及忽略文件 |
| `npm audit --json --registry=https://registry.npmjs.org` | 4 个受影响包：high 3、moderate 1；没有执行 audit fix |
| Markdown 隔离浏览器探针 | 当前 4.21.3 依赖执行了无害全局标记；测试页无真实 token、无业务数据写入，外部网络请求被阻断 |
| 安全过滤器探针 | 三条匿名交互 POST 返回 401；改密前 JWT 在改密后仍认证成功 |
| 业务探针 | 评论父子变两个根、限流提前重置、GIF 被拒、隐藏工具返回、文章密码被序列化、EOF 越界均复现 |
| 日志与依赖探针 | DEBUG 下日志服务调用数为 0；Springdoc 调用了当前 Spring 不存在的构造器 |

探针仅使用内存对象、Mockito、MockMvc、反射及隔离浏览器，不连接真实数据库或 SMTP。没有进行生产流量压测、真实图片 OOM、内网请求探测、真实管理员令牌使用或全部 Maven 依赖的 CVE 扫描。因此性能问题是代码级风险判断，不是实测线上吞吐结论；未发现某类漏洞不代表不存在。

隔离探针在 Git 忽略的 target 临时构建目录运行，未加入项目正式测试套件；交付时这些临时验证文件已被清理，本报告保留验证摘要。修复时应将关键场景补充为可重复运行的回归测试。

**建议实施顺序**

1. 首先处理 01–04：关闭可执行 Markdown 路径、修复公开响应与文章口令策略、统一会话撤销、对图片做解码前限额。
2. 然后处理 05–13、16：恢复公开交互，统一内容状态约束，修复评论树、留言模型、并发计数、限流及订阅状态/投递幂等性。
3. 随后处理文档转换隔离、增量迁移、依赖兼容、导入校验、分页投影和编辑保护；补齐安全过滤器、状态转换、数据库并发和导入导出的回归测试。

本轮交付新增文件仅为审查报告。审查期间工作区另外出现两个启动脚本删除状态，未由本轮执行删除，也未擅自恢复；报告代码行号以审查时内容为准。
