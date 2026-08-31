-- Schema only. Existing databases baseline at version 1; no seeds or destructive DDL.

CREATE TABLE IF NOT EXISTS user (
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

CREATE TABLE IF NOT EXISTS category (
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

CREATE TABLE IF NOT EXISTS tag (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    description VARCHAR(255),
    color       VARCHAR(20),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='文章标签';

CREATE TABLE IF NOT EXISTS article (
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
    INDEX article_idx_status_time (status, publish_time),
    INDEX article_idx_category (category_id),
    INDEX article_idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='文章';

CREATE TABLE IF NOT EXISTS article_tag (
    article_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    INDEX article_tag_idx_tag (tag_id)
) ENGINE=InnoDB COMMENT='文章-标签关联';

CREATE TABLE IF NOT EXISTS comment (
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
    INDEX comment_idx_article (article_id),
    INDEX comment_idx_status (status),
    INDEX comment_idx_featured (featured)
) ENGINE=InnoDB COMMENT='评论';

CREATE TABLE IF NOT EXISTS comment_like (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id   BIGINT       NOT NULL,
    ip           VARCHAR(45)  NOT NULL DEFAULT '',
    user_id      BIGINT       NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY comment_like_uq_like_ip (comment_id, ip),
    UNIQUE KEY comment_like_uq_like_user (comment_id, user_id),
    INDEX comment_like_idx_comment (comment_id)
) ENGINE=InnoDB COMMENT='评论点赞记录';

CREATE TABLE IF NOT EXISTS comment_report (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    comment_id   BIGINT       NOT NULL,
    reason       VARCHAR(50)  NOT NULL,
    detail       VARCHAR(500) DEFAULT '',
    email        VARCHAR(100) DEFAULT '',
    ip           VARCHAR(45)  NOT NULL DEFAULT '',
    status       TINYINT      NOT NULL DEFAULT 0,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX comment_report_idx_comment (comment_id),
    INDEX comment_report_idx_status (status)
) ENGINE=InnoDB COMMENT='评论举报记录';

CREATE TABLE IF NOT EXISTS project (
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
    INDEX project_idx_status (status),
    INDEX project_idx_sort (sort_order),
    INDEX project_idx_category (category_id)
) ENGINE=InnoDB COMMENT='项目';

CREATE TABLE IF NOT EXISTS project_category (
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
    INDEX project_category_idx_status (status),
    INDEX project_category_idx_sort (sort_order)
) ENGINE=InnoDB COMMENT='项目分类';

CREATE TABLE IF NOT EXISTS friend_link (
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
    INDEX friend_link_idx_status (status),
    INDEX friend_link_idx_sort (sort_order),
    INDEX friend_link_idx_recommended (recommended)
) ENGINE=InnoDB COMMENT='友情链接';

CREATE TABLE IF NOT EXISTS site_config (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key  VARCHAR(80)  NOT NULL UNIQUE,
    config_value  TEXT,
    description VARCHAR(255),
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='站点配置';

CREATE TABLE IF NOT EXISTS share_log (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id   BIGINT       NOT NULL,
    channel      VARCHAR(16)  NOT NULL COMMENT 'wechat/weibo/qq/douban/copy/link',
    ip           VARCHAR(64),
    user_agent   VARCHAR(255),
    share_date   DATE         NOT NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX share_log_idx_article_day (article_id, share_date),
    INDEX share_log_idx_day_channel (share_date, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享点击日志(每日同 IP+UA+渠道去重)';

CREATE TABLE IF NOT EXISTS email_subscription (
    id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    email        VARCHAR(120) NOT NULL COMMENT '订阅邮箱(统一小写存储)',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待确认 1=已确认',
    token        VARCHAR(64)  DEFAULT NULL COMMENT '确认令牌(UUID 去横杠)',
    source       VARCHAR(20)  DEFAULT 'web' COMMENT '订阅来源',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirm_time DATETIME     DEFAULT NULL COMMENT '确认时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(@TableLogic)',
    UNIQUE KEY email_subscription_uk_email (email),
    KEY email_subscription_idx_status (status),
    KEY email_subscription_idx_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱订阅';

CREATE TABLE IF NOT EXISTS visit_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    ip          VARCHAR(45)  NOT NULL DEFAULT '',
    device_type VARCHAR(16)  NOT NULL DEFAULT '',
    os          VARCHAR(64)  NOT NULL DEFAULT '',
    browser     VARCHAR(64)  NOT NULL DEFAULT '',
    path        VARCHAR(255) NOT NULL DEFAULT '',
    user_agent  VARCHAR(512) NOT NULL DEFAULT '',
    province    VARCHAR(64)  NOT NULL DEFAULT '',
    visit_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX visit_log_idx_visit_time (visit_time),
    INDEX visit_log_idx_visit_ip_time (ip, visit_time),
    INDEX visit_log_idx_visit_province (province, visit_time)
) ENGINE=InnoDB COMMENT='公开端点访问日志';

CREATE TABLE IF NOT EXISTS tool (
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
    UNIQUE KEY tool_uk_slug (slug),
    KEY tool_idx_category (category),
    KEY tool_idx_status (status),
    KEY tool_idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线工具箱';

CREATE TABLE IF NOT EXISTS tool_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL COMMENT '分类键,对应 tool.category',
    name VARCHAR(50) NOT NULL COMMENT '显示名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序:越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=下线(前台工具页隐藏该分类)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY tool_category_uk_code (code),
    KEY tool_category_idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具分类';

CREATE TABLE IF NOT EXISTS tool_daily_click (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_id BIGINT NOT NULL,
    click_date DATE NOT NULL,
    click_count INT NOT NULL DEFAULT 0,
    UNIQUE KEY tool_daily_click_uk_tool_date (tool_id, click_date),
    KEY tool_daily_click_idx_date (click_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具每日点击统计';

CREATE TABLE IF NOT EXISTS operation_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    module     VARCHAR(30)  NOT NULL COMMENT '业务模块,如 工具分类',
    action     VARCHAR(20)  NOT NULL COMMENT '操作类型:create/update/delete/status',
    target     VARCHAR(120) NOT NULL DEFAULT '' COMMENT '操作对象,如分类名(code)',
    operator   VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '操作人(账号)',
    detail     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '变更摘要',
    ip         VARCHAR(45)  NOT NULL DEFAULT '' COMMENT '操作来源 IP',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY operation_log_idx_module (module),
    KEY operation_log_idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作日志';
