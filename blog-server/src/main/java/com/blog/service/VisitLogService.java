package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.VisitLog;
import com.blog.mapper.VisitLogMapper;
import com.blog.util.IpRegionUtil;
import com.blog.util.UserAgentUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VisitLogService {
    private final VisitLogMapper visitLogMapper;
    private final IpRegionUtil ipRegionUtil;

    public VisitLogService(VisitLogMapper visitLogMapper, IpRegionUtil ipRegionUtil) {
        this.visitLogMapper = visitLogMapper;
        this.ipRegionUtil = ipRegionUtil;
    }

    /**
     * 异步写访问日志(由 Filter 触发,不阻塞业务请求)。
     * 自带爬虫过滤:bot/crawler/spider 跳过,免得表被刷爆。
     */
    @Async("mailTaskExecutor")
    public void recordAsync(String ip, String path, String userAgent) {
        if (UserAgentUtil.isBot(userAgent)) return;
        try {
            VisitLog v = new VisitLog();
            v.setIp(ip == null ? "" : ip);
            v.setPath(path == null ? "" : (path.length() > 255 ? path.substring(0, 255) : path));
            v.setUserAgent(UserAgentUtil.truncate(userAgent, 512));
            UserAgentUtil.Info info = UserAgentUtil.parse(userAgent);
            v.setDeviceType(info.deviceType);
            v.setOs(info.os);
            v.setBrowser(info.browser);
            v.setProvince(ipRegionUtil.resolveProvince(v.getIp()));
            v.setVisitTime(LocalDateTime.now());
            visitLogMapper.insert(v);
        } catch (Exception e) {
            // 写日志失败不应影响请求 — 静默
        }
    }

    // ────────────────────────────────────── 统计聚合 ──────────────────────────────────────

    /**
     * 今日统计(默认 LocalDate.now(),可指定日期)。
     * 全程用 SQL 聚合,避免把全表数据拉回内存(PV 几十万时性能差异显著)。
     * 返回:
     *   pv — 今日总访问
     *   uv — 今日独立 IP 数
     *   peakHour — 访问量最大的小时(0-23),null 表示无访问
     *   byHour — 24 个 key 的 map,hour -> count
     *   byDevice / byOs / byBrowser — 设备/系统/浏览器分布
     */
    public Map<String, Object> todayStats(LocalDate date) {
        if (date == null) date = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);

        // 1) 全天 PV / UV
        Map<String, Object> agg = visitLogMapper.selectMaps(
                new QueryWrapper<VisitLog>()
                        .select("COUNT(*) AS pv", "COUNT(DISTINCT ip) AS uv")
                        .between("visit_time", start, end)
        ).stream().findFirst().orElse(new HashMap<>());

        long pv = toLong(agg.get("pv"));
        long uv = toLong(agg.get("uv"));

        // 2) 峰值小时（按小时分组，取访问量最大的一行）
        Map<String, Object> peak = visitLogMapper.selectMaps(
                new QueryWrapper<VisitLog>()
                        .select("HOUR(visit_time) AS peak_hour", "COUNT(*) AS peak_count")
                        .between("visit_time", start, end)
                        .groupBy("HOUR(visit_time)")
                        .orderByDesc("peak_count")
                        .last("LIMIT 1")
        ).stream().findFirst().orElse(new HashMap<>());
        Long peakHour = peak.get("peak_hour") == null ? null : toLong(peak.get("peak_hour"));

        // 3) 分时柱图(24 小时分桶,缺的小时补 0)
        Map<String, Long> byHourMap = new LinkedHashMap<>();
        long[] buckets = new long[24];
        visitLogMapper.selectMaps(
                new QueryWrapper<VisitLog>()
                        .select("HOUR(visit_time) AS h, COUNT(*) AS c")
                        .between("visit_time", start, end)
                        .groupBy("HOUR(visit_time)")
        ).forEach(row -> {
            int h = ((Number) row.get("h")).intValue();
            buckets[h] = toLong(row.get("c"));
        });
        for (int h = 0; h < 24; h++) byHourMap.put(String.format("%02d", h), buckets[h]);
        // peakHour 已在第 1 步 SQL 中拿到,此处无需再算

        // 3) 设备/系统/浏览器/省份 分布(每个一行 GROUP BY)
        Map<String, Long> byDevice = distribution("device_type", start, end);
        Map<String, Long> byOs = distribution("os", start, end);
        Map<String, Long> byBrowser = distribution("browser", start, end);
        Map<String, Long> byProvince = distribution("province", start, end);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", date.toString());
        out.put("pv", pv);
        out.put("uv", uv);
        out.put("peakHour", peakHour);
        out.put("byHour", byHourMap);
        out.put("byDevice", byDevice);
        out.put("byOs", byOs);
        out.put("byBrowser", byBrowser);
        out.put("byProvince", byProvince);
        return out;
    }

    private Map<String, Long> distribution(String column, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> m = new LinkedHashMap<>();
        visitLogMapper.selectMaps(
                new QueryWrapper<VisitLog>()
                        .select(column + " AS k, COUNT(*) AS c")
                        .between("visit_time", start, end)
                        .groupBy(column)
                        .orderByDesc("c")
        ).forEach(row -> {
            Object k = row.get("k");
            if (k == null || k.toString().isBlank()) return;
            m.put(k.toString(), toLong(row.get("c")));
        });
        return m;
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    /** 详情分页(支持 ip/os/browser/deviceType/province 筛选 + 时间区间) */
    public Page<VisitLog> page(LocalDateTime start, LocalDateTime end, String ip,
                               String deviceType, String os, String browser, String province,
                               long page, long size) {
        LambdaQueryWrapper<VisitLog> w = new LambdaQueryWrapper<VisitLog>()
                .orderByDesc(VisitLog::getVisitTime);
        if (start != null && end != null) w.between(VisitLog::getVisitTime, start, end);
        else if (start != null) w.ge(VisitLog::getVisitTime, start);
        else if (end != null) w.le(VisitLog::getVisitTime, end);
        if (ip != null && !ip.isBlank()) w.like(VisitLog::getIp, ip.trim());
        if (deviceType != null && !deviceType.isBlank()) w.eq(VisitLog::getDeviceType, deviceType);
        if (os != null && !os.isBlank()) w.eq(VisitLog::getOs, os);
        if (browser != null && !browser.isBlank()) w.eq(VisitLog::getBrowser, browser);
        if (province != null && !province.isBlank()) w.eq(VisitLog::getProvince, province);
        return visitLogMapper.selectPage(Page.of(page, size), w);
    }
}