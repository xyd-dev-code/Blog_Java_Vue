# 拾光小筑 · Blog Java Vue

基于 **Spring Boot + Vue 3** 的前后端分离个人博客。包含公开站点与管理后台，支持文章创作、项目展示、工具导航、评论留言、访问统计和邮箱订阅，并提供「晴天」与「武侠水墨」两套主题。

本文以当前仓库实现为准。运行需要 Java、Node.js 和 MySQL；Docker 仅用于可选的文档转换，Redis 不是启动依赖。

## 功能

### 公开站点

- **文章阅读**：文章列表、关键词搜索、分类、标签、归档、详情、关联文章与上一篇／下一篇。
- **阅读体验**：阅读进度、二三级标题目录、滚动高亮、锚点跳转和移动端布局。文章列表置顶优先，同组内按创建时间倒序，与卡片日期一致。
- **内容展示**：项目集、工具导航、友情链接、关于页面；支持分享与点击统计。
- **评论与留言**：评论、嵌套回复、回复折叠与继续加载、头像上传或图片 URL；留言板支持点赞、举报和精选展示。
- **主题**：晴天与武侠水墨配色、背景、装饰及文案。水墨阅读页标题与摘要使用系统楷体，避免网络题字字体加载后反复改变字形；具体字体取决于设备是否安装 KaiTi／STKaiti。
- **天气**：IP 粗略定位、用户主动授权的设备定位、城市搜索与天气展示。
- **订阅**：邮箱确认订阅、退订及内容更新通知。

### 管理后台

| 模块 | 当前能力 |
| --- | --- |
| 仪表盘与资料 | 概览、站点配置、管理员资料与密码维护 |
| 文章 | Markdown 编辑与预览、草稿／发布、置顶／推荐、分类和标签、归档、导入导出 |
| 评论／留言 | 审核、回复、标记垃圾、删除；按昵称或内容搜索，支持状态筛选与分页 |
| 举报 | 处理、驳回、处理被举报评论；按被举报人、内容、举报详情或联系邮箱搜索 |
| 项目 | 项目与项目分类管理 |
| 工具 | 工具与工具分类管理、搜索、分类状态和操作日志 |
| 友链 | 友情链接管理 |
| 订阅 | 订阅用户管理、CSV 导出 |
| 访问统计 | 访问日志、设备、系统、浏览器与地区分布 |
| 主题 | 晴天／武侠水墨主题管理 |

文章编辑器使用 **md-editor-v3**。支持 Markdown／DOCX 导入，以及 Markdown／DOCX／PDF 导出；DOCX 和 PDF 转换依赖 Docker 中的 Pandoc。

## 技术栈

以下版本来自 `pom.xml` 与 `package.json`；前端安装结果由 `package-lock.json` 锁定。

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.5.6、Spring Security、MyBatis-Plus 3.5.7 |
| 数据与迁移 | MySQL、Flyway；测试使用 H2 |
| 认证与缓存 | JJWT 0.12.6、BCrypt、Caffeine |
| API 文档 | SpringDoc OpenAPI 2.8.17 |
| 前端 | Vue 3.5、Vite 6、Vue Router 4.5、Pinia 2.3、Axios 1.7 |
| 界面 | Element Plus 2.9、SCSS、ECharts 6.1 |
| 编辑与渲染 | md-editor-v3 6.5、marked 15、DOMPurify |
| 验证 | JUnit、Spring Boot Test、Playwright、GitHub Actions |
| 可选服务 | SMTP、Docker／Pandoc、ip2region、天气与 IP 定位服务 |

## 目录结构

