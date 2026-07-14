# ☁️ DemoAuthor · 个人博客系统

一个基于 **Spring Boot 3 + Vue 3 + MyBatis-Plus + Element Plus** 的全栈个人博客系统，采用"晴天"主题视觉。

> 草木蔓发，春山可望。

> ⚠️ **上线部署后请立即在后台「个人中心」修改 admin 初始密码，并替换 `site_config` 中 `email` / `github` 的占位值。**

---

## ✨ 特性

- **后端**：Spring Boot 3.3 + Spring Security 6 + JWT + MyBatis-Plus 3.5 + MySQL 8 + Redis + Swagger
- **前端**：Vue 3 + Vite 6 + Pinia + Vue Router 4 + Element Plus 2 + Axios + SCSS
- **图床**：本地图床（Nginx 静态服务 + Let's Encrypt 签发 HTTPS），不再依赖第三方对象存储
- **部署**：Alibaba Cloud Ubuntu 24.04 + Nginx 反向代理 + systemd + Let's Encrypt
- **功能**：文章 / 分类 / 标签 / 评论 / 留言 / 归档 / 搜索 / 项目 / 友链 / 站点配置 / 后台管理

## 🏗 目录结构

```
Blog_Java_Vue/
├── blog-server/             # 后端 Spring Boot (端口 8080)
│   ├── src/main/java/com/blog/
│   │   ├── common/          # 统一响应、异常、PageQuery
│   │   ├── config/          # MyBatis-Plus、安全、CORS、BlogProperties 配置
│   │   ├── controller/      # REST 控制器
│   │   ├── dto/             # 数据传输对象
│   │   ├── entity/          # 数据库实体
│   │   ├── mapper/          # MyBatis-Plus mapper
│   │   ├── security/        # JWT + 登录用户
│   │   └── service/         # 业务逻辑（LocalStorageService 等）
│   └── src/main/resources/
│       ├── application.yml
│       └── db/init.sql      # 初始化数据库 + 示例数据
├── blog-web/                # 前端 Vue 3 (端口 5173)
│   └── src/
│       ├── api/             # axios 封装
│       ├── components/      # 通用组件（AppHeader、HeroSection、SkyHero 等）
│       ├── layouts/         # 布局 (前台 / 后台)
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

### 2. 初始化数据库

```bash
mysql -uroot -p123456 < blog-server/src/main/resources/db/init.sql
```

### 3. 启动后端

```bash
# Windows
start-backend.bat

# 或手动
cd blog-server
mvn spring-boot:run
```

后端启动后访问：<http://localhost:8080>

### 4. 启动前端

```bash
# Windows
start-frontend.bat

# 或手动
cd blog-web
npm install
npm run dev
```

前端启动后访问：<http://localhost:5173>

### 5. 默认账号

| 角色 | 用户名 | 初始密码 |
|---|---|---|
| 管理员 | admin | `BOOTSTRAP_REQUIRED` |

> ⚠️ 该初始密码写在 `init.sql` 中，任何拿到源码的人都能看到。**部署后请第一时间在后台修改。**

### 6. 本地图床（本地开发）

本地开发时图片存到本地，配置在 `application.yml`：

```yaml
blog:
  local-storage:
    dir: D:/AppData/uploads/img    # Windows 本地路径
    base-url: https://img.example.com   # 生产图床域名
```

> 本地开发时把 `base-url` 临时改为 `http://localhost/img` 即可，无需真实域名。

## 📚 接口文档

启动后端后访问 Swagger UI：<http://localhost:8080/swagger-ui.html>

API 前缀：`/api/v1`

- 前台：`/api/v1/home`, `/api/v1/articles`, `/api/v1/categories`, `/api/v1/tags`, `/api/v1/projects`, `/api/v1/friend-links`, ...
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

## ☁️ 生产图床

部署后图片走独立域名 `img.example.com`：

```
浏览器 → https://img.example.com/2026/07/xxx.jpg
                  ↓
              Nginx (ssl + static)
                  ↓
        /home/blog/blog/uploads/img/2026/07/xxx.jpg
```

- 证书：Let's Encrypt，3 个月自动续期
- 上传：Spring Boot `LocalStorageService` 按 MD5 自动去重，按日期分目录
- URL 格式：`https://img.example.com/{yyyy/MM}/{md5}.{ext}`

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

## 🚢 生产部署（阿里云 Ubuntu 24.04）

简要步骤（详见个人部署笔记）：

1. **服务器初始化**：创建 `blog` 用户，安装 JDK 17、MySQL、Redis、Nginx、certbot
2. **初始化数据库**：将 `init.sql` 导入 MySQL
3. **打包后端**：
   ```bash
   cd blog-server
   mvn -DskipTests clean package
   ```
4. **上传 jar 与 prod 配置**：
   ```bash
   scp target/blog-server.jar blog@<server>:/home/blog/blog/jar/
   scp application-prod.yml blog@<server>:/home/blog/blog/jar/
   ```
5. **配置 systemd**：写入 `/etc/systemd/system/blog.service`
6. **配置 Nginx**：
   - `example.com`：前台 + 反代 `/api/` 到 `127.0.0.1:8080`
   - `img.example.com`：HTTPS 静态图床
7. **申请证书**：
   ```bash
   sudo certbot --nginx -d example.com -d www.example.com -d img.example.com
   ```
8. **启动服务**：
   ```bash
   sudo systemctl enable --now blog
   sudo systemctl reload nginx
   ```

## 🔐 安全注意事项

1. **修改 admin 初始密码**：上线后立即在后台「个人中心 → 修改密码」
2. **不要打开外网端口**：3306 / 6379 / 8080 仅监听 127.0.0.1，只通过 Nginx 443 反代访问
3. **不要 push `application-prod.yml`**：里面包含数据库密码、JWT secret，必须 `.gitignore`
4. **MySQL 用户**：应用使用低权限用户 `blog_app`，不要用 `root` 连接 Spring Boot
5. **图床目录权限**：建议 `drwxr-x---`，所有者 `blog:blog`，Nginx 用 `www-data` 用户读取
6. **七牛云 AK/SK**：如果以后重新启用第三方存储，**严禁**写入公开仓库

## 📝 License

MIT