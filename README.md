# ☁️ 个人博客系统

一个基于 **Spring Boot 3 + Vue 3 + MyBatis-Plus + Element Plus** 的全栈个人博客系统，采用"晴天"主题视觉。

> 草木蔓发，春山可望。

> ⚠️ **部署后请立即在后台「个人中心」重置 admin 密码，并在「站点配置」中替换 `email` / `github` 等占位值。**

---

## ✨ 特性

- **后端**：Spring Boot 3.5 + Spring Security 6 + JWT + MyBatis-Plus 3.5 + MySQL 8 + Redis + Caffeine（本地缓存）+ SpringDoc OpenAPI
- **前端**：Vue 3.5 + Vite 6 + Pinia + Vue Router 4 + Element Plus（按需引入）+ Axios + SCSS
- **图表**：ECharts 5（后台访问统计饼图，按需引入）
- **编辑器**：Tiptap 3 所见即所得 + Markdown 互转（GFM 表格支持），后台另配 MdEditor
- **图床**：本地图床（Nginx 静态服务 + Let's Encrypt 签发 HTTPS），不依赖第三方对象存储（七牛云 SDK 已预留，可按需启用）
- **部署**：Ubuntu + Nginx 反向代理 + systemd + Let's Encrypt；一键发布脚本 `deploy.sh` / `deploy.ps1`
- **功能**：文章 / 分类 / 标签 / 评论 / 留言板 / 归档 / 搜索 / 项目集 / 工具集 / 友链 / 关于 / 站点配置 / 后台管理 / 首页天气卡 / 访问统计 / **邮箱订阅（新）**
- **天气卡**：Open-Meteo 实时天气（温度 / 体感 / 湿度 / 风力风向 / 气压 / 天气状况），含 WMO code 校验与高温降级；城市默认定位 + 访客手动搜索切换；拒绝定位时显示"位置信息暂未授权" + 重新授权
- **访问统计**：基于 ip2region 的 IP 地理解析，按设备 / 系统 / 浏览器 / 省份分布用 ECharts 饼图展示，访客明细按 IP + 日期分组折叠
- **邮箱订阅（新）**：双确认邮件订阅流（pending → confirmed → unsubscribed），公开接口按 IP 限频；管理员端含 CRUD + CSV 导出（公式注入防护）
- **内容自动通知（新）**：`@Scheduled` 定时检测新发布文章 / 项目 / 工具，邮件通知全部已确认订阅者；SMTP 未启用时自动停推且不标记 notified，启用后自动补推，避免通知丢失

## 🏗 目录结构

```
Blog_Java_Vue/
├── blog-server/             # 后端 Spring Boot (端口 8080)
│   ├── src/main/java/com/blog/
│   │   ├── common/          # 统一响应、异常、PageQuery
│   │   ├── config/          # MyBatis-Plus、安全、CORS、BlogProperties
│   │   ├── controller/      # REST 控制器
│   │   ├── dto/             # 数据传输对象
│   │   ├── entity/          # 数据库实体
│   │   ├── mapper/          # MyBatis-Plus mapper
│   │   ├── security/        # JWT + 登录用户
│   │   └── service/         # 业务逻辑（LocalStorageService 等）
│   └── src/main/resources/
│       ├── application-example.yml   # 配置模板（已脱敏，提交到仓库）
│       └── db/init.sql      # 新库初始化（建表 + 示例数据）
├── blog-web/                # 前端 Vue 3 (端口 5173)
│   └── src/
│       ├── api/             # axios 封装（front.js / admin.js / share.js）
│       ├── components/      # 通用组件（含 SkyHero 天气卡、CountUp 等）
│       ├── composables/     # 组合式函数（useWeather 等）
│       ├── layouts/         # 布局（前台 / 后台）
│       ├── router/          # 路由（含懒加载 + 导航预取）
│       ├── stores/          # Pinia
│       ├── styles/          # 全局 SCSS
│       ├── utils/           # 工具
│       └── views/
│           ├── front/       # 前台页面
│           └── admin/       # 后台管理（含 Stats 访问统计）
├── server-config/           # 部署与迁移配置
│   ├── deploy.sh            # Linux 一键发布（默认不跑 SQL）
│   ├── migration-*.sql      # 增量迁移脚本（幂等，可重复执行）
│   └── *.nginx.conf         # Nginx 站点配置示例
├── ip2region.xdb            # IP 地理库（访客统计用）
├── start-backend.bat        # Windows 启动后端
├── start-frontend.bat       # Windows 启动前端
├── deploy.ps1               # Windows 一键发布
└── README.md
```

## 🚀 快速开始（本地开发）

### 1. 准备环境

| 工具 | 版本 |
|---|---|
| JDK | 17 |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 7+（可选，用于评论防刷 / 缓存） |

### 2. 准备配置文件（⚠️ 不要提交真实配置）

```bash
cd blog-server/src/main/resources
cp application-example.yml application.yml
# 按需修改 application.yml 中的 DB / Redis / JWT / CORS / 图床 段落
```

