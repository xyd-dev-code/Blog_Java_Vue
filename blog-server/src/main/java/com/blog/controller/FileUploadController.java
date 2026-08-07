package com.blog.controller;

import com.blog.common.BizException;
import com.blog.common.R;
import com.blog.security.ClientIpResolver;
import com.blog.security.RateLimiter;
import com.blog.service.LocalStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
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
import java.util.Base64;
import java.util.Map;

@Tag(name = "后台 - 文件上传")
@RestController
@RequestMapping("/api/v1/admin/upload")
public class FileUploadController {
    private final LocalStorageService storage;
    private final RateLimiter rateLimiter;
    private final ClientIpResolver ipResolver;

    private static final long MAX_SIZE = 10 * 1024 * 1024;  // 原始文件最大 10MB
    private static final int MAX_DIM = 1920;                // 长边最大像素
    private static final float JPEG_QUALITY = 0.85f;        // JPEG 压缩质量

    /** 后台上传限频:30/min/IP。被劫持 admin 账号时防止资源耗尽 */
    private static final int UPLOAD_LIMIT = 30;
    private static final int UPLOAD_WINDOW_SEC = 60;

    public FileUploadController(LocalStorageService storage,
                                RateLimiter rateLimiter,
                                ClientIpResolver ipResolver) {
        this.storage = storage;
        this.rateLimiter = rateLimiter;
        this.ipResolver = ipResolver;
    }

    @PostMapping
    @Operation(summary = "上传文件到本地图床（自动去重）")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        checkUploadRate(req);
        if (file.isEmpty()) throw new BizException("文件为空");

        String original = file.getOriginalFilename();
        if (original == null) throw new BizException("文件名无效");

        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot).toLowerCase();

        if (!ext.matches("\\.(png|jpg|jpeg|gif|webp)")) {
            throw new BizException("仅支持图片格式 (png/jpg/gif/webp)");
        }

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_SIZE) throw new BizException("文件不能超过 10 MB");
            String extLower = ext.substring(1);
            // magic-bytes 校验:避免扩展名伪装(比如 .jpg 实际是 svg/script)
            if (!matchesMagic(bytes, extLower)) {
                throw new BizException("文件类型与扩展名不匹配");
            }

            // 对 jpg/png 自动压缩，gif/webp 原样保留
            if ("jpg".equals(extLower) || "jpeg".equals(extLower) || "png".equals(extLower)) {
                bytes = compressImage(bytes, extLower);
                if ("jpg".equals(extLower) || "jpeg".equals(extLower)) {
                    ext = ".jpg";
                }
            }

            String url = storage.uploadBytes(bytes, ext);
            return R.ok(Map.of("url", url));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("上传失败");
        }
    }

    @DeleteMapping
    @Operation(summary = "删除本地图床上的文件")
    public R<Void> delete(@RequestParam("url") String url, HttpServletRequest req) {
        checkUploadRate(req);
        if (!StringUtils.hasText(url)) return R.ok();
        storage.deleteByUrl(url);
        return R.ok();
    }

    /**
     * 从 Base64 data URL 上传（用于前端粘贴图片场景）
     * 请求体 JSON: { "base64": "data:image/png;base64,iVBOR..." }
     */
    @PostMapping("/base64")
    @Operation(summary = "Base64 上传到七牛云（自动去重）")
    public R<Map<String, String>> uploadBase64(@RequestBody Map<String, String> body, HttpServletRequest req) {
        checkUploadRate(req);
        String b64 = body.get("base64");
        if (!StringUtils.hasText(b64)) throw new BizException("缺少 base64 数据");

        // 解析 data:image/png;base64,... 格式
        String mime = "image/png";
        String data = b64;
        if (b64.startsWith("data:")) {
            int comma = b64.indexOf(',');
            if (comma < 0) throw new BizException("无效的 base64 格式");
            String header = b64.substring(0, comma);
            int colon = header.indexOf(':');
            int semi = header.indexOf(';');
            if (colon >= 0 && semi > colon) mime = header.substring(colon + 1, semi);
            data = b64.substring(comma + 1);
        }

        String ext = switch (mime.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            default -> ".png";
        };

        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            if (bytes.length > MAX_SIZE) throw new BizException("文件不能超过 10 MB");
            if (!matchesMagic(bytes, ext.substring(1))) {
                throw new BizException("文件类型与扩展名不匹配");
            }

            // Base64 上传也统一压缩（与普通上传逻辑一致）
            String extLower = ext.substring(1);
            if ("jpg".equals(extLower) || "jpeg".equals(extLower) || "png".equals(extLower)) {
                bytes = compressImage(bytes, extLower);
                if ("jpg".equals(extLower) || "jpeg".equals(extLower)) {
                    ext = ".jpg";
                }
            }

            String url = storage.uploadBytes(bytes, ext);
            return R.ok(Map.of("url", url));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("上传失败");
        }
    }

    /** 后台上传限频:失败抛 BizException(被 GlobalExceptionHandler 转 429)。 */
    private void checkUploadRate(HttpServletRequest req) {
        String ip = ipResolver.resolve(req);
        rateLimiter.acquireOrThrow("upload:admin:ip:" + ip, UPLOAD_LIMIT, UPLOAD_WINDOW_SEC);
    }

    // 注：MD5 去重、日期目录、上传前查重等逻辑已下沉到 LocalStorageService。

    /**
     * magic-bytes 校验:防扩展名伪装。常见图片格式最小可识别字节数 12 字节,
     * 传入 bytes 长度 < 12 直接视为非法(短到不可能是合法图片)。
     */
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
     * 压缩图片：缩放到 MAX_DIM 以内，JPEG 质量压缩
     * @param bytes  原始图片字节
     * @param format "jpg" 或 "png"
     * @return 压缩后的字节
     */
    private static byte[] compressImage(byte[] bytes, String format) throws Exception {
        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (img == null) return bytes; // 无法解析，原样返回

        int w = img.getWidth();
        int h = img.getHeight();

        // 缩放
        if (w > MAX_DIM || h > MAX_DIM) {
            double ratio = Math.min((double) MAX_DIM / w, (double) MAX_DIM / h);
            int nw = (int) (w * ratio);
            int nh = (int) (h * ratio);
            BufferedImage scaled = new BufferedImage(nw, nh, img.getType() == BufferedImage.TYPE_CUSTOM
                    ? BufferedImage.TYPE_INT_RGB : img.getType());
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, nw, nh, null);
            g.dispose();
            img = scaled;
        }

        // 输出
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if ("jpg".equals(format) || "jpeg".equals(format)) {
            // JPEG 质量压缩
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.setOutput(new MemoryCacheImageOutputStream(out));
            writer.write(null, new IIOImage(img, null, null), param);
            writer.dispose();
        } else {
            // PNG 保持原格式
            ImageIO.write(img, "png", out);
        }
        return out.toByteArray();
    }
}
