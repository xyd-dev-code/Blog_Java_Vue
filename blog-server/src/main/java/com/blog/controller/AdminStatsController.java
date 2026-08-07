package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.entity.VisitLog;
import com.blog.service.VisitLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Tag(name = "后台 - 访问统计")
@RestController
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {
    private final VisitLogService visitLogService;

    public AdminStatsController(VisitLogService visitLogService) {
        this.visitLogService = visitLogService;
    }

    /**
     * 今日(默认)或指定日期的统计聚合(PV/UV/按小时分布/设备/浏览器分布)
     */
    @GetMapping("/today")
    @Operation(summary = "今日统计")
    public R<Map<String, Object>> today(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(visitLogService.todayStats(date));
    }

    /**
     * 访客明细列表(分页 + 筛选)
     */
    @GetMapping("/logs")
    @Operation(summary = "访问日志分页")
    public R<Page<VisitLog>> logs(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String os,
            @RequestParam(required = false) String browser,
            @RequestParam(required = false) String province,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(500) long size) {
        // 前端只传日期不传时间时,默认 00:00:00 / 23:59:59
        if (start != null && end == null) end = LocalDateTime.of(LocalDate.from(start), LocalTime.MAX);
        if (end != null && start == null) start = LocalDateTime.of(LocalDate.from(end), LocalTime.MIN);
        return R.ok(visitLogService.page(start, end, ip, deviceType, os, browser, province, page, size));
    }
}