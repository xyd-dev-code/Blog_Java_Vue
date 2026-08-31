package com.blog.controller;
import com.blog.common.BizException;
import com.blog.common.R;
import com.blog.security.ClientIpResolver;
import com.blog.security.RateLimiter;
import com.blog.service.LocalStorageService;
import com.blog.service.SafeImageProcessor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.Map;

@Tag(name = "后台 - 文件上传")
@RestController
@RequestMapping("/api/v1/admin/upload")
public class FileUploadController {
    private static final int MAX_SIZE = 10 * 1024 * 1024;
    private final LocalStorageService storage;
    private final RateLimiter rateLimiter;
    private final ClientIpResolver ipResolver;
    public FileUploadController(LocalStorageService storage, RateLimiter rateLimiter, ClientIpResolver ipResolver) {
        this.storage = storage;
        this.rateLimiter = rateLimiter;
        this.ipResolver = ipResolver;
    }
    @PostMapping
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        checkRate(request);
        if (file.isEmpty() || file.getSize() > MAX_SIZE) throw new BizException("图片不能为空或超过 10MB");
        try { return store(file.getBytes()); }
        catch (java.io.IOException ex) { throw new BizException("读取图片失败"); }
    }
    @PostMapping("/base64")
    public R<Map<String, String>> uploadBase64(@RequestBody Map<String, String> body, HttpServletRequest request) {
        checkRate(request);
        String base64 = body.get("base64");
        if (base64 == null || base64.isBlank() || base64.length() > ((MAX_SIZE + 2) / 3) * 4 + 128)
            throw new BizException("图片不能为空或超过 10MB");
        if (base64.startsWith("data:")) {
            int comma = base64.indexOf(',');
            if (comma < 0 || !base64.substring(0, comma).endsWith(";base64")) throw new BizException("无效的 base64 格式");
            base64 = base64.substring(comma + 1);
        }
        try { return store(Base64.getDecoder().decode(base64)); }
        catch (IllegalArgumentException ex) { throw new BizException("无效的 base64 格式"); }
    }
    @DeleteMapping
    public R<Void> delete(@RequestParam("url") String url, HttpServletRequest request) {
        checkRate(request);
        storage.deleteByUrl(url);
        return R.ok();
    }
    private R<Map<String, String>> store(byte[] bytes) {
        if (bytes.length > MAX_SIZE) throw new BizException("图片不能超过 10MB");
        try {
            var image = SafeImageProcessor.process(bytes, 1920);
            return R.ok(Map.of("url", storage.uploadBytes(image.bytes(), image.extension())));
        } catch (BizException ex) { throw ex; }
        catch (Exception ex) { throw new BizException("上传失败，请检查图片内容"); }
    }
    private void checkRate(HttpServletRequest request) {
        rateLimiter.acquireOrThrow("upload:admin:" + ipResolver.resolve(request), 30, 60);
    }
}
