package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.VisitLog;
import com.blog.mapper.VisitLogMapper;
import com.blog.util.IpRegionUtil;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VisitLogLocationTest {
    @Test void provinceChartMergesCitiesAndPreservesVisitCounts() {
        VisitLogMapper mapper = mock(VisitLogMapper.class);
        IpRegionUtil regions = mock(IpRegionUtil.class);
        when(mapper.selectMaps(any())).thenAnswer(invocation -> {
            QueryWrapper<VisitLog> query = invocation.getArgument(0);
            if (query.getSqlSelect().equals("ip AS k, COUNT(*) AS c")) return List.of(
                    Map.of("k", "ip-changsha", "c", 38L),
                    Map.of("k", "ip-zhuzhou", "c", 12L),
                    Map.of("k", "ip-fushun", "c", 5L),
                    Map.of("k", "ip-unknown", "c", 2L));
            return List.of();
        });
        when(regions.resolveProvince("ip-changsha")).thenReturn("湖南省");
        when(regions.resolveProvince("ip-zhuzhou")).thenReturn("湖南省");
        when(regions.resolveProvince("ip-fushun")).thenReturn("辽宁省");
        Map<String, Object> stats = new VisitLogService(mapper, regions).todayStats(LocalDate.of(2026, 9, 8));
        assertEquals(Map.of("湖南省", 50L, "辽宁省", 5L, "未知", 2L), stats.get("byProvince"));
    }

    @Test void provinceFilterFindsRowsWhoseStoredProvinceIsACity() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                        new com.baomidou.mybatisplus.core.MybatisConfiguration(), "location-test"), VisitLog.class);
        VisitLogMapper mapper = mock(VisitLogMapper.class);
        IpRegionUtil regions = mock(IpRegionUtil.class);
        VisitLog old = row("ip-changsha", "长沙市");
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(old));
        when(regions.resolveProvince("ip-changsha")).thenReturn("湖南省");
        Page<VisitLog> page = new Page<>(1, 10);
        page.setRecords(List.of(old));
        when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Wrapper<VisitLog> query = invocation.getArgument(1);
            assertTrue(query.getSqlSegment().contains("ip IN"));
            assertFalse(query.getSqlSegment().contains("province ="));
            return page;
        });
        VisitLogService service = new VisitLogService(mapper, regions);
        assertEquals(1, service.page(null, null, null, null, null, null, "湖南省", 1, 10).getRecords().size());
        assertEquals(0, service.page(null, null, null, null, null, null, "辽宁省", 1, 10).getTotal());
        verify(mapper, times(1)).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test void enrichesHistoricalRowsWithoutChangingStoredProvince() {
        VisitLogMapper mapper = mock(VisitLogMapper.class);
        IpRegionUtil regions = mock(IpRegionUtil.class);
        VisitLog first = row("203.0.113.42", "长沙市");
        VisitLog second = row("203.0.113.42", "长沙市");
        VisitLog unavailable = row("192.0.2.1", "旧地区");
        Page<VisitLog> result = new Page<>(1, 10);
        result.setRecords(List.of(first, second, unavailable));
        when(mapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(result);
        when(regions.resolveLocation("203.0.113.42")).thenReturn("湖南省·长沙市");
        when(regions.resolveLocation("192.0.2.1")).thenReturn("");
        new VisitLogService(mapper, regions).page(null, null, null, null, null, null, null, 1, 10);
        assertEquals("湖南省·长沙市", first.getLocation());
        assertEquals("湖南省·长沙市", second.getLocation());
        assertEquals("长沙市", first.getProvince());
        assertEquals("旧地区", unavailable.getLocation());
        verify(regions, times(1)).resolveLocation("203.0.113.42");
    }

    private VisitLog row(String ip, String province) {
        VisitLog row = new VisitLog();
        row.setIp(ip);
        row.setProvince(province);
        return row;
    }
}