所有敏感字段都已改为环境变量占位符（`DB_PASSWORD` / `BLOG_JWT_SECRET` / `FRONTEND_ORIGIN` / `IMG_BASE_URL` 等），
生产部署时强烈建议通过环境变量注入。**`BLOG_JWT_SECRET` 必填**（无值时 Spring 启动失败，避免意外用默认 key 签 token）。

### 3. 初始化数据库

```bash
mysql -u<user> -p<pass> < blog-server/src/main/resources/db/init.sql
```

`init.sql` 已脱敏：admin 用户的 `password` 字段是**无效占位 hash**（任何密码都登不上），
请按文件内提示用 `BCryptPasswordEncoder.encode("你的新密码")` 生成新 hash 后入库。

> 后续表结构变更请走 **增量迁移脚本**（见下文「🗄 数据库迁移规范」），不要把改动直接手改进 `init.sql` 又忘了老库。

### 4. 启动后端

```bash
# Windows
start-backend.bat

# 或手动
cd blog-server
mvn spring-boot:run
```

后端启动后访问：<http://localhost:8080>
Swagger UI：<http://localhost:8080/swagger-ui.html>

### 5. 启动前端

```bash
# Windows
start-frontend.bat

# 或手动
cd blog-web
npm install
npm run dev
```

前端启动后访问：<http://localhost:5173>

### 6. 默认账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | admin | 见 `db/init.sql` 顶部说明，需自行重置 |

## 📚 接口文档

启动后端后访问 Swagger UI：<http://localhost:8080/swagger-ui.html>

API 前缀：`/api/v1`

- 前台：`/api/v1/home`, `/api/v1/articles`, `/api/v1/categories`, `/api/v1/tags`, `/api/v1/projects`, `/api/v1/tools`, `/api/v1/friend-links`, `/api/v1/about`, `/api/v1/comments/guestbook`, `/api/v1/site`
- 订阅：`/api/v1/subscribe`（POST 订阅 / GET 确认 / GET 退订，公开）
- 后台：`/api/v1/admin/**`（需登录），含 `/api/v1/admin/stats` 访问统计 + `/api/v1/admin/subscriptions` 订阅管理
- 认证：`/api/v1/auth/login`

> 完整端点以 Swagger UI 为准；以上为常用分组示例。

## ☁️ 天气卡与访客统计

### 天气卡（`SkyHero.vue` + `useWeather.js`）

- **数据源**：Open-Meteo V1 Forecast（免费、无需 Key、CORS 允许）。`current` 取温度 / 体感 / 湿度 / 风速 / 风向 / 气压 / 天气状况。
- **天气状况校验**：`weather_code`（WMO Code）经完整映射表转中文；并做合理性校验——高温（≥30°C）+ 极端风暴码（95/96/99）属模型偏差，自动降级为「晴 / 局部晴朗 / 多云」。数值字段（温度 / 湿度 / 风力）经温度范围、湿度范围、数据时效（<2h）校验。
- **城市定位**：默认定位访客所在城市（浏览器定位 + BigDataCloud 反向地理编码中文名）；访客可点 📍 打开搜索面板，用 Open-Meteo Geocoding（中文名）手动切换城市，选定后记忆到 `localStorage`。
- **拒绝定位**：访客拒绝授权时，天气卡显示「位置信息暂未授权」+「重新授权」按钮，重新触发定位流程；**不**回退显示任何预设城市。

### 访客统计（`views/admin/Stats.vue`）

- **IP 地理解析**：基于 `ip2region.xdb`，将访客 IP 解析到省 / 国家；境外 IP 在省份列显示为「境外(美国)」等友好文案。
- **分布可视化**：设备 / 系统 / 浏览器 / 省份四类分布用 ECharts 真饼图（按需引入 `PieChart` + 必要组件）展示，空态用 Vue 模板层占位（不依赖图表库自身渲染）。
- **访客明细**：按「IP + 日期」分组折叠，汇总行展示 IP / 省份 / 设备 / 系统 / 浏览器 / 访问次数 / 时间范围，展开看当日该 IP 全部明细。

## 🎨 设计说明（晴天主题）

**主色板**

| 名称 | 色值 | 用途 |
|---|---|---|
| 天空蓝 Sky | `#38bdf8` | 品牌主色、按钮、强调 |
| 薄荷青 Mint | `#22d3ee` | 二级色、辅色、过渡 |
| 暖阳金 Sun | `#fbbf24` | 点缀色、高亮 |

**字体**
- 中文衬线：`Noto Serif SC / Songti SC`
- 中文无衬线：`PingFang SC / Microsoft YaHei`

## ☁️ 图床与部署

图床采用独立 HTTPS 子域（如 `img.yourdomain.com`），与主站分离，
避免主站反代时携带大图片流量。

浏览器 → https://img.yourdomain.com/yyyy/MM/xxx.jpg
                      ↓
                  Nginx (ssl + static)
                      ↓
           ${UPLOAD_DIR}/yyyy/MM/xxx.jpg

- 证书：Let's Encrypt，3 个月自动续期
- 上传：`LocalStorageService` 按 MD5 自动去重，按日期分目录
- URL 格式：`${IMG_BASE_URL}/{yyyy/MM}/{md5}.{ext}`

