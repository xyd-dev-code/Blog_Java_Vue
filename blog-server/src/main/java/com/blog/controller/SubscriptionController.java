package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.SubscribeDTO;
import com.blog.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "前台 - 邮箱订阅")
@RestController
@RequestMapping("/api/v1/subscribe")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @Operation(summary = "邮箱订阅（双重确认：提交后发送确认邮件，点击链接完成订阅）")
    public R<Map<String, Object>> subscribe(@Valid @RequestBody SubscribeDTO dto, HttpServletRequest req) {
        return R.ok(subscriptionService.subscribe(dto.getEmail(), "web", req));
    }

    @GetMapping(value = "/confirm", produces = "text/html;charset=UTF-8")
    @Operation(summary = "点击邮件链接确认订阅（返回自包含 HTML 页面）")
    public String confirm(@RequestParam("token") String token) {
        return subscriptionService.confirm(token);
    }

    @GetMapping(value = "/unsubscribe", produces = "text/html;charset=UTF-8")
    @Operation(summary = "退订（点击邮件退订链接，逻辑删除订阅记录）")
    public String unsubscribe(@RequestParam("token") String token) {
        return subscriptionService.unsubscribe(token);
    }
}
