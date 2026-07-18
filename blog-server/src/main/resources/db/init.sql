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
    comment_count  INT          NOT NULL DEFAULT 0,
    like_count     INT          NOT NULL DEFAULT 0,
    is_top         TINYINT      NOT NULL DEFAULT 0,
    is_featured    TINYINT      NOT NULL DEFAULT 0,
    allow_comment  TINYINT      NOT NULL DEFAULT 1,
    password       VARCHAR(100) DEFAULT NULL,
    status         TINYINT      NOT NULL DEFAULT 1,
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
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_article (article_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='评论';

-- ---------------------------------------------------------------
-- 独立页面 (关于、留言板模板等)
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS page;
CREATE TABLE page (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    content     LONGTEXT,
    cover       VARCHAR(255),
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='独立页面';

-- ---------------------------------------------------------------
-- 项目（作品集）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS project;
CREATE TABLE project (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    kind        VARCHAR(20)  NOT NULL DEFAULT '工具',
    description VARCHAR(500),
    tech_stack  VARCHAR(500),
    icon        VARCHAR(50)  DEFAULT 'Folder',
    color       VARCHAR(50)  DEFAULT '#38bdf8',
    github_url  VARCHAR(255),
    demo_url    VARCHAR(255),
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB COMMENT='项目';

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
    status      TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_sort (sort_order)
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

-- ===============================================================
--                       Seed Data
-- ===============================================================

-- 默认管理员账号 admin
-- ⚠️ 这里给的是**无效占位 BCrypt hash**,任何密码都登不上。首次登录前请重置:
--   1) 用 backend BCryptPasswordEncoder.encode("你的新密码") 生成 hash
--   2) UPDATE user SET password='<新 hash>' WHERE username='admin';
--   3) 或登录后台"个人中心"直接修改密码
INSERT INTO user (username, password, nickname, email, role, status) VALUES
('admin', '$2a$10$REPLACE_WITH_YOUR_OWN_BCRYPT_HASH_FOR_NEW_PASSWORD', '站长', 'your_email@example.com', 'ADMIN', 1);

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
('DemoAuthor · 个人博客系统上线了', 'hello-blog',
 '你好，欢迎路过。这是一封写给第一波访客的短信：关于这个博客会写什么，以及为什么开。',
 '# DemoAuthor · 上线

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

-- 独立页面
INSERT INTO page (title, slug, content, sort_order, status) VALUES
('关于我', 'about',
 '# 你好，我是站长

一个普通的工程师与写作者。
白天写代码，晚上偶尔写点别的。

## 在这里

- 记录日常开发里值得记下来的事
- 整理阅读时划线过的句子
- 偶尔发一些摄影与生活片段

## 怎么联系我

- Email：见页脚
- GitHub：见页脚

如果只是想聊聊天，欢迎在 [留言板](/guestbook) 留下几句话。
',
 1, 1),
('留言板模板', 'guestbook', '欢迎在此留言，我会一封一封看完。', 2, 1);

-- 站点配置
-- ⚠️ email / github 字段为占位值，部署后请在后台「站点配置」中替换为真实值
INSERT INTO site_config (config_key, config_value, description) VALUES
('site_name', 'DemoAuthor', '站点名称'),
('motto', '草木蔓发，春山可望', '站点副标题'),
('description', '一个工程师与写作者的小角落，记录代码、设计、生活与思考。', '站点描述'),
('keywords', '个人博客,DemoAuthor,Spring Boot,Vue,代码,设计,生活', 'SEO 关键词'),
('beian', '', 'ICP 备案号'),
('comment_audit', '1', '评论是否需要审核 (0=不需, 1=需要)'),
('github', 'https://github.com/', 'GitHub 链接（占位，部署后请在后台替换）'),
('email', 'your_email@example.com', '联系邮箱（占位，部署后请在后台替换）'),
('authorName', '站长', '站长/博主展示名,默认随 admin 用户昵称同步');

-- 项目（与前端静态示例保持一致）
INSERT INTO project (name, kind, description, tech_stack, icon, color, github_url, demo_url, sort_order, status) VALUES
('DemoAuthor 博客', '开源', '本站源码，Spring Boot + Vue 3 全栈实践，支持 Markdown、评论、SEO。', 'Spring Boot,Vue 3,MySQL,Element Plus', 'ChatDotRound', '#38bdf8', '#', '/', 1, 1),
('Markdown Notebook', '工具', '本地优先的笔记应用，支持双向链接、图表、快捷键。', 'Tauri,Rust,TypeScript', 'Sunny', '#fbbf24', '#', NULL, 2, 1),
('Weather Card', '实验', '嵌入卡片式天气小组件，支持多城市、动态背景与极简动画。', 'Vue 3,Canvas,OpenWeather API', 'Calendar', '#0ea5e9', '#', '#', 3, 1),
('Todo CLI', '工具', '极简命令行 TODO 工具，支持优先级、标签、归档。', 'Go,Cobra', 'Promotion', '#22d3ee', '#', NULL, 4, 1);

-- 友情链接（已通过）
INSERT INTO friend_link (name, url, avatar, description, link_group, sort_order, status) VALUES
('Vue.js', 'https://vuejs.org', 'https://vuejs.org/images/logo.svg', '渐进式 JavaScript 框架', '网友', 1, 1),
('Element Plus', 'https://element-plus.org', 'https://element-plus.org/images/element-plus-logo.svg', '基于 Vue 3 的组件库', '网友', 2, 1),
('Spring Boot', 'https://spring.io/projects/spring-boot', 'https://spring.io/img/projects/spring-boot.svg', 'Java 微服务的事实标准', '网友', 3, 1),
('MyBatis-Plus', 'https://baomidou.com', 'https://baomidou.com/img/logo.svg', 'MyBatis 的增强工具', '网友', 4, 1),
('Vite', 'https://vitejs.dev', 'https://vitejs.dev/logo.svg', '下一代前端构建工具', '网友', 5, 1),
('Pinia', 'https://pinia.vuejs.org', 'https://pinia.vuejs.org/logo.svg', 'Vue 官方推荐的状态管理', '网友', 6, 1);

-- 完
SELECT 'Database initialized.' AS message;
SELECT COUNT(*) AS users FROM user;
SELECT COUNT(*) AS categories FROM category;
SELECT COUNT(*) AS tags FROM tag;
SELECT COUNT(*) AS articles FROM article;
SELECT COUNT(*) AS comments FROM comment;
SELECT COUNT(*) AS pages FROM page;
SELECT COUNT(*) AS configs FROM site_config;
SELECT COUNT(*) AS projects FROM project;
SELECT COUNT(*) AS friend_links FROM friend_link;