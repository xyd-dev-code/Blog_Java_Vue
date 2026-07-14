package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.service.SiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Tag(name = "前台 - 站点配置")
@RestController
@RequestMapping("/api/v1/site")
public class SiteController {

    private final SiteConfigService siteConfigService;
    private final UserMapper userMapper;

    public SiteController(SiteConfigService siteConfigService, UserMapper userMapper) {
        this.siteConfigService = siteConfigService;
        this.userMapper = userMapper;
    }

    @GetMapping
    @Operation(summary = "读取全部配置")
    public R<Map<String, String>> all() {
        Map<String, String> data = siteConfigService.allAsMap();
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
        return R.ok(data);
    }

    /**
     * favicon：取 admin 头像 URL，302 重定向到实际图片。
     * 优先用 admin 头像，否则 fallback 到 site_config.siteLogo，再否则 fallback 到项目自带 SVG。
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
        // 302 重定向到实际图片 URL（CDN / 本地图床都行）
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        headers.setCacheControl("public, max-age=300");
        return new ResponseEntity<>(headers, org.springframework.http.HttpStatus.FOUND);
    }
}