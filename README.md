# Blog Java Vue

一个采用“晴天”视觉主题的前后端分离个人博客系统。项目包含公开博客、内容管理后台、评论与留言、项目和工具展示、访问统计、邮箱订阅，以及文章导入导出等功能。

> 草木蔓发，春山可望。

## 功能概览

### 公开站点

- 首页、文章列表与详情、全文搜索、分类、标签和归档
- 项目集、工具集、友链、留言板和关于页面
- 文章评论、回复、点赞、举报及访客头像上传
- Open-Meteo 天气卡，默认按浏览器公网 IP 估算位置（不主动申请定位权限），支持用户授权设备精确定位及城市/区县搜索
- 文章、项目和工具的分享点击统计
- 邮箱订阅、确认订阅、退订和内容更新通知
- 响应式布局、路由懒加载和“晴天”主题动效

天气定位精度、可选区县数据源与部署配置见 [天气 IP 定位说明](docs/天气IP定位.md)。

### 管理后台

- 仪表盘和站点配置
- 文章、分类、标签和归档管理
- Tiptap 富文本编辑，支持 Markdown、DOCX 导入和 Markdown、DOCX、PDF 导出
- 评论、留言和举报审核
- 项目、项目分类、工具、工具分类和友链管理
- 订阅用户管理及 CSV 导出
- 访问日志和设备、系统、浏览器、地区分布统计
- 管理员资料与密码维护

### 安全与运行特性

- Spring Security + JWT 无状态认证
- BCrypt 管理员密码，不提供可登录的默认密码
- 登录、评论、订阅、分享和上传接口限频
- 图片扩展名与文件签名校验、尺寸限制和自动压缩
- Caffeine 本地缓存和进程内 JWT 黑名单
- 公开评论与站点配置响应脱敏
- SMTP 默认关闭，通知任务可独立启停

## 技术栈

| 层级 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.5.6、Spring Security 6、MyBatis-Plus 3.5.7 |
| 数据 | MySQL 8、Caffeine；当前单实例限流和 JWT 黑名单使用进程内存 |
| API | REST、JWT、SpringDoc OpenAPI 2.6.0 |
| 前端 | Vue 3.5、Vite 6、Vue Router 4.5、Pinia 2.3、Axios 1.7 |
| UI | Element Plus 2.9、SCSS、ECharts 5.5 |
| 编辑器 | Tiptap 3、md-editor-v3、marked、Turndown |
| 可选集成 | SMTP、Pandoc、ip2region、Open-Meteo、IPWhois、IP2Location.io |

> 当前业务不依赖 Redis，无需启动 Redis。JWT 撤销状态保存在数据库；限流使用进程内存，多实例部署时需要为限流配置共享存储。

## 项目结构

```text
Blog_Java_Vue/
├── blog-server/                         # Spring Boot 后端
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/blog/
│       │   ├── common/                  # 统一响应与异常处理
│       │   ├── config/                  # 安全、缓存、OpenAPI、任务配置
│       │   ├── controller/              # 公开与后台 REST API
│       │   ├── dto/                     # 请求与响应对象
│       │   ├── entity/                  # MyBatis-Plus 实体
│       │   ├── mapper/                  # 数据访问层
│       │   ├── security/                # JWT、限流、客户端 IP 解析
│       │   ├── service/                 # 业务服务
│       │   └── vo/                      # 公开脱敏视图对象
│       ├── main/resources/
│       │   ├── application-example.yml  # 可提交的配置模板
│       │   ├── application-dev.yml      # 开发环境非敏感配置
│       │   ├── db/init.sql              # 新数据库结构与示例数据
│       │   └── templates/               # DOCX 导出模板
│       └── test/                         # 后端测试
├── blog-web/                            # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   ├── design-prototypes/               # 页面设计原型
│   └── src/
│       ├── api/                          # 前后台 API 封装
│       ├── components/                   # 通用组件与视觉效果
│       ├── composables/                  # 天气、交互和动画逻辑
│       ├── layouts/                      # 前台与后台布局
│       ├── router/                       # 路由与后台鉴权
│       ├── stores/                       # Pinia 状态
│       ├── styles/                       # 全局样式
│       └── views/                        # 前台与后台页面
├── docs/                                # 需求、隐私迁移和文章草稿
├── scripts/privacy-scan.ps1             # 隐私信息扫描
├── start-backend.bat                    # Windows 后端启动辅助脚本
├── start-frontend.bat                   # Windows 前端启动辅助脚本
├── reset-db.bat                         # 本地数据库重置脚本（会删除数据库）
└── README.md
```

## 本地开发

### 1. 环境要求

- JDK 17
- Maven 3.8+
- Node.js 18+ 和 npm
- MySQL 8.0+

以下组件按需安装：

