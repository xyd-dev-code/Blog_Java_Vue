package com.blog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "blog")
public class BlogProperties {
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private LocalStorage localStorage = new LocalStorage();
    private VisitLog visitLog = new VisitLog();
    private Ip2Region ip2Region = new Ip2Region();
    private Site site = new Site();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public LocalStorage getLocalStorage() { return localStorage; }
    public void setLocalStorage(LocalStorage localStorage) { this.localStorage = localStorage; }
    public VisitLog getVisitLog() { return visitLog; }
    public void setVisitLog(VisitLog visitLog) { this.visitLog = visitLog; }
    public Ip2Region getIp2Region() { return ip2Region; }
    public void setIp2Region(Ip2Region ip2Region) { this.ip2Region = ip2Region; }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }

    /**
     * 站点对外可访问的根 URL(用于邮件/通知里的审核/详情链接)。
     * 默认 https://yourdomain.com 占位符,部署时通过 application.yml 的
     * blog.site.url 覆盖。dev 环境通常填 http://localhost:5173。
     */
    public static class Site {
        private String url = "https://yourdomain.com";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Jwt {
        private String secret = "";
        private long expiration = 7 * 24 * 3600 * 1000L;
        private String header = "Authorization";
        private String prefix = "Bearer ";

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) { this.expiration = expiration; }
        public String getHeader() { return header; }
        public void setHeader(String header) { this.header = header; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class LocalStorage {
        private String dir = "";
        private String baseUrl = "";

        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class VisitLog {
        /**
         * 是否把本地/回环 IP(127.0.0.1 / ::1)也计入访问统计。
         * 默认 false(生产行为):避免 admin 本地操作、健康检查刷爆数据。
         * 开发环境下,前端 API 经 Vite 代理转发到 localhost:8080,
         * 后端 getRemoteAddr() 拿到的是 127.0.0.1,会被跳过,导致开发时统计恒为空。
         * 因此 dev 配置里设为 true,方便在 5173 直接看到数据流动;
         * 注意:开启后开发期间所有访问都记成 127.0.0.1,UV/真实 IP 会失真,生产务必关闭。
         */
        private boolean includeLocalIp = false;

        /**
         * 会话级去重窗口(秒)。同一 IP + UA 在此窗口内无论访问多少个接口,
         * 只记一次,防止恶意/无意识刷新刷爆数据库。
         * 默认 30 秒;若站点内容更新极快或希望更严格,可设为 60 或 300。
         */
        private int sessionIntervalSeconds = 30;

        public boolean isIncludeLocalIp() { return includeLocalIp; }
        public void setIncludeLocalIp(boolean includeLocalIp) { this.includeLocalIp = includeLocalIp; }
        public int getSessionIntervalSeconds() { return sessionIntervalSeconds; }
        public void setSessionIntervalSeconds(int sessionIntervalSeconds) { this.sessionIntervalSeconds = sessionIntervalSeconds; }
    }

    public static class Ip2Region {
        /**
         * ip2region.xdb 数据文件路径。
         * 默认 classpath:ip2region.xdb(放在 src/main/resources 下会打包进 jar)。
         * 也可以配绝对路径,如 /opt/blog/data/ip2region.xdb。
         * 文件不存在时,省份解析会返回空字符串,不影响其它功能。
         */
        private String dbPath = "classpath:ip2region.xdb";

        public String getDbPath() { return dbPath; }
        public void setDbPath(String dbPath) { this.dbPath = dbPath; }
    }
}