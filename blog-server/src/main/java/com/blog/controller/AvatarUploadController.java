package com.blog.controller;
import com.blog.common.BizException;
import com.blog.common.R;
import com.blog.security.ClientIpResolver;
import com.blog.security.RateLimiter;
import com.blog.service.LocalStorageService;
import com.blog.service.SafeImageProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Tag(name = "访客 - 头像上传")
@RestController
@RequestMapping("/api/v1/upload/avatar")
public class AvatarUploadController {
    private final LocalStorageService storage;
    private final ClientIpResolver ipResolver;
    private final RateLimiter rateLimiter;
    public AvatarUploadController(LocalStorageService storage, ClientIpResolver ipResolver, RateLimiter rateLimiter) {
        this.storage = storage;
        this.ipResolver = ipResolver;
        this.rateLimiter = rateLimiter;
    }
    @PostMapping
    @Operation(summary = "上传访客头像（PNG/JPEG/GIF，最大 2MB）")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        rateLimiter.acquireOrThrow("upload:avatar:" + ipResolver.resolve(request), 20, 60);
        rateLimiter.acquireOrThrow("upload:avatar:global", 300, 3600);
        if (file.isEmpty()) throw new BizException("文件为空");
        if (file.getSize() > 2 * 1024 * 1024) throw new BizException("头像不能超过 2MB");
        try {
            var image = SafeImageProcessor.process(file.getBytes(), 512);
            return R.ok(Map.of("url", storage.uploadBytes(image.bytes(), image.extension())));
        } catch (BizException ex) { throw ex; }
        catch (Exception ex) { throw new BizException("头像上传失败，请检查图片内容"); }
    }
}
