# ☁️ DemoAuthor · 个人博客系统

一个基于 **Spring Boot 3 + Vue 3 + MyBatis-Plus + Element Plus** 的全栈个人博客系统，采用"晴天"主题视觉。

> 草木蔓发，春山可望。

> ⚠️ **部署后请立即在后台「个人中心」重置 admin 密码，并在「站点配置」中替换 `email` / `github` 等占位值。**

---

## ✨ 特性

- **后端**：Spring Boot 3.3 + Spring Security 6 + JWT + MyBatis-Plus 3.5 + MySQL 8 + Redis + SpringDoc OpenAPI
- **前端**：Vue 3 + Vite 6 + Pinia + Vue Router 4 + Element Plus 2 + Axios + SCSS
- **图床**：本地图床（Nginx 静态服务 + Let's Encrypt 签发 HTTPS），不依赖第三方对象存储
- **部署**：Ubuntu 24.04 + Nginx 反向代理 + systemd + Let's Encrypt
- **功能**：文章 / 分类 / 标签 / 评论 / 留言 / 归档 / 搜索 / 项目 / 友链 / 站点配置 / 后台管理
- **编辑器**：Tiptap 所见即所得 + Markdown 互转（GFM 表格支持）

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
│       └── db/init.sql      # 初始化数据库 + 示例数据
├── blog-web/                # 前端 Vue 3 (端口 5173)
│   └── src/
│       ├── api/             # axios 封装
│       ├── components/      # 通用组件
│       ├── layouts/         # 布局（前台 / 后台）
│       ├── router/          # 路由
│       ├── stores/          # Pinia
│       ├── styles/          # 全局 SCSS
│       ├── utils/           # 工具
│       └── views/
│           ├── front/       # 前台页面
│           └── admin/       # 后台管理
├── start-backend.bat        # Windows 启动后端
├── start-frontend.bat       # Windows 启动前端
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

所有敏感字段都已改为环境变量占位符（`DB_PASSWORD` / `JWT_SECRET` / `FRONTEND_ORIGIN` / `IMG_BASE_URL` 等），
生产部署时强烈建议通过环境变量注入。

### 3. 初始化数据库

```bash
mysql -u<user> -p<pass> < blog-server/src/main/resources/db/init.sql
```

`init.sql` 已脱敏：admin 用户的 `password` 字段是**无效占位 hash**（任何密码都登不上），
请按文件内提示用 `BCryptPasswordEncoder.encode("你的新密码")` 生成新 hash 后入库。

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

- 前台：`/api/v1/home`, `/api/v1/articles`, `/api/v1/categories`, `/api/v1/tags`, `/api/v1/projects`, `/api/v1/friend-links`
- 后台：`/api/v1/admin/**`（需登录）
- 认证：`/api/v1/auth/login`

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

## 🛠 技术栈版本

| 技术 | 版本 |
|---|---|
| Spring Boot | 3.3.4 |
| Java | 17 |
| MyBatis-Plus | 3.5.7 |
| Spring Security | 6.3 |
| jjwt | 0.12.6 |
| SpringDoc OpenAPI | 2.6.0 |
| MySQL | 8.0 |
| Redis | 7.x |
| Vue | 3.5 |
| Vite | 6 |
| Element Plus | 2.9 |
| Pinia | 2.x |

## 🚢 生产部署（Ubuntu 24.04）

1. **服务器初始化**：创建 `blog` 用户，安装 JDK 17、MySQL、Redis、Nginx、certbot
2. **初始化数据库**：将 `init.sql` 导入 MySQL，按文件内提示重置 admin 密码
3. **打包后端**：
   ```bash
   cd blog-server
   mvn -DskipTests clean package
   ```
4. **上传 jar 与 prod 配置**：
   ```bash
   scp target/blog-server.jar <user>@<server>:/srv/blog/jar/
   scp application-prod.yml   <user>@<server>:/srv/blog/jar/
   ```
5. **配置 systemd**：写入 `/etc/systemd/system/blog.service`
6. **配置 Nginx**：
   - 主域（前台 + `/api/` 反代到 `127.0.0.1:8080`）
   - 图床子域（HTTPS 静态服务）
7. **申请证书**：
   ```bash
   sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com -d img.yourdomain.com
   ```
8. **启动服务**：
   ```bash
   sudo systemctl enable --now blog
   sudo systemctl reload nginx
   ```

## 🔐 安全 / 隐私注意事项

1. **重置 admin 密码**：部署后立即在后台「个人中心 → 修改密码」
2. **不要将 `application.yml` / `application-prod.yml` 提交到仓库**——本仓库已通过 `.gitignore` 屏蔽这两个文件，只保留 `application-example.yml` 模板
3. **不要打开外网端口**：3306 / 6379 / 8080 仅监听 `127.0.0.1`，通过 Nginx 443 反代访问
4. **MySQL 用户**：应用使用低权限用户，不要用 `root` 连接 Spring Boot
5. **图床目录权限**：建议 `drwxr-x---`，Nginx 以独立用户读取
6. **JWT secret**：生产环境通过 `JWT_SECRET` 环境变量注入，至少 64 字节随机

## 📝 License

MIT
