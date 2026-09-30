package com.blog.service;

import com.blog.config.BlogProperties;

/** Shared rendering and URL helpers for administrative notification emails. */
final class NotificationEmailSupport {
    private NotificationEmailSupport() {}

    static String siteUrl(BlogProperties properties) {
        String preferred = trimSlash(properties.getSiteBaseUrl());
        if (!preferred.isBlank()) return preferred;
        return trimSlash(properties.getSite() == null ? null : properties.getSite().getUrl());
    }

    static String link(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) return path;
        return trimSlash(baseUrl) + (path.startsWith("/") ? path : "/" + path);
    }

    static String adminHtml(String siteName,
                            String heading,
                            String intro,
                            String[][] details,
                            String actionLabel,
                            String actionUrl) {
        StringBuilder rows = new StringBuilder();
        for (String[] detail : details) {
            rows.append("<tr><th style=\"padding:9px 12px;text-align:left;vertical-align:top;"
                    + "color:#64748b;font-size:14px;font-weight:500;white-space:nowrap\">")
                    .append(html(detail[0]))
                    .append("</th><td style=\"padding:9px 12px;color:#0f172a;font-size:14px;"
                            + "line-height:1.7;word-break:break-word\">")
                    .append(multiline(detail[1]))
                    .append("</td></tr>");
        }

        return "<!doctype html><html><body style=\"margin:0;background:#f1f5f9;"
                + "font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC',"
                + "'Microsoft YaHei',sans-serif;color:#0f172a\">"
                + "<div style=\"max-width:620px;margin:0 auto;padding:28px 16px\">"
                + "<div style=\"background:#fff;border:1px solid #e2e8f0;border-radius:14px;"
                + "overflow:hidden;box-shadow:0 4px 18px rgba(15,23,42,.06)\">"
                + "<div style=\"padding:24px 26px 12px\"><div style=\"color:#16a34a;"
                + "font-size:13px;font-weight:700;letter-spacing:.04em\">"
                + html(siteName) + "</div><h1 style=\"margin:8px 0 6px;font-size:22px;"
                + "line-height:1.4\">" + html(heading) + "</h1><p style=\"margin:0;color:#475569;"
                + "font-size:14px;line-height:1.7\">" + html(intro) + "</p></div>"
                + "<div style=\"padding:10px 14px 8px\"><table role=\"presentation\" width=\"100%\""
                + " cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:collapse;"
                + "background:#f8fafc;border-radius:10px\">" + rows + "</table></div>"
                + "<div style=\"padding:18px 26px 26px\"><a href=\"" + attribute(actionUrl)
                + "\" style=\"display:inline-block;padding:11px 20px;border-radius:8px;"
                + "background:#16a34a;color:#fff;text-decoration:none;font-weight:700;"
                + "font-size:14px\">" + html(actionLabel) + "</a>"
                + "<p style=\"margin:16px 0 0;color:#94a3b8;font-size:12px;line-height:1.6\">"
                + "若按钮无法打开，请复制此地址到浏览器：<br><span style=\"word-break:break-all\">"
                + html(actionUrl) + "</span></p></div></div></div></body></html>";
    }

    private static String multiline(String value) {
        return html(value).replace("\n", "<br>");
    }

    private static String attribute(String value) {
        return html(value).replace("'", "&#39;");
    }

    private static String html(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String trimSlash(String value) {
        if (value == null) return "";
        return value.strip().replaceAll("/+$", "");
    }
}