## 🗄 数据库迁移规范

> ⚠️ **铁律**：改 Java 实体 / SQL 前，必须先把对应的迁移 SQL 跑进目标库，否则 MyBatis-Plus 会报 `Unknown column` / `Data too long` 导致接口 500。

- **新库**：`blog-server/src/main/resources/db/init.sql`（建表 + 示例数据）。**每次加表 / 加列必须 `init.sql` 与迁移脚本双写**，否则新库照样缺列。
- **增量**：`server-config/migration-*.sql`，统一用 `information_schema` 探测 + `ALTER ADD COLUMN` / `CREATE TABLE IF NOT EXISTS` + `ON DUPLICATE KEY UPDATE`，**幂等、可重复执行**。
  - ⚠️ 注意：`CREATE TABLE IF NOT EXISTS` 在表已存在时会整段跳过，**不会补列**。老库补列必须写 `information_schema` 探测 + 动态 `PREPARE/EXECUTE ALTER` 段（`no-op` 分支用 `DO 0`）。
- **部署时执行迁移**：
  - Linux：`RUN_MIGRATION=1 DB_PASS='<pwd>' bash server-config/deploy.sh`（默认不跑 SQL）
  - Windows：`deploy.ps1` 加 `-Migrate` 参数或设 `$RunMigration=$true`
  - 迁移脚本按文件名顺序逐个应用，失败会打印 `FAILED` 但不中断其余脚本。

## 🛠 技术栈版本

| 技术 | 版本 |
|---|---|
| Spring Boot | 3.5.6 |
| Java | 17 |
| MyBatis-Plus | 3.5.7 |
| Spring Security | 6.x |
| jjwt | 0.12.6 |
| SpringDoc OpenAPI | 2.6.0 |
| MySQL | 8.0 |
| Redis | 7.x |
| Vue | 3.5 |
| Vite | 6 |
| Element Plus | 2.9（按需引入：unplugin-vue-components + unplugin-auto-import） |
| ECharts | 5.5 |
| Pinia | 2.x |
| Tiptap | 3.x |
| md-editor-v3 | 4.x |
| highlight.js | 11.x |

## 🚢 生产部署（Ubuntu）

1. **服务器初始化**：创建 `blog` 用户，安装 JDK 17、MySQL、Redis、Nginx、certbot
2. **初始化数据库**：将 `db/init.sql` 导入 MySQL，按文件内提示重置 admin 密码
3. **打包后端**：
   ```bash
   cd blog-server
   mvn -DskipTests clean package
   ```
4. **构建前端**：
   ```bash
   cd blog-web
   npm install
   npm run build        # 产物在 blog-web/dist
   ```
5. **一键发布**（Linux）：
   ```bash
   PROJECT_ROOT=/path/to/Blog_Java_Vue \
   RUN_MIGRATION=1 DB_PASS='<pwd>' \
   bash server-config/deploy.sh
   ```
   - 备份并覆盖 jar（chown `blog:blog`）、灌入 `dist`、重启 systemd + reload Nginx、健康检查
   - 需应用增量迁移时务必带 `RUN_MIGRATION=1`（否则只发代码不跑 SQL）
   - Windows 用 `deploy.ps1`（参数 `-Migrate` 开启迁移）
6. **（首次 / 手动）配置 systemd**：写入 `/etc/systemd/system/blog.service`
7. **（首次 / 手动）配置 Nginx**：主域（前台 + `/api/` 反代到 `127.0.0.1:8080`）、图床子域（HTTPS 静态服务）
8. **申请证书**：
   ```bash
   sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com -d img.yourdomain.com
   ```

> 详细 SOP 见团队《博客部署上线教程》；`deploy.sh` 顶部注释含全部可覆盖的环境变量（`BACKEND_DIR` / `FRONTEND_DIR` / `SKIP_FRONTEND` / `SKIP_BACKEND` 等）。

## 🔐 安全 / 隐私注意事项

1. **重置 admin 密码**：部署后立即在后台「个人中心 → 修改密码」
2. **不要将 `application.yml` / `application-prod.yml` 提交到仓库**——本仓库已通过 `.gitignore` 屏蔽这两个文件，只保留 `application-example.yml` 模板
3. **不要打开外网端口**：3306 / 6379 / 8080 仅监听 `127.0.0.1`，通过 Nginx 443 反代访问
4. **MySQL 用户**：应用使用低权限用户，不要用 `root` 连接 Spring Boot
5. **图床目录权限**：建议 `drwxr-x---`，Nginx 以独立用户读取
6. **JWT secret**：生产环境通过 `BLOG_JWT_SECRET` 环境变量注入，至少 64 字节随机
7. **访客隐私**：访问统计仅记录 IP 与派生地理位置用于展示分布，不展示完整 IP 明细给非授权用户
8. **评论者隐私**：公开评论 API 通过 `CommentPublicVO` 返回，自动剥离邮箱 / IP / UA 等字段，匿名访客无法读取评论者隐私信息

## 📝 License

MIT
