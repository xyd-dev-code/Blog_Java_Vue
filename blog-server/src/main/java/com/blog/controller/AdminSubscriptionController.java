package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.SubscribeDTO;
import com.blog.entity.EmailSubscription;
import com.blog.service.SubscriptionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "后台 - 邮箱订阅")
@RestController
@RequestMapping("/api/v1/admin/subscriptions")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    public AdminSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @Operation(summary = "分页订阅者列表")
    public R<Page<EmailSubscription>> page(
            @RequestParam(defaultValue = "1") @jakarta.validation.constraints.Min(1) long page,
            @RequestParam(defaultValue = "15") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(200) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return R.ok(subscriptionService.adminPage(page, size, keyword, status));
    }

    @GetMapping("/stats")
    @Operation(summary = "订阅统计")
    public R<Map<String, Long>> stats() {
        return R.ok(subscriptionService.adminStats());
    }

    @PostMapping
    @Operation(summary = "后台添加订阅（直接已确认）")
    public R<EmailSubscription> create(@RequestBody @Valid SubscribeDTO dto) {
        return R.ok(subscriptionService.adminCreate(dto.getEmail()));
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "手动确认订阅")
    public R<Void> confirm(@PathVariable Long id) {
        subscriptionService.adminConfirm(id);
        return R.ok();
    }

    @PutMapping("/{id}/unsubscribe")
    @Operation(summary = "后台退订（保留记录，标记为已退订）")
    public R<Void> unsubscribe(@PathVariable Long id) {
        subscriptionService.adminUnsubscribe(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除订阅（彻底移除记录）")
    public R<Void> delete(@PathVariable Long id) {
        subscriptionService.adminDelete(id);
        return R.ok();
    }

    @GetMapping("/export")
    @Operation(summary = "导出订阅者 CSV")
    public ResponseEntity<byte[]> export() {
        String csv = subscriptionService.exportCsv();
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        String filename = "subscriptions-" + LocalDate.now() + ".csv";
        String disposition = "attachment; filename=\"" + filename
                + "\"; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }
}