```text
Blog_Java_Vue/
├── blog-server/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/blog/       # 接口、业务、认证、配置、实体与数据访问
│       ├── main/java/db/migration/   # Flyway Java 迁移
│       ├── main/resources/
│       │   ├── application.yml       # 当前运行配置，含原部署环境的默认值
│       │   ├── application-dev.yml   # 开发日志与本地访问统计配置
│       │   ├── application-example.yml # 可复用的环境变量配置模板
│       │   ├── db/migration/         # Flyway SQL 迁移
│       │   └── templates/           # 文档导入导出模板
│       └── test/                    # 后端单元与集成测试
├── blog-web/
│   ├── src/
│   │   ├── api/                     # 前后台接口封装
│   │   ├── components/              # 评论、目录配套、分享等组件
│   │   ├── composables/             # 天气、交互等组合式逻辑
│   │   ├── layouts/                 # 前后台布局
│   │   ├── router/                  # 路由与后台访问校验
│   │   ├── stores/                  # 状态管理
│   │   ├── themes/                  # 主题注册表与设计变量
│   │   ├── styles/                  # 全局与主题样式
│   │   └── views/                   # 前后台页面
│   ├── public/                     # 图片、字体等静态资源
│   ├── scripts/                    # 前端回归检查
│   └── design-qa-assets/            # 本地设计验收产物
├── deployment/nginx/               # Nginx 部署参考
├── server-config/                  # 原部署环境配置与历史增量 SQL
├── scripts/                        # 部署配置检查、隐私扫描、产物清理
├── docs/                           # 专题文档、审查记录与文章草稿
├── .github/workflows/              # 构建、回归与隐私扫描
├── start-backend.bat               # Windows 后端启动辅助
├── start-backend.local.bat.example  # 本地启动变量示例
├── start-frontend.bat              # Windows 前端启动辅助
├── deploy.ps1 / deploy.bat         # 原站点部署辅助
└── README.md
```

## 本地运行

推荐使用 **JDK 17、Maven、Node.js 22、MySQL 8**。Node.js 22 与仓库 CI 一致。以下命令从项目根目录执行，环境变量示例使用 PowerShell。

### 1. 创建数据库

在 MySQL 客户端中创建空库，并准备有该库建表及读写权限的账号：

```sql
CREATE DATABASE IF NOT EXISTS blog_java_vue
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

启动后由 Flyway 执行 `db/migration` 下的 SQL 与 Java 迁移。无需手动导入示例数据。已有数据库升级前应备份，并核对结构与迁移基线；配置启用了 `baseline-on-migrate`，它不会证明旧库结构完整。

**不要用 `blog-server/src/main/resources/db/init.sql` 升级已有环境**：这是包含删除同名数据库操作的历史初始化脚本。

### 2. 配置后端

建议显式选择仓库的通用模板，避免沿用 `application.yml` 中原开发机的上传路径、图片域名与连接配置：

```powershell
$env:SPRING_CONFIG_NAME = "application-example"
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_USERNAME = "你的数据库账号"
$env:DB_PASSWORD = "你的数据库密码"
$env:BLOG_JWT_SECRET = "替换为至少32字节的随机密钥"
$env:BLOG_ADMIN_INITIAL_PASSWORD = "替换为至少12个字符的初始密码"
$env:UPLOAD_DIR = "$PWD/blog-server/uploads/img"
$env:IMG_BASE_URL = "http://localhost:9000"
```

请将示例值替换为自己的配置。JWT 密钥可以用 `openssl rand -base64 64` 生成，生成后保存在本地环境配置中，重启时使用同一个值。

| 变量 | 作用与模板默认值 |
| --- | --- |
| `DB_URL` | 可选，默认连接本机 `blog_java_vue`，时区为 Asia/Shanghai |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号与密码，模板账号默认 `blog` |
| `BLOG_JWT_SECRET` | 必填；至少 32 字节，不提供可用默认值 |
| `BLOG_ADMIN_INITIAL_PASSWORD` | 首次初始化管理员时设置，至少 12 个字符 |
| `UPLOAD_DIR` | 可写的图片存储目录，模板默认 `./uploads/img` |
| `IMG_BASE_URL` | 必填；图片对外访问的 URL 前缀 |
| `SERVER_PORT` | 默认 `8080` |
| `FRONTEND_ORIGIN` / `FRONTEND_ORIGIN_WWW` | 额外允许跨域访问的前端来源 |

这些变量名以 **`application-example.yml`** 为准。直接使用默认 `application.yml` 时，部分配置没有对应的模板变量映射，应使用 Spring 标准环境变量覆盖或外部配置文件。

### 3. 启动后端

在设置变量的同一终端执行：

```powershell
cd blog-server
mvn spring-boot:run
```

- 健康检查：[http://localhost:8080/api/v1/ping](http://localhost:8080/api/v1/ping)
- 开发环境 Swagger：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON：[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

初始管理员用户名默认 `admin`。初始化只创建新账号或替换未初始化的密码标记，不会覆盖已有密码。确认初始化成功后，从启动配置中移除 `BLOG_ADMIN_INITIAL_PASSWORD`，后续在后台修改密码。

Windows 双击启动也可使用 `start-backend.bat`，并参考 `start-backend.local.bat.example` 创建被 Git 忽略的本地变量文件。不过启动脚本含开发机专用 `JAVA_HOME`，换电脑时需先调整；上面的 Maven 命令更通用。

### 4. 启动前端

另开终端，从项目根目录执行：

```powershell
cd blog-web
npm ci
npm run dev
```

- 前台：[http://localhost:5173](http://localhost:5173)
- 后台：[http://localhost:5173/admin/login](http://localhost:5173/admin/login)

Vite 默认使用 5173 端口，端口占用时以终端输出为准。`/api` 和 `/uploads` 代理到 `http://localhost:8080`；后端端口改变时需同步调整 `vite.config.js`。