- Pandoc：使用 DOCX/PDF 导入导出时需要
- SMTP 服务：订阅确认、评论通知和内容通知需要
- `ip2region.xdb`：需要在访问统计中解析地区时使用
- Nginx 或其他静态文件服务器：需要查看本地上传图片时使用

### 2. 初始化数据库

将 [`blog-server/src/main/resources/db/init.sql`](blog-server/src/main/resources/db/init.sql) 导入 MySQL 8。

```bash
mysql -u root -p < blog-server/src/main/resources/db/init.sql
```

Windows PowerShell 可通过 `cmd` 调用重定向：

```powershell
cmd /c "mysql -u root -p < blog-server\src\main\resources\db\init.sql"
```

> `init.sql` 会执行 `DROP DATABASE IF EXISTS blog_java_vue`。它只适合初始化新环境，会完整删除同名数据库；已有数据的环境必须先备份，并使用经过审核的增量 SQL。

推荐为应用创建独立的低权限账号，不要让 Spring Boot 使用 MySQL `root`：

```sql
CREATE USER IF NOT EXISTS 'blog'@'localhost' IDENTIFIED BY 'replace_with_a_strong_password';
GRANT ALL PRIVILEGES ON blog_java_vue.* TO 'blog'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 创建本地配置

直接双击 `start-backend.bat` 时，如果没有本地配置，启动器会隐藏输入 MySQL 密码并为本次运行生成临时 JWT 密钥。需要让登录状态在重启后继续有效时，复制本地启动模板：

```powershell
Copy-Item start-backend.local.bat.example start-backend.local.bat
```

然后在 `start-backend.local.bat` 中填写数据库密码和固定的随机 JWT 密钥。该文件已被 `.gitignore` 忽略，不应提交到仓库。

至少准备以下配置：

| 变量 | 是否必需 | 说明 |
|---|---:|---|
| `DB_URL` | 否 | 默认连接 `localhost:3306/blog_java_vue` |
| `DB_USERNAME` | 否 | 默认 `blog` |
| `DB_PASSWORD` | 视数据库而定 | MySQL 应用账号密码 |
| `BLOG_JWT_SECRET` | 是 | JWT 密钥，至少 32 字节随机字符串 |
| `BLOG_ADMIN_INITIAL_PASSWORD` | 首次登录必需 | 首次启动时初始化 `admin`，至少 12 个字符 |
| `UPLOAD_DIR` | 否 | 图片目录，默认 `./uploads/img` |
| `IMG_BASE_URL` | 是 | 图片对外访问 URL 前缀；为空时后端拒绝启动 |

PowerShell 示例：

```powershell
$env:DB_USERNAME = "blog"
$env:DB_PASSWORD = "<your-database-password>"
$env:BLOG_JWT_SECRET = "<your-random-secret-of-at-least-32-bytes>"
$env:BLOG_ADMIN_INITIAL_PASSWORD = "<your-strong-initial-password>"
$env:IMG_BASE_URL = "http://localhost:9000"
```

管理员密码只会在数据库仍为初始化标记时写入一次。第一次成功启动后，应从运行环境中移除一次性变量：

```powershell
Remove-Item Env:BLOG_ADMIN_INITIAL_PASSWORD
```

### 4. 启动后端

在设置环境变量的同一个终端中运行：

```powershell
Set-Location blog-server
mvn spring-boot:run
```

可用地址：

- 健康检查：<http://localhost:8080/api/v1/ping>
- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>

Swagger 只在 `dev` Profile 下开放。仓库中的 `start-backend.bat` 带有开发机器专用的 `JAVA_HOME`，其他电脑使用前需要修改；通用启动方式以上面的 Maven 命令为准。

### 5. 启动前端

打开第二个终端：

```powershell
Set-Location blog-web
npm ci
npm run dev
```

访问地址：

- 公开站点：<http://localhost:5173>
- 管理后台：<http://localhost:5173/admin/login>

Vite 会把 `/api` 和 `/uploads` 请求代理到 `http://localhost:8080`。

### 6. 本地图片访问

上传服务会把图片写入 `UPLOAD_DIR`，但当前 Spring Boot 不直接暴露该目录。生产环境应由 Nginx 或独立图片域名提供静态文件服务。

如果本地将 `IMG_BASE_URL` 设置为 `http://localhost:9000`，可以在项目根目录临时启动静态服务器：

```powershell
python -m http.server 9000 --directory blog-server/uploads/img
```

不测试上传功能时，只需保证 `IMG_BASE_URL` 非空即可启动后端。

## 可选功能配置

### 邮件、订阅与内容通知

邮件功能默认关闭。启用前至少配置：

```text
BLOG_MAIL_ENABLED=true
BLOG_MAIL_HOST=...
BLOG_MAIL_PORT=...
BLOG_MAIL_USERNAME=...
BLOG_MAIL_PASSWORD=...
BLOG_MAIL_FROM_EMAIL=...
BLOG_MAIL_ADMIN_EMAIL=...
```

