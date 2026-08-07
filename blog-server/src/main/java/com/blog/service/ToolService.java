package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.dto.ToolDTO;
import com.blog.entity.Tool;
import com.blog.entity.ToolDailyClick;
import com.blog.mapper.ToolDailyClickMapper;
import com.blog.mapper.ToolMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class ToolService {

    private final ToolMapper toolMapper;
    private final ToolDailyClickMapper clickMapper;

    @Autowired
    public ToolService(ToolMapper toolMapper, ToolDailyClickMapper clickMapper) {
        this.toolMapper = toolMapper;
        this.clickMapper = clickMapper;
    }

    /** 前台:只查 status=1 的工具,按 sort_order 升序 */
    public List<Tool> listPublished(String category) {
        LambdaQueryWrapper<Tool> w = new LambdaQueryWrapper<Tool>()
                .eq(Tool::getStatus, 1)
                .orderByAsc(Tool::getSortOrder)
                .orderByDesc(Tool::getId);
        if (category != null && !category.isEmpty() && !"all".equals(category)) {
            w.eq(Tool::getCategory, category);
        }
        return toolMapper.selectList(w);
    }

    public Tool getById(Long id) {
        Tool t = toolMapper.selectById(id);
        if (t == null) throw new BizException("工具不存在");
        return t;
    }

    public Tool getBySlug(String slug) {
        Tool t = toolMapper.selectOne(new LambdaQueryWrapper<Tool>()
                .eq(Tool::getSlug, slug)
                .eq(Tool::getStatus, 1)
                .last("LIMIT 1"));
        if (t == null) throw new BizException("工具不存在或已下线");
        return t;
    }

    /** 累计点击 +1 + 今日点击 upsert */
    @Transactional
    public void recordClick(Long toolId) {
        if (toolId == null) return;
        // click_count + 1
        toolMapper.update(null, new LambdaUpdateWrapper<Tool>()
                .eq(Tool::getId, toolId)
                .setSql("click_count = click_count + 1"));
        // tool_daily_click upsert
        LocalDate today = LocalDate.now();
        ToolDailyClick exist = clickMapper.selectOne(new LambdaQueryWrapper<ToolDailyClick>()
                .eq(ToolDailyClick::getToolId, toolId)
                .eq(ToolDailyClick::getClickDate, today)
                .last("LIMIT 1"));
        if (exist == null) {
            ToolDailyClick rec = new ToolDailyClick();
            rec.setToolId(toolId);
            rec.setClickDate(today);
            rec.setClickCount(1);
            clickMapper.insert(rec);
        } else {
            clickMapper.update(null, new LambdaUpdateWrapper<ToolDailyClick>()
                    .eq(ToolDailyClick::getId, exist.getId())
                    .setSql("click_count = click_count + 1"));
        }
    }

    // ====== 后台 ======

    /** 后台分页(可按 category / status / name 过滤) */
    public Page<Tool> page(long page, long size, String category, Integer status, String keyword) {
        Page<Tool> p = Page.of(page, size);
        LambdaQueryWrapper<Tool> w = new LambdaQueryWrapper<Tool>()
                .orderByAsc(Tool::getSortOrder)
                .orderByDesc(Tool::getId);
        if (category != null && !category.isEmpty()) w.eq(Tool::getCategory, category);
        if (status != null) w.eq(Tool::getStatus, status);
        if (keyword != null && !keyword.isBlank()) {
            w.and(qw -> qw.like(Tool::getName, keyword).or().like(Tool::getSlug, keyword));
        }
        return toolMapper.selectPage(p, w);
    }

    /** 下一个可用序号 = 当前最大 sort_order + 1(无记录返回 1)。新建工具默认接在末尾 */
    public int nextSortOrder() {
        Tool maxTool = toolMapper.selectOne(new LambdaQueryWrapper<Tool>()
                .select(Tool::getSortOrder)
                .orderByDesc(Tool::getSortOrder)
                .last("LIMIT 1"));
        return (maxTool == null || maxTool.getSortOrder() == null)
                ? 1 : maxTool.getSortOrder() + 1;
    }

    public Long create(ToolDTO dto) {
        // slug 唯一性检查
        Long dup = toolMapper.selectCount(new LambdaQueryWrapper<Tool>()
                .eq(Tool::getSlug, dto.getSlug()));
        if (dup > 0) throw new BizException("slug 已存在,请换一个");
        Tool t = new Tool();
        applyDto(t, dto);
        if (t.getSortOrder() == null) t.setSortOrder(0);
        if (t.getStatus() == null) t.setStatus(1);
        if (t.getType() == null) t.setType(0);
        // 插入式让位:把 >= 新位次的其他工具各 +1
        pushDownSortOrder(null, t.getSortOrder());
        toolMapper.insert(t);
        renumberSortOrder();   // 入库后归一化,保证 1..N 连续(含手动填超大序号的场景)
        return t.getId();
    }

    public void update(Long id, ToolDTO dto) {
        Tool t = toolMapper.selectById(id);
        if (t == null) throw new BizException("工具不存在");
        // slug 若变更,查唯一
        if (!t.getSlug().equals(dto.getSlug())) {
            Long dup = toolMapper.selectCount(new LambdaQueryWrapper<Tool>()
                    .eq(Tool::getSlug, dto.getSlug())
                    .ne(Tool::getId, id));
            if (dup > 0) throw new BizException("slug 已存在,请换一个");
        }
        Integer oldSort = t.getSortOrder();
        applyDto(t, dto);
        Integer newSort = t.getSortOrder();
        // 位次变化 → 平移中间项落位,再全局归一化,保证 1..N 始终连续(连续场景 renumber 为空操作)
        if (newSort != null && !newSort.equals(oldSort)) {
            shiftSortOrder(id, oldSort, newSort);
            toolMapper.updateById(t);
            renumberSortOrder();
        } else {
            toolMapper.updateById(t);
        }
    }

    /**
     * 编辑平移位次:被编辑项从 oldSort 移到 newSort,中间工具紧随平移一位,
     * 不产生空洞也不撑开(区别于新建时的「插入式让位」)。
     * - 往前移(newSort<oldSort):区间 [newSort, oldSort-1] 各 +1 让位
     * - 往后移(newSort>oldSort):区间 [oldSort+1, newSort] 各 -1 填补
     * 排除被编辑项自身,单条 UPDATE 原子完成,无链式冲突。
     */
    private void shiftSortOrder(Long excludeId, int oldSort, int newSort) {
        if (newSort == oldSort) return;
        LambdaUpdateWrapper<Tool> w = new LambdaUpdateWrapper<Tool>()
                .ne(Tool::getId, excludeId);
        if (newSort < oldSort) {
            w.ge(Tool::getSortOrder, newSort).lt(Tool::getSortOrder, oldSort)
             .setSql("sort_order = sort_order + 1");
        } else {
            w.gt(Tool::getSortOrder, oldSort).le(Tool::getSortOrder, newSort)
             .setSql("sort_order = sort_order - 1");
        }
        toolMapper.update(null, w);
    }

    /**
     * 位次冲突下推:把所有 sortOrder>=target 的工具(排除 excludeId)按 sortOrder 降序
     * 自增 +1,避免链式 UPDATE 互相覆盖。
     */
    private void pushDownSortOrder(Long excludeId, int target) {
        List<Tool> conflicts = toolMapper.selectList(new LambdaQueryWrapper<Tool>()
                .ne(excludeId != null, Tool::getId, excludeId)
                .ge(Tool::getSortOrder, target)
                .orderByDesc(Tool::getSortOrder));
        for (Tool c : conflicts) {
            toolMapper.update(null, new LambdaUpdateWrapper<Tool>()
                    .eq(Tool::getId, c.getId())
                    .set(Tool::getSortOrder, c.getSortOrder() + 1));
        }
    }

    /** 归一化位次：按当前排序重排为 1,2,3...（消除 pushDown 留下的空位） */
    private void renumberSortOrder() {
        List<Tool> all = toolMapper.selectList(new LambdaQueryWrapper<Tool>()
                .orderByAsc(Tool::getSortOrder)
                .orderByDesc(Tool::getId));
        for (int i = 0; i < all.size(); i++) {
            int target = i + 1;
            Tool c = all.get(i);
            if (c.getSortOrder() == null || c.getSortOrder() != target) {
                toolMapper.update(null, new LambdaUpdateWrapper<Tool>()
                        .eq(Tool::getId, c.getId())
                        .set(Tool::getSortOrder, target));
            }
        }
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || status < 0 || status > 3) throw new BizException("status 不合法");
        Tool t = toolMapper.selectById(id);
        if (t == null) throw new BizException("工具不存在");
        toolMapper.update(null, new LambdaUpdateWrapper<Tool>()
                .eq(Tool::getId, id)
                .set(Tool::getStatus, status));
    }

    public void delete(Long id) {
        Tool t = toolMapper.selectById(id);
        if (t == null) throw new BizException("工具不存在");
        toolMapper.deleteById(id);
        renumberSortOrder();   // 删除后归一化,保持 1..N 连续
    }

    /** 拖拽排序:整组 ids 按新顺序回传,根据数组下标写回 sort_order（1-indexed 位次） */
    @Transactional
    public void reorder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            toolMapper.update(null, new LambdaUpdateWrapper<Tool>()
                    .eq(Tool::getId, id)
                    .set(Tool::getSortOrder, i + 1));
        }
    }

    /** 取出今日点击 Top N(给前台 banner 用,可空) */
    public List<Tool> hotToday(int limit) {
        LocalDate today = LocalDate.now();
        // 直接返回最近点击若干条(简化实现:按 click_count 倒序)
        return toolMapper.selectList(new LambdaQueryWrapper<Tool>()
                .eq(Tool::getStatus, 1)
                .orderByDesc(Tool::getClickCount)
                .last("LIMIT " + Math.max(1, limit)));
    }

    public List<Tool> safeList(List<Tool> in) { return in == null ? Collections.emptyList() : in; }

    private void applyDto(Tool t, ToolDTO dto) {
        t.setName(dto.getName());
        t.setSlug(dto.getSlug());
        t.setIcon(dto.getIcon());
        t.setCategory(dto.getCategory());
        t.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        t.setUrl(dto.getUrl() == null ? "" : dto.getUrl());
        t.setType(dto.getType());
        t.setStatus(dto.getStatus());
        t.setSortOrder(dto.getSortOrder());
        t.setAnnouncement(dto.getAnnouncement() == null ? "" : dto.getAnnouncement());
    }
}