### 5. 本地图片访问

图片上传写入 `UPLOAD_DIR`，**当前后端未直接挂载该目录为静态资源**。`IMG_BASE_URL` 必须指向实际提供图片的服务，仅配置 Vite 的 `/uploads` 代理并不能让磁盘文件可访问。

按上述本地配置，可在项目根目录另开终端运行：

```powershell
python -m http.server 9000 --directory blog-server/uploads/img
```

生产环境应改为 Nginx 静态目录或独立图片域名。

## 可选功能

### 文档转换

Markdown 导入导出不依赖 Docker。DOCX 导入，以及 DOCX／PDF 导出，通过 Docker 内的 Pandoc 转换，当前不调用宿主机原生 Pandoc。

```sh
docker pull pandoc/latex:3.6.4
```

需要可用的 Linux 容器运行环境，并让后端运行用户有权限调用 Docker。可通过 `BLOG_DOCKER_BIN`、`BLOG_PANDOC_IMAGE`、`BLOG_EXPORT_TEMPLATE_DIR` 指定可执行文件、镜像与模板工作目录。普通文章编辑和阅读无需启用文档转换。

### 邮件与订阅

通用模板默认关闭邮件与自动推送。启用时配置：

- SMTP：`BLOG_MAIL_HOST`、`BLOG_MAIL_PORT`、`BLOG_MAIL_USERNAME`、`BLOG_MAIL_PASSWORD`。
- 传输方式：`BLOG_MAIL_SMTP_AUTH`、`BLOG_MAIL_SSL`、`BLOG_MAIL_STARTTLS`，按邮箱服务实际要求设置。
- 邮件开关与身份：`BLOG_MAIL_ENABLED`、`BLOG_MAIL_FROM_EMAIL`、`BLOG_MAIL_FROM_NAME`、`BLOG_MAIL_ADMIN_EMAIL`。
- 链接域名：`BLOG_SITE_BASE_URL`，以及按功能需要设置的 `BLOG_SUBSCRIBE_BASE_URL`、`BLOG_NOTIFY_BASE_URL`。
- 自动推送：`BLOG_NOTIFY_ENABLED`、`BLOG_NOTIFY_INTERVAL`，默认周期为 300000 毫秒。

外发链接应使用真实的 HTTPS 站点域名。注意默认 `application.yml` 与通用模板的通知开关默认值不同，部署时应显式指定。

### 天气与访问地区

天气显示位置与访问日志地区统计是两个独立用途。可通过 `IP2_REGION_DB_PATH` 指定离线 ip2region 数据文件；天气定位服务、精度限制与可选配置见 [天气 IP 定位说明](docs/天气IP定位.md)。

本机／内网访问通常无法得到公网地区信息。开发配置可记录本地访问，但不代表能识别真实地理位置。

## 构建与验证

后端完整验证和打包（与 CI 使用同一配置入口）：

```sh
cd blog-server
mvn -B -Dspring.config.name=application-example verify
```

测试使用隔离的 H2 等测试环境，产物为 `blog-server/target/blog-server.jar`。

前端构建：

```sh
cd blog-web
npm ci
npm run build
```

产物为 `blog-web/dist/`。常用检查：

```sh
# 在 blog-web 目录
npm run test:auth-guard
npm run test:review
npm run test:theme-layout
node ../scripts/check-deployment-config.mjs
```

