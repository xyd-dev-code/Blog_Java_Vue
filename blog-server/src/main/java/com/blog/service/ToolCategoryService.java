package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.common.BizException;
import com.blog.dto.ToolCategoryDTO;
import com.blog.entity.Tool;
import com.blog.entity.ToolCategory;
import com.blog.mapper.ToolCategoryMapper;
import com.blog.mapper.ToolMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToolCategoryService {

    private static final String MODULE = "工具分类";

    private final ToolCategoryMapper categoryMapper;
    private final ToolMapper toolMapper;
    private final OperationLogService logService;

    @Autowired
    public ToolCategoryService(ToolCategoryMapper categoryMapper, ToolMapper toolMapper, OperationLogService logService) {
        this.categoryMapper = categoryMapper;
        this.toolMapper = toolMapper;
        this.logService = logService;
    }

    public List<ToolCategory> listAll() {
        List<ToolCategory> list = categoryMapper.selectList(new LambdaQueryWrapper<ToolCategory>()
                .orderByAsc(ToolCategory::getSortOrder)
                .orderByAsc(ToolCategory::getId));
        // 关联统计:每个分类下的未删除工具数
        Map<String, Long> counts = toCountMap(categoryMapper.countToolsByCode());
        for (ToolCategory c : list) {
            c.setToolCount(counts.getOrDefault(c.getCode(), 0L));
        }
        return list;
    }

    public ToolCategory getById(Long id) {
        ToolCategory c = categoryMapper.selectById(id);
        if (c == null) throw new BizException("分类不存在");
        return c;
    }

    public Long create(ToolCategoryDTO dto) {
        checkCodeUnique(null, dto.getCode());
        ToolCategory c = new ToolCategory();
        applyDto(c, dto);
        if (c.getSortOrder() == null) c.setSortOrder(1);
        if (c.getStatus() == null) c.setStatus(1);
        pushDownSortOrder(null, c.getSortOrder());
        renumberSortOrder();
        categoryMapper.insert(c);
        logService.record(MODULE, "create", c.getCode(),
                "新建分类 " + c.getName() + "(" + c.getCode() + ")");
        return c.getId();
    }

    public void update(Long id, ToolCategoryDTO dto) {
        ToolCategory c = getById(id);
        String oldName = c.getName();
        String oldCode = c.getCode();
        if (!c.getCode().equals(dto.getCode())) {
            checkCodeUnique(id, dto.getCode());
            // 级联同步：以旧 code 关联的工具一并改为新 code，否则前台会匹配不到、渲染成「其他」
            toolMapper.update(null, new LambdaUpdateWrapper<Tool>()
                    .eq(Tool::getCategory, oldCode)
                    .set(Tool::getCategory, dto.getCode()));
        }
        Integer oldSort = c.getSortOrder();
        applyDto(c, dto);
        // 位次变化 → 下推冲突
        if (c.getSortOrder() != null && !c.getSortOrder().equals(oldSort)) {
            pushDownSortOrder(id, c.getSortOrder());
            renumberSortOrder();
        }
        categoryMapper.updateById(c);
        logService.record(MODULE, "update", c.getCode(),
                "编辑分类 " + oldName + "(" + oldCode + ") -> " + c.getName() + "(" + c.getCode() + ")");
    }

    /** 切换状态:1=正常 0=下线 */
    public void updateStatus(Long id, Integer status) {
        ToolCategory c = getById(id);
        int s = (status == null || status != 0) ? 1 : 0;
        c.setStatus(s);
        categoryMapper.updateById(c);
        logService.record(MODULE, "status", c.getCode(),
                "分类 " + c.getName() + " 状态 -> " + (s == 1 ? "正常" : "下线"));
    }

    public void delete(Long id) {
        ToolCategory c = getById(id);
        // 删除保护:分类下还有工具则拒绝,避免工具变孤儿
        Long cnt = toCountMap(categoryMapper.countToolsByCode()).getOrDefault(c.getCode(), 0L);
        if (cnt != null && cnt > 0) {
            throw new BizException("该分类下还有 " + cnt + " 个工具,无法删除。请先迁移或删除这些工具。");
        }
        categoryMapper.deleteById(c.getId());
        logService.record(MODULE, "delete", c.getCode(),
                "删除分类 " + c.getName() + "(" + c.getCode() + ")");
    }

    @Transactional
    public void reorder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            categoryMapper.update(null, new LambdaUpdateWrapper<ToolCategory>()
                    .eq(ToolCategory::getId, id)
                    .set(ToolCategory::getSortOrder, i + 1));
        }
    }

    /** 位次冲突下推：sortOrder>=target 的其他记录降序 +1 */
    private void pushDownSortOrder(Long excludeId, int target) {
        LambdaQueryWrapper<ToolCategory> w = new LambdaQueryWrapper<ToolCategory>()
                .ne(excludeId != null, ToolCategory::getId, excludeId)
                .ge(ToolCategory::getSortOrder, target)
                .orderByDesc(ToolCategory::getSortOrder);
        List<ToolCategory> conflicts = categoryMapper.selectList(w);
        for (ToolCategory c : conflicts) {
            c.setSortOrder(c.getSortOrder() + 1);
            categoryMapper.updateById(c);
        }
    }

    /** 归一化位次：按当前排序重排为 1,2,3...（消除 pushDown 留下的空位） */
    private void renumberSortOrder() {
        List<ToolCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<ToolCategory>()
                .orderByAsc(ToolCategory::getSortOrder)
                .orderByAsc(ToolCategory::getId));
        for (int i = 0; i < all.size(); i++) {
            int target = i + 1;
            ToolCategory c = all.get(i);
            if (c.getSortOrder() == null || c.getSortOrder() != target) {
                c.setSortOrder(target);
                categoryMapper.updateById(c);
            }
        }
    }

    private void checkCodeUnique(Long excludeId, String code) {
        LambdaQueryWrapper<ToolCategory> w = new LambdaQueryWrapper<ToolCategory>()
                .eq(ToolCategory::getCode, code);
        if (excludeId != null) w.ne(ToolCategory::getId, excludeId);
        if (categoryMapper.selectCount(w) > 0) {
            throw new BizException("分类键已存在,请换一个");
        }
    }

    private void applyDto(ToolCategory c, ToolCategoryDTO dto) {
        c.setCode(dto.getCode());
        c.setName(dto.getName());
        c.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) c.setStatus(dto.getStatus());
    }

    /** 将 countToolsByCode 返回的 List<Map> 转为 Map<String, Long> */
    private Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) return map;
        for (Map<String, Object> row : rows) {
            String code = row.get("code") != null ? row.get("code").toString() : "";
            Object cnt = row.get("cnt");
            map.put(code, cnt != null ? ((Number) cnt).longValue() : 0L);
        }
        return map;
    }
}