订阅确认和通知邮件中的公开链接还需要配置：

```text
BLOG_SITE_BASE_URL=https://your-domain.example
BLOG_SUBSCRIBE_BASE_URL=https://your-domain.example
BLOG_NOTIFY_BASE_URL=https://your-domain.example
BLOG_NOTIFY_ENABLED=true
```

SMTP 未启用或发送失败时，业务请求不会因为普通异步通知而中断；订阅确认等关键流程会返回实际发送状态。

### IP 地区解析

仓库不提交大型 `ip2region.xdb` 数据文件。下载兼容 ip2region 2.x 的 XDB 后，通过以下方式指定：

```text
IP2_REGION_DB_PATH=D:/path/to/ip2region.xdb
```

文件缺失时，访问统计仍可工作，但地区字段为空。

### Pandoc 导入导出

Markdown 导入导出不依赖额外程序。DOCX/PDF 转换需要安装 Pandoc，并在无法从 `PATH` 发现时指定：

```text
BLOG_PANDOC_BIN=D:/path/to/pandoc.exe
```

生成中文 PDF 还需要系统中存在可用的 PDF 引擎和中文字体。

## API 分组

所有业务 API 使用 `/api/v1` 前缀。

| 分组 | 路径示例 |
|---|---|
| 系统 | `/api/v1/ping` |
| 认证 | `/api/v1/auth/login`、`/api/v1/auth/me` |
| 文章 | `/api/v1/articles`、`/api/v1/categories`、`/api/v1/tags` |
| 内容 | `/api/v1/projects`、`/api/v1/tools`、`/api/v1/friend-links` |
| 互动 | `/api/v1/comments`、`/api/v1/share`、`/api/v1/upload/avatar` |
| 订阅 | `/api/v1/subscribe` |
| 管理后台 | `/api/v1/admin/**` |

完整参数、响应结构和状态码以开发环境的 Swagger UI 为准。

## 构建与检查

后端测试与打包：

```powershell
Set-Location blog-server
mvn test
mvn -DskipTests clean package
```

产物为 `blog-server/target/blog-server.jar`。

前端生产构建：

```powershell
Set-Location blog-web
npm ci
npm run build
```

产物位于 `blog-web/dist/`。

提交公开仓库前可运行隐私扫描：

```powershell
pwsh ./scripts/privacy-scan.ps1
pwsh ./scripts/privacy-scan.ps1 -History
```

## 生产部署说明

当前公开仓库不包含生产服务器地址、systemd 单元、私钥或一键部署脚本；提供了可审查、无凭据的 [Nginx 安全基线](deployment/nginx/example.com.conf.example)。部署前仍须按目标主机复核路径和证书位置。一个完整部署至少需要：

1. 设置 `SPRING_PROFILES_ACTIVE=prod`，并从外部文件或环境变量注入全部敏感配置。
2. 运行后端 JAR，仅向反向代理暴露服务端口。
3. 使用 Nginx 等静态服务器托管 `blog-web/dist/` 的构建产物；当前发布目录为 `/home/blog/blog/frontend`。
4. 将 `/api/` 反向代理到 Spring Boot。
5. 独立托管 `UPLOAD_DIR`，并让公开地址与 `IMG_BASE_URL` 一致。
6. 配置 HTTPS、日志轮转、数据库备份和最小权限账号。

仓库目前只提供全量初始化 SQL，没有可直接用于生产老库的增量迁移脚本。升级已有数据库前必须先备份并审查表结构差异。

## 安全注意事项

- 不要提交 `application.yml`、`.env`、数据库备份、证书、令牌或真实服务器配置。
- `admin` 没有默认密码；不要长期保留 `BLOG_ADMIN_INITIAL_PASSWORD`。
- 生产 JWT 密钥应使用密码学安全的随机值，并至少包含 32 字节。
- `reset-db.bat` 和 `init.sql` 都会删除 `blog_java_vue` 数据库，只能用于可丢弃的本地环境。
- 生产环境不要直接暴露 MySQL、后端管理端口或上传目录写权限。
- 当前限流、缓存和 JWT 黑名单面向单实例设计；多实例部署前必须改为共享状态方案。
- 访问统计会处理访客 IP 和 User-Agent，部署时应根据适用法律提供隐私说明和保留策略。
- 公开仓库清理和凭据轮换流程参见 [`docs/PRIVACY-MIGRATION.md`](docs/PRIVACY-MIGRATION.md)。

## 仓库状态说明

- 本项目当前没有独立的 `LICENSE` 文件。
- `target/`、`dist/`、`node_modules/`、上传文件、私密配置和可执行生产部署文件不会进入 Git；仓库只保留脱敏的部署基线模板。
- 设计原型位于 [`blog-web/design-prototypes/`](blog-web/design-prototypes/)，需求文档位于 [`docs/需求分析文档.md`](docs/需求分析文档.md)。