还提供 `test:weather`、`test:theme-layout:browser`，以及 `scripts/article-toc-regression.mjs` 等专项脚本。浏览器脚本的前置条件不同：有些自行启动测试服务，有些要求本地 Vite 服务已运行，部分 Windows 脚本使用本机 Edge。运行前查看对应脚本的端口、浏览器与环境变量设置。

GitHub Actions 会执行后端验证、前端依赖审计与构建、部署配置检查、后台鉴权／功能回归及主题布局检查。隐私扫描由独立工作流执行。

## 部署

推荐结构：**Nginx 提供前端与图片，Spring Boot 提供 API，MySQL 保存业务数据**。

1. 备份数据库和上传目录，构建前后端产物。
2. 为后端配置数据库、固定 JWT 密钥、图片目录与访问域名；使用 `prod` Profile。
3. 发布 `blog-server.jar`，由 systemd 或其他进程管理器运行。启动时由 Flyway 执行版本迁移。
4. 发布 `dist/`，配置 Vue history 路由回退与 `/api/` 反向代理。
5. 单独配置图片静态服务，并检查图片 URL、登录、文章阅读及上传是否正常。

通用模板打包在 JAR 中，可通过环境变量 `SPRING_CONFIG_NAME=application-example`、`SPRING_PROFILES_ACTIVE=prod` 选择，再运行：

```sh
java -jar blog-server.jar
```

Nginx 参考：[deployment/nginx/example.com.conf.example](deployment/nginx/example.com.conf.example)。其中域名、证书和目录属于原站点，复制前需要替换；其 `/uploads/` 代理不能替代图片目录的静态映射。带哈希的前端资源可长期缓存，`index.html` 和 `theme-bootstrap.js` 应重新验证缓存，避免发布后继续加载旧页面。

根目录 `deploy.ps1` / `deploy.bat` 也绑定了原站点主机、用户、路径和服务名，**不是开箱即用的通用部署脚本**。复用前检查配置区。`deploy.ps1 -Migrate` 会运行 `server-config` 中的历史增量 SQL，这与应用启动的 Flyway 迁移不是同一个入口，应先核对数据库版本与 SQL 内容。

## 认证与数据边界

- 管理接口由 Spring Security + JWT 保护，后台路由会向服务端验证管理员身份。
- 密码保存为 BCrypt 摘要；JWT 撤销记录保存在数据库，应用重启后仍有效。
- 限流与 Caffeine 缓存主要使用进程内存；多实例部署需要另行考虑共享限流和缓存一致性。
- 公开接口使用脱敏视图；Markdown／评论渲染与图片上传有相应校验处理。
- Swagger 仅在开发 Profile 下开放。
- 密码、JWT 密钥、SMTP 凭据保存在运行环境或未跟踪的本地配置中，不写入 README 或提交到仓库。

## 常见问题

| 现象 | 优先检查 |
| --- | --- |
| 后端启动提示 JWT 或图片配置缺失 | 是否选择通用模板，是否设置 `BLOG_JWT_SECRET` 与 `IMG_BASE_URL`，上传目录是否可写 |
| 后台首次无法登录 | 是否设置一次性管理员密码；已有密码不会被初始化变量覆盖 |
| 上传成功但图片打不开 | `IMG_BASE_URL` 是否能映射到真实上传目录，Nginx／静态图片服务是否启动 |
| DOCX／PDF 转换失败 | Docker 是否可访问、Linux 镜像是否已拉取、后端用户是否有运行权限 |
| 发布后页面仍旧 | 是否更新前端 `dist`，检查 HTML 与主题启动文件的缓存策略 |
| 搜索框出现但筛选无效 | 涉及新增接口参数的改动是否同时发布了后端 |
| 刷新文章详情出现 404 | Nginx 是否设置 `try_files ... /index.html` 的 SPA 回退 |

## 相关文档

- [需求分析文档](docs/需求分析文档.md)
- [天气 IP 定位说明](docs/天气IP定位.md)
- [隐私迁移说明](docs/PRIVACY-MIGRATION.md)
- [代码审查记录](docs/CODE-REVIEW-2026-08-31.md)
- [审查修复记录](docs/CODE-REVIEW-FIXES-2026-08-31.md)

历史文档用于了解背景；实际运行行为以当前代码、配置与迁移为准。
