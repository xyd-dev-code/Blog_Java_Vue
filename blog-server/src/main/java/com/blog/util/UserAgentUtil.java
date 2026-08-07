package com.blog.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 极简 User-Agent 解析器(无第三方依赖,自实现覆盖 95% 主流 UA)。
 *
 * <p>分类逻辑:</p>
 * <ul>
 *   <li>deviceType:Tablet(iPad/Android+Mobile) → Tablet;Mobile(iPhone/Android+Mobile) → Mobile;其他 → PC</li>
 *   <li>os:从 UA 关键词提取(Windows/macOS/iOS/Android/Linux/Other)</li>
 *   <li>browser:从 UA 关键词提取(Edge/Chrome/Safari/Firefox/QQ/WeChat/Other)</li>
 * </ul>
 *
 * <p>不追求 100% 准确(付费 UA 库如 device-detector-java 太重,本系统做趋势分析够用)。</p>
 */
public final class UserAgentUtil {

    private UserAgentUtil() {}

    public static class Info {
        public final String deviceType;   // PC / Mobile / Tablet
        public final String os;           // Windows / macOS / iOS / Android / Linux / Other
        public final String browser;      // Edge / Chrome / Safari / Firefox / QQ / WeChat / Other

        public Info(String deviceType, String os, String browser) {
            this.deviceType = deviceType;
            this.os = os;
            this.browser = browser;
        }
    }

    public static Info parse(String ua) {
        if (ua == null || ua.isBlank()) return new Info("Other", "Other", "Other");
        String s = ua.toLowerCase(Locale.ROOT);

        // deviceType
        String deviceType;
        if (s.contains("ipad")) {
            deviceType = "Tablet";
        } else if (s.contains("android")) {
            // Android + "Mobile" → 手机;否则平板/TV
            deviceType = s.contains("mobile") ? "Mobile" : "Tablet";
        } else if (s.contains("iphone") || (s.contains("mobile") && (s.contains("safari") || s.contains("applewebkit")))) {
            deviceType = "Mobile";
        } else {
            deviceType = "PC";
        }

        // os
        String os;
        if (s.contains("windows")) os = "Windows";
        else if (s.contains("mac os x") || s.contains("macintosh")) os = "macOS";
        else if (s.contains("iphone") || s.contains("ipad") || s.contains("ios")) os = "iOS";
        else if (s.contains("android")) os = "Android";
        else if (s.contains("linux")) os = "Linux";
        else os = "Other";

        // browser(顺序很关键:Edge 必须在 Chrome 之前,因为新版 Edge 也是 Chrome 内核)
        String browser;
        if (s.contains("edg/") || s.contains("edge/")) browser = "Edge";
        else if (s.contains("qqbrowser")) browser = "QQ";
        else if (s.contains("micromessenger")) browser = "WeChat";
        else if (s.contains("ucbrowser")) browser = "UC";
        else if (s.contains("firefox/") || s.contains("fxios")) browser = "Firefox";
        else if (s.contains("chrome/") || s.contains("crios/")) browser = "Chrome";
        else if (s.contains("safari/") && !s.contains("chrome")) browser = "Safari";
        else browser = "Other";

        return new Info(deviceType, os, browser);
    }

    /**
     * 截断过长的 UA,避免数据库字段溢出(>512 截掉尾部)
     */
    public static String truncate(String ua, int max) {
        if (ua == null) return "";
        return ua.length() > max ? ua.substring(0, max) : ua;
    }

    /**
     * 是否疑似爬虫/机器人(简单关键词)
     */
    private static final Pattern BOT = Pattern.compile(
            "(bot|spider|crawler|slurp|httpclient|java/|python-requests|scrapy|curl|wget|ahrefsbot|bingpreview)",
            Pattern.CASE_INSENSITIVE);

    public static boolean isBot(String ua) {
        if (ua == null || ua.isBlank()) return false;
        Matcher m = BOT.matcher(ua);
        return m.find();
    }
}