package com.blog.controller;

import com.blog.common.BizException;
import com.blog.common.R;
import com.blog.service.LocalStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 访客头像上传 — 公开接口，限频 + 压缩 + 文件类型白名单
 */
@Tag(name = "访客 - 头像上传")
@RestController
@RequestMapping("/api/v1/upload/avatar")
public class AvatarUploadController {
    private final LocalStorageService storage;

    private static final long MAX_SIZE = 2 * 1024 * 1024;
    private static final int MAX_DIM = 512;
    private static final float JPEG_QUALITY = 0.85f;

    private static final int RATE_LIMIT = 20;
    private static final long RATE_WINDOW_MS = 60_000L;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    private static class Counter {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();
    }

    public AvatarUploadController(LocalStorageService storage) {
        this.storage = storage;
    }

    @PostMapping
    @Operation(summary = "上传访客头像（公开）")
    public R<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xff) {
        String ip = xff != null ? xff.split(",")[0].trim() : "unknown";
        if (!checkRate(ip)) throw new BizException("上传过于频繁，请稍后再试");

        if (file.isEmpty()) throw new BizException("文件为空");
        if (file.getSize() > MAX_SIZE) throw new BizException("头像不能超过 2MB");

        String original = file.getOriginalFilename();
        if (original == null) throw new BizException("文件名无效");
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot).toLowerCase();
        if (!ext.matches("\\.(png|jpg|jpeg|webp|gif)")) {
            throw new BizException("仅支持 png/jpg/webp/gif 格式");
        }

        try {
            byte[] bytes = file.getBytes();
            String extLower = ext.substring(1);
            if ("gif".equals(extLower)) {
                String url = storage.uploadBytes(bytes, ".gif");
                return R.ok(Map.of("url", url));
            }
            bytes = compressAvatar(bytes);
            String url = storage.uploadBytes(bytes, ".jpg");
            return R.ok(Map.of("url", url));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("上传失败: " + e.getMessage());
        }
    }

    private synchronized boolean checkRate(String ip) {
        long now = System.currentTimeMillis();
        Counter c = counters.compute(ip, (k, v) -> {
            if (v == null || now - v.windowStart > RATE_WINDOW_MS) return new Counter();
            return v;
        });
        return c.count.incrementAndGet() <= RATE_LIMIT;
    }

    private static byte[] compressAvatar(byte[] bytes) throws Exception {
        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (img == null) return bytes;
        int w = img.getWidth(), h = img.getHeight();
        if (w > MAX_DIM || h > MAX_DIM) {
            double ratio = Math.min((double) MAX_DIM / w, (double) MAX_DIM / h);
            int nw = (int) (w * ratio), nh = (int) (h * ratio);
            BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            img = scaled;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        writer.setOutput(new MemoryCacheImageOutputStream(out));
        writer.write(null, new IIOImage(img, null, null), param);
        writer.dispose();
        return out.toByteArray();
    }
}
