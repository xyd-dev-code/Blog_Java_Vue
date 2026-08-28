package com.blog.service;

import com.blog.config.BlogProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘图床 — 替代七牛云，把图片写到服务器本地磁盘，
 * Nginx 静态服务 /img/ 目录，由 Let's Encrypt 签发的证书提供 HTTPS。
 */
@Service
public class LocalStorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path baseDir;
    private final String publicUrlPrefix;

    public LocalStorageService(BlogProperties props) {
        BlogProperties.LocalStorage ls = props.getLocalStorage();
        if (ls == null || !StringUtils.hasText(ls.getDir()) || !StringUtils.hasText(ls.getBaseUrl())) {
            throw new IllegalStateException(
                "本地图床未配置（blog.local-storage.dir / blog.local-storage.base-url），请在 application.yml 中填写");
        }
        this.baseDir = Paths.get(ls.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建图床目录: " + baseDir, e);
        }
        String base = ls.getBaseUrl();
        if (!base.startsWith("http://") && !base.startsWith("https://")) base = "https://" + base;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        this.publicUrlPrefix = base;
        log.info("LocalStorageService 初始化完成");
    }

    /** 写入字节，自动按 MD5 去重。返回可访问 URL。 */
    public String uploadBytes(byte[] bytes, String ext) throws Exception {
        String hash = md5Hex(bytes);
        String dateDir = LocalDate.now().format(DATE_FMT);
        String name = dateDir + "/" + hash + ext;
        Path target = baseDir.resolve(name).normalize();
        // 防越权：必须在 baseDir 下
        if (!target.startsWith(baseDir)) {
            throw new SecurityException("非法路径: " + target);
        }
        if (!Files.exists(target)) {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }
        return publicUrlPrefix + "/" + name;
    }

    /** 从 URL 删除本地文件（容错，不存在也算成功）。 */
    public boolean deleteByUrl(String url) {
        if (!StringUtils.hasText(url)) return false;
        if (!url.startsWith(publicUrlPrefix + "/")) return false;
        String rel = url.substring(publicUrlPrefix.length() + 1);
        Path target = baseDir.resolve(rel).normalize();
        if (!target.startsWith(baseDir)) return false;
        try {
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("删除本地图片失败: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    private static String md5Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                int v = b & 0xff;
                sb.append(Character.forDigit(v >>> 4, 16));
                sb.append(Character.forDigit(v & 0x0f, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
