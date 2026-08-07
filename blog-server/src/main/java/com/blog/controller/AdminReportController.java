package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.common.R;
import com.blog.entity.Comment;
import com.blog.entity.CommentReport;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.CommentReportMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 后台 - 举报管理：评论/留言的举报记录
 */
@Tag(name = "后台 - 举报管理")
@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {

    private final CommentReportMapper reportMapper;
    private final CommentMapper commentMapper;

    public AdminReportController(CommentReportMapper reportMapper, CommentMapper commentMapper) {
        this.reportMapper = reportMapper;
        this.commentMapper = commentMapper;
    }

    @GetMapping
    @Operation(summary = "分页举报列表")
    public R<Page<CommentReport>> page(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "15") long size,
                                      @RequestParam(required = false) Integer status) {
        Page<CommentReport> p = Page.of(page, size);
        LambdaQueryWrapper<CommentReport> w = new LambdaQueryWrapper<CommentReport>()
                .orderByDesc(CommentReport::getCreateTime);
        if (status != null) w.eq(CommentReport::getStatus, status);
        Page<CommentReport> result = reportMapper.selectPage(p, w);

        // 填充被举报留言的摘要、昵称(用于表格里展示)
        List<CommentReport> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            Set<Long> commentIds = new HashSet<>();
            for (CommentReport r : records) if (r.getCommentId() != null) commentIds.add(r.getCommentId());
            if (!commentIds.isEmpty()) {
                List<Comment> comments = commentMapper.selectBatchIds(commentIds);
                Map<Long, Comment> map = new HashMap<>();
                for (Comment c : comments) map.put(c.getId(), c);
                for (CommentReport r : records) {
                    Comment c = map.get(r.getCommentId());
                    if (c != null) {
                        r.setCommentNickname(c.getNickname());
                        String content = c.getContent();
                        r.setCommentExcerpt(content == null ? "" :
                            content.length() > 60 ? content.substring(0, 60) + "…" : content);
                    }
                }
            }
        }
        return R.ok(result);
    }

    @GetMapping("/stats")
    @Operation(summary = "举报状态统计")
    public R<Map<String, Long>> stats() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("pending",  reportMapper.selectCount(new LambdaQueryWrapper<CommentReport>().eq(CommentReport::getStatus, 0)));
        map.put("resolved", reportMapper.selectCount(new LambdaQueryWrapper<CommentReport>().eq(CommentReport::getStatus, 1)));
        map.put("dismissed",reportMapper.selectCount(new LambdaQueryWrapper<CommentReport>().eq(CommentReport::getStatus, 2)));
        return R.ok(map);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新状态：0=待处理 1=已处理 2=已驳回")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        if (body == null || body.get("status") == null) throw new BizException("status 必填");
        Integer status = body.get("status");
        if (status < 0 || status > 2) throw new BizException("status 必须为 0/1/2");
        CommentReport r = new CommentReport();
        r.setId(id);
        r.setStatus(status);
        reportMapper.updateById(r);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除一条举报记录")
    public R<Void> delete(@PathVariable Long id) {
        reportMapper.deleteById(id);
        return R.ok();
    }
}