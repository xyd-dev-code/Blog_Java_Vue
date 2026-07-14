package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.SiteConfig;
import com.blog.service.SiteConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "后台 - 站点配置")
@RestController
@RequestMapping("/api/v1/admin/site")
public class AdminSiteController {
    private final SiteConfigService siteConfigService;

    public AdminSiteController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    @GetMapping
    @Operation(summary = "获取全部站点配置")
    public R<Map<String, String>> all() {
        return R.ok(siteConfigService.allAsMap());
    }

    @PutMapping
    @Operation(summary = "保存配置（map<key,value>）")
    public R<Void> save(@RequestBody Map<String, String> data) {
        siteConfigService.save(data);
        return R.ok();
    }
}
