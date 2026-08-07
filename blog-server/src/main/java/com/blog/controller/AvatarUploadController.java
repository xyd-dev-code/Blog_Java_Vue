package com.blog.controller;

import com.blog.common.BizException;
import com.blog.common.R;
import com.blog.security.ClientIpResolver;
import com.blog.service.LocalStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ClientIpResolver ipResolver;

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

    public AvatarUploadController(LocalStorageService storage, ClientIpResolver ipResolver) {
        this.storage = storage;
        this.ipResolver = ipResolver;
    }

    @PostMapping
    @Operation(summary = "上传访客头像（公开）")
    public R<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest req) {
        String ip = ipResolver.resolve(req);
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
        // 拒绝 SVG / html 等伪图片(扩展名白名单对内容无效)
        if (ext.endsWith("svg") || ext.endsWith("htm") || ext.endsWith("xml")) {
            throw new BizException("不支持的图片格式");
        }

        try {
            byte[] bytes = file.getBytes();
            String extLower = ext.substring(1);
            // 1) magic-bytes 必须匹配扩展名(防止 .jpg 实际是 html/svg)
            if (!matchesMagic(bytes, extLower)) {
                throw new BizException("文件内容与扩展名不匹配");
            }
            // 2) 真实格式 = magic-bytes 探测的结果(不信任文件名,因为文件名常被改后缀)
            String realExt = detectFormat(bytes);
            if (realExt == null) {
                throw new BizException("无法识别的图片格式");
            }
            // 3) 真实格式必须在白名单(防御深度)
            if (!"png".equals(realExt) && !"jpg".equals(realExt) && !"webp".equals(realExt)) {
                throw new BizException("不支持的图片格式: " + realExt);
            }
            // 4) GIF 走原样保存(保留动画),但先用 ImageIO 验证可解析
            if ("gif".equals(realExt)) {
                if (ImageIO.read(new java.io.ByteArrayInputStream(bytes)) == null) {
                    throw new BizException("无法识别的图片内容");
                }
                String url = storage.uploadBytes(bytes, ".gif");
                return R.ok(Map.of("url", url));
            }
            // 5) 非 GIF:压缩后输出,扩展名按真实格式(PNG 保留 .png 保透明)
            bytes = compressAvatar(bytes, realExt);
            String outExt = "png".equals(realExt) ? ".png" : ".jpg";
            String url = storage.uploadBytes(bytes, outExt);
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

    private static boolean matchesMagic(byte[] bytes, String extLower) {
        if (bytes == null || bytes.length < 12) return false;
        return switch (extLower) {
            case "jpg", "jpeg" ->
                bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
            case "png" ->
                bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
            case "gif" ->
                bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8';
            case "webp" ->
                bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    /**
     * 不信任文件名,直接根据字节内容判断真实格式。
     * 用户常把 .jpg 改成 .png 上传(壁纸网站下载的图经常命名错乱),
     * 这里检测出真实类型,以真实类型为准。
     * 返回小写扩展名(png/jpg/gif/webp),无法识别返回 null。
     */
    private static String detectFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return "jpg";
        if (bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') return "png";
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') return "gif";
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "webp";
        return null;
    }

    /**
     * 压缩头像:先 ImageIO.read 验证可解析,失败抛 BizException 而非原样回退。
     * 按输入格式选 writer:
     *   - PNG  → PNG 写出(保 alpha/透明)
     *   - JPG/JPEG/WEBP → JPEG 写出(尺寸>512 才缩放,有损压缩减小体积)
     *   - 未知格式 → fallback JPEG
     */
    private static byte[] compressAvatar(byte[] bytes, String extLower) throws Exception {
        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (img == null) {
            throw new BizException("无法解析的图片内容");
        }
        boolean isPng = "png".equals(extLower);
        boolean isJpeg = "jpg".equals(extLower) || "jpeg".equals(extLower);

        int w = img.getWidth(), h = img.getHeight();
        // 仅当超过 MAX_DIM 才缩放;PNG/JPG 都需要
        boolean needScale = w > MAX_DIM || h > MAX_DIM;
        int targetType = isPng ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        if (needScale) {
            double ratio = Math.min((double) MAX_DIM / w, (double) MAX_DIM / h);
            int nw = (int) (w * ratio), nh = (int) (h * ratio);
            BufferedImage scaled = new BufferedImage(nw, nh, targetType);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            img = scaled;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (isPng) {
            // PNG 无损写出,保留 alpha(透明头像不被填白)
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        }
        // JPEG/WEBP/未知 → 走 JPEG 有损压缩;写入前若无 TYPE_INT_RGB,转一次以避免 JPEG 不支持 alpha 报错
        BufferedImage rgb = img;
        if (isJpeg && img.getType() != BufferedImage.TYPE_INT_RGB && img.getRaster().getNumBands() >= 3) {
            BufferedImage converted = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = converted.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            rgb = converted;
        }
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);
        writer.setOutput(new MemoryCacheImageOutputStream(out));
        writer.write(null, new IIOImage(rgb, null, null), param);
        writer.dispose();
        return out.toByteArray();
    }
}