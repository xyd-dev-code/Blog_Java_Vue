-- ===============================================================
--  Blog Java Vue - Database Schema + Seed Data
--  MySQL 8.0+
-- ===============================================================

DROP DATABASE IF EXISTS blog_java_vue;
CREATE DATABASE blog_java_vue DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE blog_java_vue;

-- ---------------------------------------------------------------
-- 用户表
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50),
    email       VARCHAR(100),
    avatar      VARCHAR(255),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status      TINYINT      NOT NULL DEFAULT 1,
    last_login  DATETIME,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='用户';

-- ---------------------------------------------------------------
-- 分类
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS category;
CREATE TABLE category (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    description VARCHAR(255),
    color       VARCHAR(20)  DEFAULT '#38bdf8',
    icon        VARCHAR(50),
    sort_order  INT          NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='文章分类';

-- ---------------------------------------------------------------
-- 标签
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS tag;
CREATE TABLE tag (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    description VARCHAR(255),
    color       VARCHAR(20),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='文章标签';

-- ---------------------------------------------------------------
-- 文章
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS article;
CREATE TABLE article (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    title          VARCHAR(200) NOT NULL,
    slug           VARCHAR(200) NOT NULL UNIQUE,
    summary        VARCHAR(500),
    content        LONGTEXT     NOT NULL,
    cover_image    VARCHAR(255),
    category_id    BIGINT,
    view_count     INT          NOT NULL DEFAULT 0,
    share_count    INT          NOT NULL DEFAULT 0,
    comment_count  INT          NOT NULL DEFAULT 0,
    like_count     INT          NOT NULL DEFAULT 0,
    is_top         TINYINT      NOT NULL DEFAULT 0,
    is_featured    TINYINT      NOT NULL DEFAULT 0,
    allow_comment  TINYINT      NOT NULL DEFAULT 1,
    password       VARCHAR(100) DEFAULT NULL,
    status         TINYINT      NOT NULL DEFAULT 1,
    notified       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已推送订阅邮件(0=未 1=已)',
    publish_time   DATETIME,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_status_time (status, publish_time),
    INDEX idx_category (category_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='文章';

-- ---------------------------------------------------------------
-- 文章-标签关联
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS article_tag;
CREATE TABLE article_tag (
    article_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    INDEX idx_tag (tag_id)
) ENGINE=InnoDB COMMENT='文章-标签关联';

-- ---------------------------------------------------------------
-- 评论
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS comment;
CREATE TABLE comment (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id  BIGINT       NOT NULL,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    nickname    VARCHAR(50)  NOT NULL,
    email       VARCHAR(100),
    website     VARCHAR(255),
    content     TEXT         NOT NULL,
    avatar      VARCHAR(255),
    ip          VARCHAR(50),
    status      TINYINT      NOT NULL DEFAULT 0,
    like_count  INT          NOT NULL DEFAULT 0,
    report_count INT         NOT NULL DEFAULT 0,
    ua          VARCHAR(255) NOT NULL DEFAULT '',
    content_type TINYINT     NOT NULL DEFAULT 0,
    featured   TINYINT      NOT NULL DEFAULT 0 COMMENT '人工置顶：1=精选留言',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_article (article_id),
    INDEX idx_status (status),
    INDEX idx_featured (featured)
) ENGINE=InnoDB COMMENT='评论';

-- 评论点赞记录（按 IP / 登录用户去重，同一人同一留言仅能点一次）
DROP TABLE IF EXISTS comment_like;
CREATE TABLE comment_like (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id   BIGINT       NOT NULL,
    ip           VARCHAR(45)  NOT NULL DEFAULT '',
    user_id      BIGINT       NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_like_ip   (comment_id, ip),
    UNIQUE KEY uq_like_user (comment_id, user_id),
    INDEX idx_comment (comment_id)
) ENGINE=InnoDB COMMENT='评论点赞记录';

-- 评论举报记录
DROP TABLE IF EXISTS comment_report;
CREATE TABLE comment_report (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id   BIGINT       NOT NULL,
    reason       VARCHAR(50)  NOT NULL,
    detail       VARCHAR(500) DEFAULT '',
    email        VARCHAR(100) DEFAULT '',
    ip           VARCHAR(45)  NOT NULL DEFAULT '',
    status       TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_comment (comment_id),
    INDEX idx_status  (status)
) ENGINE=InnoDB COMMENT='评论举报记录';

-- ---------------------------------------------------------------

-- ---------------------------------------------------------------
-- 项目（作品集）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS project;
CREATE TABLE project (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    tech_stack  VARCHAR(500),
    icon        VARCHAR(50)  DEFAULT 'Folder',
    color       VARCHAR(50)  DEFAULT '#38bdf8',
    github_url  VARCHAR(255),
    demo_url    VARCHAR(255),
    cover_url   VARCHAR(512) DEFAULT '',
    category_id BIGINT       DEFAULT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    notified    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已推送订阅邮件(0=未 1=已)',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_sort (sort_order),
    INDEX idx_category (category_id)
) ENGINE=InnoDB COMMENT='项目';

-- ---------------------------------------------------------------
-- 项目分类（与前台筛选联动，后台可管理）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS project_category;
CREATE TABLE project_category (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    color       VARCHAR(50)  DEFAULT '#38bdf8',
    description VARCHAR(255) DEFAULT '',
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB COMMENT='项目分类';

-- 项目分类种子(原 project.kind 的 开源/工具/实验 收口为单一可管理分类体系)
INSERT INTO project_category (name, slug, color, description, sort_order, status) VALUES
('开源',   'open-source', '#38bdf8', '对外开源、可协作的项目', 1, 1),
('工具',   'tool',        '#fbbf24', '提升效率的实用工具',     2, 1),
('实验',   'experiment',  '#0ea5e9', '探索性、练手型的小实验', 3, 1);

-- ---------------------------------------------------------------
-- 友情链接
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS friend_link;
CREATE TABLE friend_link (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    url         VARCHAR(255) NOT NULL,
    avatar      VARCHAR(255),
    description VARCHAR(255),
    email       VARCHAR(100),
    link_group  VARCHAR(32)  NOT NULL DEFAULT '网友',
    sort_order  INT          NOT NULL DEFAULT 0,
    recommended TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '人工推荐：1=前台显示推荐徽章',
    status      TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_sort (sort_order),
    INDEX idx_recommended (recommended)
) ENGINE=InnoDB COMMENT='友情链接';

-- ---------------------------------------------------------------
-- 站点配置 (k-v)
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS site_config;
CREATE TABLE site_config (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key  VARCHAR(80)  NOT NULL UNIQUE,
    config_value  TEXT,
    description VARCHAR(255),
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='站点配置';

CREATE TABLE share_log (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id   BIGINT       NOT NULL,
    channel      VARCHAR(16)  NOT NULL COMMENT 'wechat/weibo/qq/douban/copy/link',
    ip           VARCHAR(64),
    user_agent   VARCHAR(255),
    share_date   DATE         NOT NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article_day (article_id, share_date),
    INDEX idx_day_channel (share_date, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享点击日志(每日同 IP+UA+渠道去重)';

-- ---------------------------------------------------------------
-- 邮箱订阅（原 RSS 订阅的替代方案，双重确认）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS email_subscription;
CREATE TABLE email_subscription (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    email        VARCHAR(120) NOT NULL COMMENT '订阅邮箱(统一小写存储)',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待确认 1=已确认',
    token        VARCHAR(64)  DEFAULT NULL COMMENT '确认令牌(UUID 去横杠)',
    source       VARCHAR(20)  DEFAULT 'web' COMMENT '订阅来源',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirm_time DATETIME     DEFAULT NULL COMMENT '确认时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(@TableLogic)',
    UNIQUE KEY uk_email (email),
    KEY idx_status (status),
    KEY idx_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱订阅';

-- ===============================================================
--                       Seed Data
-- ===============================================================

-- 默认管理员账号 admin，但不提供任何可登录的默认密码。
-- 首次启动前通过 BLOG_ADMIN_INITIAL_PASSWORD 注入至少 12 位强密码；应用仅在密码仍为
-- !BOOTSTRAP_REQUIRED! 时写入 BCrypt 摘要，完成后必须从运行环境删除该一次性变量。
INSERT INTO user (username, password, nickname, email, role, status) VALUES
('admin', '!BOOTSTRAP_REQUIRED!', '站长', 'your_email@example.com', 'ADMIN', 1);

-- 分类
INSERT INTO category (name, slug, description, color, sort_order) VALUES
('技术', 'tech', '代码、技术与工程', '#38bdf8', 1),
('生活', 'life', '生活随笔、思考与感悟', '#fbbf24', 2),
('设计', 'design', '界面与交互设计', '#0ea5e9', 3),
('读书', 'reading', '阅读、笔记与摘录', '#22d3ee', 4);

-- 标签
INSERT INTO tag (name, slug, description, color) VALUES
('Java', 'java', NULL, '#f89820'),
('Spring Boot', 'spring-boot', NULL, '#6db33f'),
('Vue', 'vue', NULL, '#42b883'),
('JavaScript', 'javascript', NULL, '#f0db4f'),
('MySQL', 'mysql', NULL, '#00758f'),
('Redis', 'redis', NULL, '#dc382d'),
('随笔', 'essay', NULL, '#d6a14b'),
('教程', 'tutorial', NULL, '#3b6b46'),
('思考', 'thoughts', NULL, '#2c4a6b');

-- 文章示例
INSERT INTO article (title, slug, summary, content, category_id, is_top, is_featured, status, publish_time, view_count) VALUES
('MyBlog · 个人博客系统上线了', 'hello-blog',
 '你好，欢迎路过。这是一封写给第一波访客的短信：关于这个博客会写什么，以及为什么开。',
 '# MyBlog · 上线

你好，欢迎路过。

这是这个小角落的第一篇文章，我想用它代替千篇一律的"Hello World"。

## 为什么开博客

工作这些年，写过的代码、读过的书、走过的路，大多留在了聊天记录、
浏览器收藏夹或者某个项目的注释里。它们存在，但很难被重新找到，
也很难被自己重新看见。

博客是给未来的自己写的东西。

## 这里会写什么

- **代码**：踩过的坑，以及从坑里爬出来的方法
- **设计**：在界面与交互里找到的小乐趣
- **生活**：山、水、书、偶尔的几句牢骚
- **思考**：还没想清楚，但值得记下来的事

## 一点愿景

> 草木蔓发，春山可望。

慢一点，让灵魂跟上脚步。

如果你也喜欢这种节奏，欢迎在 [留言板](/guestbook) 留下一句话。
',
 1, 1, 1, 1, NOW(), 128),
('Spring Boot 3 整合 JWT 鉴权实战', 'spring-boot-jwt',
 '从零开始实现一个生产可用的 JWT 鉴权流程，包括登录签发、过滤器鉴权、刷新策略。',
 '# JWT 鉴权实战\n\n## 流程\n\n1. 用户登录 → 验证密码 → 签发 token\n2. 前端把 token 放在 `Authorization: Bearer xxx`\n3. 后端 Filter 解析 token，注入用户上下文\n4. 受保护接口通过 SecurityContext 鉴权\n\n## 代码\n\n```java\npublic Claims parse(String token) {\n  return Jwts.parser()\n    .verifyWith(key())\n    .build()\n    .parseSignedClaims(token)\n    .getPayload();\n}\n```\n\n## 注意\n\n- secret 必须够长，至少 32 字节\n- 过期时间不要设太久\n- refresh token 与 access token 分离',
 1, 0, 1, 1, NOW(), 256),
('Vue 3 Composition API 入门笔记', 'vue3-composition-api',
 '用一组例子，从 Options API 过渡到 Composition API。',
 '# Composition API\n\n## 为什么切换\n\n- 更好的逻辑复用（composables）\n- 更灵活的代码组织\n- TypeScript 更友好\n\n## 一个例子\n\n```js\nimport { ref, computed } from ''vue''\nconst count = ref(0)\nconst double = computed(() => count.value * 2)\n```\n\n`ref` 用于基础类型，`reactive` 用于对象。',
 1, 0, 1, 1, NOW(), 89),
('在山间小住', 'mountain-stay',
 '一次周末的短途，记录山里的清晨、雾气和一杯热茶。',
 '# 在山间小住\n\n## 清晨\n\n五点多被鸟叫醒，窗外全是雾。\n\n## 茶\n\n主人泡了一壶本地野茶，回甘很好。\n\n## 夜\n\n星星特别亮，听见远处的溪水声。',
 2, 0, 0, 1, NOW(), 32),
('配色笔记：自然诗意', 'color-poetry',
 '记录一组被山水滋养的配色：木绿、雨声蓝、晚季金。',
 '# 自然诗意\n\n## 主色\n\n| 名字 | 色值 |\n|------|------|\n| 木绿 | #3b6b46 |\n| 雨声 | #2c4a6b |\n| 晚季 | #d6a14b |\n\n## 搭配\n\n- 木绿做主按钮\n- 雨声蓝做信息背景\n- 晚季金做点缀',
 3, 0, 0, 1, NOW(), 41),
('重读《瓦尔登湖》', 'walden-reread',
 '三年前读过的书，再翻一遍，看到了不一样的东西。',
 '# 重读《瓦尔登湖》\n\n## 简单\n\n> 简化，再简化。\n\n## 一些句子\n\n- 我步入丛林，因为我希望生活得有意义。\n- 时间决定你会在生命中遇见谁。\n',
 4, 0, 0, 1, NOW(), 27);

-- 文章标签关联
INSERT INTO article_tag (article_id, tag_id) VALUES
(1, 7), (1, 9),
(2, 1), (2, 2),
(3, 3), (3, 4), (3, 8),
(4, 7),
(5, 9),
(6, 9);

-- 示例评论（已通过）
INSERT INTO comment (article_id, parent_id, nickname, email, content, status, create_time) VALUES
(1, 0, '路过的小熊', 'comment1@example.com', '博客做得真好看！配色很舒服。', 1, NOW()),
(1, 1, '站长', 'your_email@example.com', '谢谢，欢迎常来～', 1, NOW()),
(2, 0, 'Java 学习者', 'comment2@example.com', 'JWT 这块讲得很清楚，期待后续的 refresh 策略更新。', 1, NOW()),
(3, 0, '前端新人', 'comment3@example.com', 'Composition API 真香，比 mixin 清晰多了。', 1, NOW());


-- 站点配置
-- ⚠️ email / github 字段为占位值，部署后请在后台「站点配置」中替换为真实值
INSERT INTO site_config (config_key, config_value, description) VALUES
('siteName', 'MyBlog', '站点名称'),
('motto', '草木蔓发，春山可望', '站点副标题'),
('description', '一个工程师与写作者的小角落，记录代码、设计、生活与思考。', '站点描述'),
('keywords', '个人博客,MyBlog,Spring Boot,Vue,代码,设计,生活', 'SEO 关键词'),
('beian', '', 'ICP 备案号'),
('comment_audit', '1', '评论是否需要审核 (0=不需, 1=需要)'),
('github', 'https://github.com/', 'GitHub 链接（占位，部署后请在后台替换）'),
('email', 'your_email@example.com', '联系邮箱（占位，部署后请在后台替换）'),
('authorName', '站长', '站长/博主展示名,默认随 admin 用户昵称同步'),
('captcha_enabled', '1', '留言/评论提交是否开启算术验证码 (0=关, 1=开)'),
('sensitive_words', '', '敏感词列表，逗号或空格分隔；命中后正文将被 ** 掩码');

-- 项目（与前端静态示例保持一致）
INSERT INTO project (name, description, tech_stack, icon, color, github_url, demo_url, cover_url, category_id, sort_order, status) VALUES
('MyBlog 博客', '本站源码，Spring Boot + Vue 3 全栈实践，支持 Markdown、评论、SEO。', 'Spring Boot,Vue 3,MySQL,Element Plus', 'ChatDotRound', '#38bdf8', '#', '/', NULL, 1, 1, 1),
('Markdown Notebook', '本地优先的笔记应用，支持双向链接、图表、快捷键。', 'Tauri,Rust,TypeScript', 'Sunny', '#fbbf24', '#', NULL, NULL, 2, 2, 1),
('Weather Card', '嵌入卡片式天气小组件，支持多城市、动态背景与极简动画。', 'Vue 3,Canvas,OpenWeather API', 'Calendar', '#0ea5e9', '#', '#', NULL, 3, 3, 1),
('Todo CLI', '极简命令行 TODO 工具，支持优先级、标签、归档。', 'Go,Cobra', 'Promotion', '#22d3ee', '#', NULL, NULL, 2, 4, 1);

-- 友情链接（已通过）
INSERT INTO friend_link (name, url, avatar, description, link_group, sort_order, status) VALUES
('Vue.js', 'https://vuejs.org', 'https://vuejs.org/images/logo.svg', '渐进式 JavaScript 框架', '网友', 1, 1),
('Element Plus', 'https://element-plus.org', 'https://element-plus.org/images/element-plus-logo.svg', '基于 Vue 3 的组件库', '网友', 2, 1),
('Spring Boot', 'https://spring.io/projects/spring-boot', 'https://spring.io/img/projects/spring-boot.svg', 'Java 微服务的事实标准', '网友', 3, 1),
('MyBatis-Plus', 'https://baomidou.com', 'https://baomidou.com/img/logo.svg', 'MyBatis 的增强工具', '网友', 4, 1),
('Vite', 'https://vitejs.dev', 'https://vitejs.dev/logo.svg', '下一代前端构建工具', '网友', 5, 1),
('Pinia', 'https://pinia.vuejs.org', 'https://pinia.vuejs.org/logo.svg', 'Vue 官方推荐的状态管理', '网友', 6, 1);

-- ---------------------------------------------------------------
-- 访问日志(后台"今日访问统计"用)
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS visit_log;
CREATE TABLE visit_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    ip          VARCHAR(45)  NOT NULL DEFAULT '',
    device_type VARCHAR(16)  NOT NULL DEFAULT '',
    os          VARCHAR(64)  NOT NULL DEFAULT '',
    browser     VARCHAR(64)  NOT NULL DEFAULT '',
    path        VARCHAR(255) NOT NULL DEFAULT '',
    user_agent  VARCHAR(512) NOT NULL DEFAULT '',
    province    VARCHAR(64)  NOT NULL DEFAULT '',
    visit_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_visit_time (visit_time),
    INDEX idx_visit_ip_time (ip, visit_time),
    INDEX idx_visit_province (province, visit_time)
) ENGINE=InnoDB COMMENT='公开端点访问日志';

-- ---------------------------------------------------------------
-- 在线工具箱
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS tool_daily_click;
DROP TABLE IF EXISTS tool_category;
DROP TABLE IF EXISTS tool;
CREATE TABLE tool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '工具名',
    slug VARCHAR(50) NOT NULL COMMENT '路由 slug 或外链标识',
    icon VARCHAR(255) NOT NULL DEFAULT 'Tools' COMMENT '图标图片 URL（可上传或填写链接），旧值可能为 Element Plus 图标名',
    category VARCHAR(30) NOT NULL COMMENT '分类键,对应 tool_category.code',
    description VARCHAR(200) NOT NULL DEFAULT '' COMMENT '简介(2 行)',
    url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '跳转地址:同页路径或外链 URL',
    type TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0=同页内嵌 1=外链',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0=下线 1=正常 2=维护中 3=预告',
    notified TINYINT NOT NULL DEFAULT 0 COMMENT '是否已推送订阅邮件(0=未 1=已)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '拖拽排序',
    view_count BIGINT NOT NULL DEFAULT 0,
    click_count BIGINT NOT NULL DEFAULT 0,
    announcement VARCHAR(200) NOT NULL DEFAULT '' COMMENT '公告横幅',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=未删除 1=已删除(@TableLogic,缺列会导致工具接口 500)',
    UNIQUE KEY uk_slug (slug),
    KEY idx_category (category),
    KEY idx_status (status),
    KEY idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线工具箱';

CREATE TABLE tool_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL COMMENT '分类键,对应 tool.category',
    name VARCHAR(50) NOT NULL COMMENT '显示名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序:越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=下线(前台工具页隐藏该分类)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code),
    KEY idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具分类';

CREATE TABLE tool_daily_click (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_id BIGINT NOT NULL,
    click_date DATE NOT NULL,
    click_count INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_tool_date (tool_id, click_date),
    KEY idx_date (click_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具每日点击统计';

CREATE TABLE operation_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    module     VARCHAR(30)  NOT NULL COMMENT '业务模块,如 工具分类',
    action     VARCHAR(20)  NOT NULL COMMENT '操作类型:create/update/delete/status',
    target     VARCHAR(120) NOT NULL DEFAULT '' COMMENT '操作对象,如分类名(code)',
    operator   VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '操作人(账号)',
    detail     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '变更摘要',
    ip         VARCHAR(45)  NOT NULL DEFAULT '' COMMENT '操作来源 IP',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_module (module),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作日志';

INSERT INTO tool_category (code, name, sort_order) VALUES
('develop',   '开发', 1),
('text',      '文本', 2),
('image',     '图片', 3),
('time',      '时间', 4),
('crypto',    '加密', 5),
('generator', '生成', 6),
('daily',     '计算', 7);

INSERT INTO tool (name, slug, icon, category, description, url, type, status, sort_order, announcement) VALUES
('JSON 格式化',      'json-format',    'Document',      'develop',   '格式化与压缩 JSON，支持结构校验和错误定位',       '/tools/json-format',    0, 1, 100, ''),
('时间戳加解密',     'time-cipher',    'Clock',         'crypto',    '秒级与毫秒级时间戳互转，支持自定义格式输出',     '/tools/time-cipher',    0, 1, 110, ''),
('图片 Base64 转换', 'img-base64',     'Picture',       'image',     '图片与 Base64 互转，支持拖拽上传和一键复制',      '/tools/img-base64',     0, 1, 120, ''),
('二维码生成器',     'qr-generator',   'ChatLineRound', 'generator', '将文本或链接生成二维码，可调尺寸与容错等级',     '/tools/qr-generator',   0, 1, 200, ''),
('URL 编解码',       'url-codec',      'Link',          'develop',   'URL 与查询参数的编码、解码双向转换',             '/tools/url-codec',      0, 1, 210, ''),
('单位换算',         'unit-convert',   'Cpu',           'daily',     '长度、时间、重量、体积等常用单位换算',           '/tools/unit-convert',   0, 1, 220, ''),
('时间戳转换',       'timestamp',      'Timer',         'time',      '时间戳与可读日期时间双向转换',                   '/tools/timestamp',      0, 1, 300, ''),
('正则表达式测试',   'regex-tester',   'MagicStick',    'develop',   '实时测试正则匹配，高亮显示全部匹配项',           '/tools/regex-tester',   0, 1, 310, ''),
('Markdown 预览',    'md-preview',     'EditPen',       'text',      '左侧编写 Markdown，右侧实时预览渲染效果',        '/tools/md-preview',     0, 1, 400, ''),
('Base64 编解码',    'base64-codec',   'Lock',          'crypto',    '字符串与 Base64 双向转换，支持 UTF-8 与二进制',   '/tools/base64-codec',   0, 1, 410, ''),
('UUID 生成器',      'uuid-generator', 'Key',           'generator', '批量生成 UUID v4，支持自定义前缀与格式',         '/tools/uuid-generator', 0, 1, 500, ''),
('图片压缩',         'img-compress',   'PictureFilled', 'image',     '在线压缩 PNG / JPG / WebP，保持画质并缩小体积',   '/tools/img-compress',   0, 2, 600, '该工具正在维护中，预计 3 天后恢复');

-- 完
SELECT 'Database initialized.' AS message;
SELECT COUNT(*) AS users FROM user;
SELECT COUNT(*) AS categories FROM category;
SELECT COUNT(*) AS tags FROM tag;
SELECT COUNT(*) AS articles FROM article;
SELECT COUNT(*) AS comments FROM comment;
SELECT COUNT(*) AS configs FROM site_config;
SELECT COUNT(*) AS projects FROM project;
SELECT COUNT(*) AS friend_links FROM friend_link;
SELECT COUNT(*) AS tools FROM tool;
SELECT COUNT(*) AS tool_categories FROM tool_category;
SELECT COUNT(*) AS operation_logs FROM operation_log;
