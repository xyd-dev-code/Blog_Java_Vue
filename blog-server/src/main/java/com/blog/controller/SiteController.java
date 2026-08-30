package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.service.LocalStorageService;
import com.blog.service.SiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Pattern;

@Tag(name = "前台 - 站点配置")
@RestController
@RequestMapping("/api/v1/site")
public class SiteController {

    private static final Pattern PRIVATE_IP = Pattern.compile(
            "^(127\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.|0\\.|169\\.254\\.|::1|fc|fd|fe80)"
    );
    private static final Safelist PLAINTEXT = Safelist.none();
    private static final int FAVICON_SIZE = 64;
    private static final long MAX_FAVICON_SOURCE_BYTES = 5L * 1024 * 1024;

    private final SiteConfigService siteConfigService;
    private final UserMapper userMapper;
    private final LocalStorageService storage;

    public SiteController(SiteConfigService siteConfigService,
                          UserMapper userMapper,
                          LocalStorageService storage) {
        this.siteConfigService = siteConfigService;
        this.userMapper = userMapper;
        this.storage = storage;
    }

    @GetMapping
    @Operation(summary = "读取公开站点配置")
    public R<Map<String, String>> all() {
        Map<String, String> data = siteConfigService.publicAsMap();
        // authorName 兜底:如果站点配置里没有或仍是默认 "站长",直接用 admin 的最新昵称
        String fromConfig = data.get("authorName");
        User admin = userMapper.selectById(1L);
        String adminNickname = admin == null ? null : admin.getNickname();
        boolean isStale = fromConfig == null
                || fromConfig.isEmpty()
                || "站长".equals(fromConfig);
        if (adminNickname != null && !adminNickname.isEmpty() && isStale) {
            data.put("authorName", adminNickname);
        } else if (adminNickname != null && !adminNickname.isEmpty()
                && !fromConfig.equals(adminNickname)) {
            // 跟随最新的 admin 昵称,无需手动同步
            data.put("authorName", adminNickname);
        }
        // 把 admin 的最新昵称单独暴露出来,前端 About 页直接读 userNickname
        if (admin != null && admin.getNickname() != null && !admin.getNickname().isEmpty()) {
            data.put("userNickname", admin.getNickname());
        }
        // greeting：你好，我是 {昵称}，若 site_config 有自定义 greeting 则用自定义值
        if (!data.containsKey("greeting") || data.get("greeting") == null || data.get("greeting").isBlank()) {
            String name = data.getOrDefault("userNickname", "站长");
            data.put("greeting", "你好，我是" + name);
        }
        // 防存储 XSS:纯文本字段去掉 HTML 标签(前端 About 页用 {{ }} 渲染,不走 v-html)
        sanitizeTextValues(data);
        return R.ok(data);
    }

    /**
     * favicon：优先读取本地图床头像并输出带透明四角的圆形 PNG。
     * 外部旧头像无法在本地派生时才回退到安全的公网 URL 重定向。
     */
    @GetMapping("/favicon")
    @Operation(summary = "站点头像（用于浏览器 tab 图标）")
    public ResponseEntity<?> favicon() {
        String url = null;
        // 1) admin 头像优先
        User admin = userMapper.selectById(1L);
        if (admin != null && admin.getAvatar() != null && !admin.getAvatar().isBlank()) {
            url = admin.getAvatar();
        }
        // 2) site_config.siteLogo 兜底
        if (url == null) {
            String logo = siteConfigService.get("siteLogo", null);
            if (logo != null && !logo.isBlank()) url = logo;
        }
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] source = storage.readByUrl(url, MAX_FAVICON_SOURCE_BYTES);
            byte[] circular = toCircularPng(source);
            if (circular != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_PNG);
                headers.setCacheControl("no-store, no-cache, must-revalidate");
                return new ResponseEntity<>(circular, headers, org.springframework.http.HttpStatus.OK);
            }
        } catch (IOException ignored) {
            // 派生失败时继续走经过校验的外部 URL 兜底，不输出文件路径或头像 URL。
        }
        // 安全校验:只允许 http/https 公网 URL
        if (!isSafeRedirectUrl(url)) {
            return ResponseEntity.badRequest().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        return new ResponseEntity<>(headers, org.springframework.http.HttpStatus.FOUND);
    }

    /** 将任意横竖比图片居中裁成 64×64 圆形透明 PNG。 */
    static byte[] toCircularPng(byte[] source) throws IOException {
        if (source == null || source.length == 0) return null;
        BufferedImage input = ImageIO.read(new ByteArrayInputStream(source));
        if (input == null || input.getWidth() <= 0 || input.getHeight() <= 0) return null;

        int cropSize = Math.min(input.getWidth(), input.getHeight());
        int sourceX = (input.getWidth() - cropSize) / 2;
        int sourceY = (input.getHeight() - cropSize) / 2;
        BufferedImage output = new BufferedImage(FAVICON_SIZE, FAVICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setClip(new Ellipse2D.Double(0, 0, FAVICON_SIZE, FAVICON_SIZE));
            graphics.drawImage(input,
                    0, 0, FAVICON_SIZE, FAVICON_SIZE,
                    sourceX, sourceY, sourceX + cropSize, sourceY + cropSize,
                    null);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(output, "png", bytes);
        return bytes.toByteArray();
    }

    private static boolean isSafeRedirectUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;
            // 拒绝私网/链路本地/回环 IP
            if (PRIVATE_IP.matcher(host).find()) return false;
            // 解析域名后也检查 IP
            try {
                InetAddress addr = InetAddress.getByName(host);
                String ip = addr.getHostAddress();
                if (PRIVATE_IP.matcher(ip).find()) return false;
            } catch (UnknownHostException ignored) {
                // DNS 解析失败时拒绝(安全第一)
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 对纯文本 config 值做 HTML 转义,防存储 XSS。
     * URL 类字段(url/logo/favicon/image)保留原样,因为它们在 <img src> 里,
     * 不在 innerHTML 里。
     */
    private static void sanitizeTextValues(Map<String, String> data) {
        for (Map.Entry<String, String> e : data.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            if (val == null || val.isBlank()) continue;
            // URL 类字段保留原样
            if (isUrlField(key)) continue;
            // 纯文本字段:strip HTML tags
            e.setValue(Jsoup.clean(val, "", PLAINTEXT,
                    new org.jsoup.nodes.Document.OutputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)));
        }
    }

    private static boolean isUrlField(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return lower.endsWith("url") || lower.endsWith("logo")
                || lower.endsWith("favicon") || lower.endsWith("image")
                || lower.endsWith("avatar");
    }
}
