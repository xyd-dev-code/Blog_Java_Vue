package com.blog.controller;

import com.blog.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "系统")
@RestController
@RequestMapping("/api/v1")
public class SystemController {

    @GetMapping("/ping")
    @Operation(summary = "健康检查")
    public R<Map<String, Object>> ping() {
        return R.ok(Map.of("pong", true, "time", LocalDateTime.now().toString()));
    }
}