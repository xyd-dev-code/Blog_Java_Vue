package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQuery;
import com.blog.dto.ImportResult;
import com.blog.entity.Article;
import com.blog.security.ClientIpResolver;
import com.blog.security.RateLimiter;
import com.blog.service.ArticleImportExportService;
import com.blog.service.ArticleService;
import com.blog.service.ExportFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "后台 - 文章")
@RestController
@RequestMapping("/api/v1/admin/articles")
public class AdminArticleController {
    private final ArticleService articleService;
    private final ArticleImportExportService importExportService;
    private final RateLimiter rateLimiter;
    private final ClientIpResolver ipResolver;

    public AdminArticleController(ArticleService articleService,
                                  ArticleImportExportService importExportService,
                                  RateLimiter rateLimiter,
                                  ClientIpResolver ipResolver) {
        this.articleService = articleService;
        this.importExportService = importExportService;
        this.rateLimiter = rateLimiter;
        this.ipResolver = ipResolver;
    }

    @GetMapping
    @Operation(summary = "分页查询")
    public R<Page<Article>> page(ArticleQuery q) {
        return R.ok(articleService.pageAdmin(q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "文章详情（含标签）")
    public R<Article> byId(@PathVariable Long id) {
        return R.ok(articleService.detailById(id));
    }

    @GetMapping("/export/{id}")
    @Operation(summary = "导出单篇(format=md|docx|pdf,默认 md)")
    public ResponseEntity<byte[]> exportOne(
            @PathVariable Long id,
            @RequestParam(name = "format", required = false, defaultValue = "md") String format) {
        ExportFormat fmt = importExportService.parseFormat(format);
        byte[] body = importExportService.exportOne(id, fmt);
        Article a = articleService.detailById(id);
        String filename = fmt.contentDispositionFilename(a);
        String contentType = fmt.mediaType();
        if (fmt == ExportFormat.MD) {
            contentType = MediaType.TEXT_MARKDOWN_VALUE + ";charset=UTF-8";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename))
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }

    @GetMapping("/export-all")
    @Operation(summary = "导出全部文章为 zip(format=md|docx|pdf,默认 md)")
    public ResponseEntity<byte[]> exportAll(
            @RequestParam(name = "format", required = false, defaultValue = "md") String format) throws IOException {
        ExportFormat fmt = importExportService.parseFormat(format);
        byte[] zip = importExportService.exportAllAsZip(fmt);
        String filename = "articles-" + LocalDate.now() + "." + fmt.extension() + ".zip";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }

    /**
     * RFC 5987 编码的 Content-Disposition:同时输出 ASCII fallback 和 filename*=UTF-8''...
     * 老浏览器/Spring 自己的 HttpHeaderWriter 不一定能正确识别 ISO-8859-1 之外的字符,
     * 加 RFC 5987 编码兼容好。
     */
    private static String contentDisposition(String filename) {
        // ASCII fallback:把非 ASCII 字符替换成下划线,老古董浏览器兜底
        String ascii = filename.replaceAll("[^\\x20-\\x7e]", "_");
        // RFC 5987 percent-encode (UTF-8 bytes)
        String encoded = java.net.URLEncoder
                .encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    @GetMapping(value = "/export/template", produces = "text/markdown;charset=UTF-8")
    @Operation(summary = "下载导入模板 .md(含 front matter 字段注释)")
    public ResponseEntity<byte[]> exportTemplate() {
        byte[] body = importExportService.getMarkdownTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"blog-article-template.md\"")
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(body);
    }

    @GetMapping("/export/template.docx")
    @Operation(summary = "下载导入模板 .docx(Word 友好,但底层是 pandoc 渲染的同一份 MD 模板)")
    public ResponseEntity<byte[]> exportTemplateDocx() {
        byte[] body = importExportService.getDocxTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition("blog-article-template.docx"))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(body);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入 .md / .docx 文件(支持多文件,按 slug 去重更新;docx 走 pandoc 转 md 后解析)")
    public R<ImportResult> importMd(@RequestPart("files") MultipartFile[] files, HttpServletRequest req) {
        // 限频:admin 上传,5/min/IP。docx 走 pandoc 会耗 CPU,严一些
        rateLimiter.acquireOrThrow("article:import:ip:" + ipResolver.resolve(req), 5, 60);
        if (files == null || files.length == 0) {
            throw new com.blog.common.BizException("未选择文件");
        }
        if (files.length > 20) {
            throw new com.blog.common.BizException("单次最多导入 20 个文件");
        }
        ImportResult r = new ImportResult();
        for (MultipartFile f : files) {
            String filename = f.getOriginalFilename();
            try {
                ImportResult.Item item = importExportService.upsertFromBytes(f.getBytes(), filename);
                if (item.isUpdated()) r.setUpdated(r.getUpdated() + 1);
                else r.setCreated(r.getCreated() + 1);
                r.getItems().add(item);
            } catch (Exception e) {
                r.setFailed(r.getFailed() + 1);
                r.getErrors().add((filename == null ? "(unknown)" : filename) + ": " + e.getMessage());
            }
        }
        return R.ok(r);
    }

    @PostMapping
    @Operation(summary = "创建文章")
    public R<Article> create(@Valid @RequestBody ArticleDTO dto) {
        return R.ok(articleService.save(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文章")
    public R<Article> update(@PathVariable Long id, @Valid @RequestBody ArticleDTO dto) {
        dto.setId(id);
        return R.ok(articleService.update(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章")
    public R<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return R.ok();
    }

    @DeleteMapping
    @Operation(summary = "批量删除")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return R.ok();
        if (ids.size() > 100) throw new com.blog.common.BizException(400, "单次最多删除 100 篇");
        articleService.batchDelete(ids);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改状态 0草稿 1发布 2归档")
    public R<Void> updateStatus(@PathVariable Long id,
                                @RequestParam @Min(0) @Max(2) Integer status) {
        articleService.updateStatus(id, status);
        return R.ok();
    }

    @PutMapping("/{id}/top")
    @Operation(summary = "置顶 0否 1是")
    public R<Void> updateTop(@PathVariable Long id,
                             @RequestParam @Min(0) @Max(1) Integer top) {
        articleService.updateTop(id, top);
        return R.ok();
    }

    @PutMapping("/{id}/featured")
    @Operation(summary = "推荐 0否 1是")
    public R<Void> updateFeatured(@PathVariable Long id,
                                  @RequestParam @Min(0) @Max(1) Integer featured) {
        articleService.updateFeatured(id, featured);
        return R.ok();
    }

    @PutMapping("/{id}/view-count")
    @Operation(summary = "设置阅读量为指定值(article)")
    public R<Void> setViewCount(@PathVariable Long id,
                                @RequestParam @Min(0) @Max(100_000_000L) Long value) {
        articleService.setViewCount(id, value);
        return R.ok();
    }

    @PutMapping("/{id}/view-count/delta")
    @Operation(summary = "按增量调整阅读量(article),delta 可负,但结果不能 < 0")
    public R<Void> updateViewDelta(@PathVariable Long id,
                                   @RequestParam @Min(-1_000_000L) @Max(1_000_000L) Long delta) {
        articleService.incrViewBy(id, delta);
        return R.ok();
    }
}
