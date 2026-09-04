package com.blog.controller;

import com.blog.common.R;
import com.blog.service.ToolService;
import com.blog.vo.ToolPublicVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "前台 - 在线工具箱")
@RestController
@RequestMapping("/api/v1/tools")
public class PublicToolController {

    private final ToolService toolService;
    private final com.blog.security.RateLimiter rateLimiter;
    private final com.blog.security.ClientIpResolver ipResolver;

    public PublicToolController(ToolService toolService, com.blog.security.RateLimiter rateLimiter,
                               com.blog.security.ClientIpResolver ipResolver) {
        this.toolService = toolService;
        this.rateLimiter = rateLimiter;
        this.ipResolver = ipResolver;
    }

    /** 前台工具列表(可选 category 过滤);返回正常、维护中和预告工具。 */
    @GetMapping
    public R<List<ToolPublicVO>> list(@RequestParam(required = false) String category) {
        return R.ok(ToolPublicVO.list(toolService.listPublished(category)));
    }

    @GetMapping("/{id}")
    public R<ToolPublicVO> detail(@PathVariable Long id) {
        return R.ok(ToolPublicVO.from(toolService.getPublishedById(id)));
    }

    @GetMapping("/slug/{slug}")
    public R<ToolPublicVO> detailBySlug(@PathVariable String slug) {
        return R.ok(ToolPublicVO.from(toolService.getBySlug(slug)));
    }

    /** 累计点击 +1 + 今日点击 upsert */
    @PostMapping("/{id}/click")
    public R<Void> click(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        rateLimiter.acquireOrThrow("tool:click:" + ipResolver.resolve(request), 60, 60);
        toolService.recordClick(id);
        return R.ok();
    }

    /** 今日热门 Top N(给前端 banner 用) */
    @GetMapping("/hot")
    public R<List<ToolPublicVO>> hot(@RequestParam(defaultValue = "4") int limit) {
        return R.ok(ToolPublicVO.list(toolService.hotToday(Math.min(limit, 20))));
    }
}
