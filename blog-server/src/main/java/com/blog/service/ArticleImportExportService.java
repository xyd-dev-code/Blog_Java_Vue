package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BizException;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ImportResult;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.TagMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文章导入导出服务:
 * - 导出:把 Article 序列化为 YAML front matter + markdown 正文;支持单篇 .md / 全部 zip
 * - 导入:解析 .md → upsert,按 slug 去重;不存在分类/标签按名自动创建
 */
@Service
public class ArticleImportExportService {

    private final ArticleMapper articleMapper;
    private final ArticleService articleService;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PandocRunner pandocRunner;
    private final Yaml yaml = new Yaml();

    public ArticleImportExportService(ArticleMapper articleMapper,
                                      ArticleService articleService,
                                      CategoryMapper categoryMapper,
                                      TagMapper tagMapper,
                                      PandocRunner pandocRunner) {
        this.articleMapper = articleMapper;
        this.articleService = articleService;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.pandocRunner = pandocRunner;
    }

    // ====================== 导出 ======================

    /** 单篇文章 → 带 YAML front matter 的 markdown 字符串 */
    public String exportOneAsString(Long id) {
        Article a = articleService.detailById(id);
        Map<String, Object> fm = buildFrontMatter(a);
        String yamlStr = yaml.dump(fm);
        String body = a.getContent() == null ? "" : a.getContent();
        if (!body.startsWith("\n")) body = "\n" + body;
        return "---\n" + yamlStr + "---\n" + body;
    }

    /** 默认导出为 .md(字节相等保留旧行为) */
    public byte[] exportOne(Long id) {
        return exportOne(id, ExportFormat.MD);
    }

    /** 按格式导出单篇文章。MD 走 JDK,其它走 pandoc */
    public byte[] exportOne(Long id, ExportFormat fmt) {
        ExportFormat f = fmt == null ? ExportFormat.MD : fmt;
        return pandocRunner.convert(exportOneAsString(id), f);
    }

    /** 默认 zip(.md) */
    public byte[] exportAllAsZip() throws IOException {
        return exportAllAsZip(ExportFormat.MD);
    }

    /** 全部文章打包成 zip,文件名 {slug}.{ext} 或 article-{id}.{ext} */
    public byte[] exportAllAsZip(ExportFormat fmt) throws IOException {
        ExportFormat f = fmt == null ? ExportFormat.MD : fmt;
        List<Article> all = articleMapper.selectList(null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (Article a : all) {
                Long id = a.getId();
                String slug = a.getSlug();
                String base = StringUtils.hasText(slug) ? slug : ("article-" + id);
                String entryName = base + "." + f.extension();
                byte[] content = exportOne(id, f);
                ZipEntry entry = new ZipEntry(entryName);
                entry.setSize(content.length);
                zos.putNextEntry(entry);
                zos.write(content);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    /** 解析外部 format 字符串(空 → MD) */
    public static ExportFormat parseFormat(String s) {
        return ExportFormat.parse(s, ExportFormat.MD);
    }

    /** 导入对话框下载的 markdown 模板(含 front matter 注释) */
    public byte[] getMarkdownTemplate() {
        return MARKDOWN_TEMPLATE.getBytes(StandardCharsets.UTF_8);
    }

    /** 导入对话框下载的 .docx 模板 — 直接打成 jar 的静态资源(我手工构建的 OpenXML,不依赖 pandoc) */
    public byte[] getDocxTemplate() {
        try (var in = getClass().getClassLoader().getResourceAsStream("templates/blog-article-template.docx")) {
            if (in == null) {
                throw new BizException("templates/blog-article-template.docx 未打进 jar");
            }
            return in.readAllBytes();
        } catch (java.io.IOException e) {
            throw new BizException("读取 docx 模板失败: " + e.getMessage());
        }
    }

    private static final String MARKDOWN_TEMPLATE = """
---
title: 我的新文章
slug: my-new-article
category: 技术
tags: Java, Vue
cover: https://img.example.com/cover.jpg
summary: 一句话简介
status: 1
top: 0
featured: 0
allowComment: 1
---

# 正文从这里开始

支持 **Markdown** / GFM 表格 / 代码块 / 任务列表。

```java
System.out.println("hello, blog");
```

> 提示:导入时若 `slug` 命中已有文章,则更新该篇;否则视为新建。
""";

    private Map<String, Object> buildFrontMatter(Article a) {
        Map<String, Object> fm = new LinkedHashMap<>();
        fm.put("title", a.getTitle());
        fm.put("slug", a.getSlug());
        fm.put("category", a.getCategoryName());
        List<String> tagNames = new ArrayList<>();
        if (a.getTags() != null) for (Tag t : a.getTags()) tagNames.add(t.getName());
        fm.put("tags", tagNames);
        fm.put("cover", a.getCoverImage());
        fm.put("summary", a.getSummary());
        fm.put("status", a.getStatus());
        fm.put("top", a.getIsTop());
        fm.put("featured", a.getIsFeatured());
        fm.put("allowComment", a.getAllowComment());
        return fm;
    }

    // ====================== 导入 ======================

    /** 解析结果:包含解析后的 DTO、是否已存在(slug 命中)、原始文件名前缀等 */
    public static class ParseResult {
        public ArticleDTO dto;
        public boolean existed;        // slug 命中已有文章
        public Long existingId;        // 命中的 id(null 表示将新建)
        public String frontMatterRaw;
        public String bodyRaw;
        public String sourceFilename;
        // 已在 parseOne 里解析好的 front matter Map(供 upsert/preview 直接复用,避免重复 yaml.load)
        public Map<String, Object> parsedFrontMatter = Collections.emptyMap();
    }

    /**
     * 解析一个 .md 文件为 ParseResult(不写库)。
     * 返回的 dto.tagIds 永远是 null —— upsert 阶段按 name 解析为 ID。
     */
    @SuppressWarnings("unchecked")
    public ParseResult parseOne(byte[] content, String filename) {
        ParseResult r = new ParseResult();
        r.sourceFilename = filename;
        String text = new String(content, StandardCharsets.UTF_8);
        // 标准化换行
        text = text.replace("\r\n", "\n").replace("\r", "\n");

        String body = text;
        Map<String, Object> fm = Collections.emptyMap();
        String fmRaw = "";

        // 1) 优先:首行就是 --- 的标准 YAML 围栏
        if (text.startsWith("---\n") || text.startsWith("---\r\n")) {
            int firstNewline = text.indexOf('\n');
            int secondDash = text.indexOf("\n---", firstNewline + 1);
            if (secondDash > 0) {
                int fmEnd = secondDash;
                fmRaw = text.substring(firstNewline + 1, fmEnd);
                body = text.substring(secondDash + 4);
                if (body.startsWith("\n")) body = body.substring(1);
                try {
                    Object parsed = yaml.load(fmRaw);
                    if (parsed instanceof Map) fm = (Map<String, Object>) parsed;
                } catch (Exception e) {
                    throw new BizException("YAML 解析失败: " + e.getMessage());
                }
            }
        } else {
            // 2) docx→md 后是连续 key: value 段落(无 --- 围栏),也按 front matter 处理。
            //    规则:从文件开头往下取连续的 key: value 行(包括多行数组的延续行),
            //    直到遇到空行/非 key: 起始的行,前面是 front matter。
            //    docx 加粗 key 在 md 里会渲染成 "**key:** value" — 这里兼容。
            int fmEnd = scanLooseFrontMatter(text);
            if (fmEnd > 0) {
                fmRaw = text.substring(0, fmEnd);
                body = text.substring(fmEnd);
                while (body.startsWith("\n")) body = body.substring(1);
                // 喂给 yaml 之前,剥掉加粗标记 **,这样 "**title:** xxx" 也能解析
                String fmForYaml = fmRaw.replaceAll("\\*\\*", "");
                // 关键:yaml 把 # / > 开头的行当注释 — 必须把装饰段剥掉,只留真正的 key:value
                StringBuilder fmClean = new StringBuilder();
                for (String line : fmForYaml.split("\n")) {
                    String t = line.trim();
                    if (t.startsWith("#") || t.startsWith(">") || t.isEmpty()) continue;
                    fmClean.append(line).append('\n');
                }
                fmForYaml = fmClean.toString();
                try {
                    Object parsed = yaml.load(fmForYaml);
                    if (parsed instanceof Map) fm = (Map<String, Object>) parsed;
                } catch (Exception e) {
                    // 解析失败不要整个挂掉,把它当成纯正文
                    fm = Collections.emptyMap();
                    body = text;
                    fmRaw = "";
                }
            }
        }

        r.frontMatterRaw = fmRaw;
        r.bodyRaw = body;
        r.parsedFrontMatter = fm;

        ArticleDTO dto = new ArticleDTO();
        String title = str(fm.get("title"));
        if (!StringUtils.hasText(title)) {
            // 无 title 时,用文件名(去掉 .md)兜底
            title = filename == null ? "未命名文章" : filename.replaceAll("\\.md$", "");
        }
        dto.setTitle(title);
        dto.setSlug(str(fm.get("slug")));
        dto.setContent(body);
        dto.setSummary(str(fm.get("summary")));
        dto.setCoverImage(str(fm.get("cover")));
        dto.setStatus(toInt(fm.get("status"), 1));
        dto.setIsTop(toInt(fm.get("top"), 0));
        dto.setIsFeatured(toInt(fm.get("featured"), 0));
        dto.setAllowComment(toInt(fm.get("allowComment"), 1));

        // 暂存 name 列表,upsert 时再转 ID
        r.dto = dto;
        // 这里先按 slug 探测一次(existed 标记用于预览)
        if (StringUtils.hasText(dto.getSlug())) {
            Article exist = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                    .eq(Article::getSlug, dto.getSlug()));
            if (exist != null) {
                r.existed = true;
                r.existingId = exist.getId();
            }
        }
        return r;
    }

    /**
     * 把 parse 结果落库。同 slug → update;否则 insert。
     * categoryName / tagNames 需通过额外 Map 传入,此方法不重做 front matter 解析。
     */
    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public Article upsertWithFrontMatter(ParseResult r, Map<String, Object> fm) {
        ArticleDTO dto = r.dto;
        // category name -> id(自动创建)
        Object catObj = fm == null ? null : fm.get("category");
        if (catObj != null && StringUtils.hasText(catObj.toString().trim())) {
            Long cid = findOrCreateCategory(catObj.toString().trim());
            dto.setCategoryId(cid);
        }
        // tags names -> ids
        Object tagsObj = fm == null ? null : fm.get("tags");
        List<String> tagNames = new ArrayList<>();
        if (tagsObj instanceof List) {
            for (Object n : (List<?>) tagsObj) {
                if (n == null) continue;
                String name = n.toString().trim();
                if (!StringUtils.hasText(name)) continue;
                tagNames.add(name);
            }
        } else if (tagsObj != null) {
            // 单字符串(逗号/顿号/分号/空白分隔)— 用户友好:docx 里通常写成 "Java, Vue, 测试"
            for (String s : tagsObj.toString().split("[,,,;\\s]+")) {
                String name = s.trim();
                if (!StringUtils.hasText(name)) continue;
                tagNames.add(name);
            }
        }
        if (!tagNames.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (String name : tagNames) ids.add(findOrCreateTag(name));
            dto.setTagIds(ids);
        }

        if (!StringUtils.hasText(dto.getSlug())) {
            dto.setSlug(toSlug(dto.getTitle()));
        }

        Article exist = StringUtils.hasText(dto.getSlug())
                ? articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                    .eq(Article::getSlug, dto.getSlug()))
                : null;
        if (exist != null) {
            dto.setId(exist.getId());
            return articleService.update(dto);
        } else {
            return articleService.save(dto);
        }
    }

    /**
     * 预处理:如果是 .docx,优先交给 PandocRunner.docxToMarkdown() 渲染成结构化 markdown
     * (自带 YAML front matter 围栏),再交给 parseOne 解析。
     * 若 pandoc 不可用,PandocUnavailableException 会原样抛出(已在 GlobalExceptionHandler
     * 映射为 503),让用户得到明确提示 — 这比静默吞掉正文要好。
     */
    private Object[] prepareBytesForParsing(byte[] content, String filename) {
        if (filename != null && filename.toLowerCase().endsWith(".docx")) {
            String md = pandocRunner.docxToMarkdown(content);
            String newName = filename.replaceAll("\\.docx$", ".md");
            return new Object[] { md.getBytes(StandardCharsets.UTF_8), newName };
        }
        return new Object[] { content, filename };
    }

    /** docx 段落抽取结果:frontMatterRaw 是 key:value 段;bodyRaw 是剩余正文,前面拼个空行隔开 */
    @Deprecated
    static class ParsedDocx {
        String frontMatterRaw = "";
        String bodyRaw = "";
    }

    /**
     * 兜底方法:把 .docx 解压,只读 word/document.xml,把每个 <w:p> 转成一行纯文本。
     * 然后按"前若干个 key:value 行为 front matter,其余是 body"切分。
     *
     * 已被 pandoc 路径取代:该方法会把短正文(< 24 字无标点)误判为"装饰章节标题"吞掉,
     * 也无法识别图片 / 表格 / 样式。保留仅作 pandoc 完全不可用时的最后手段,
     * 正式 docx 导入请用 PandocRunner.docxToMarkdown。
     */
    @Deprecated
    private static ParsedDocx parseDocxParagraphs(byte[] docxBytes) {
        ParsedDocx out = new ParsedDocx();
        if (docxBytes == null || docxBytes.length == 0) return out;
        try (var zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(docxBytes))) {
            java.util.zip.ZipEntry e;
            byte[] documentXml = null;
            while ((e = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(e.getName())) {
                    documentXml = zis.readAllBytes();
                    break;
                }
            }
            if (documentXml == null) return out;
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(new java.io.ByteArrayInputStream(documentXml));
            org.w3c.dom.NodeList paragraphs = doc.getElementsByTagName("w:p");
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (int i = 0; i < paragraphs.getLength(); i++) {
                StringBuilder sb = new StringBuilder();
                org.w3c.dom.NodeList runs = paragraphs.item(i).getChildNodes();
                for (int j = 0; j < runs.getLength(); j++) {
                    org.w3c.dom.Node n = runs.item(j);
                    if ("w:r".equals(n.getNodeName())) {
                        org.w3c.dom.NodeList t = n.getChildNodes();
                        for (int k = 0; k < t.getLength(); k++) {
                            if ("w:t".equals(t.item(k).getNodeName())) {
                                sb.append(t.item(k).getTextContent());
                            }
                        }
                    } else if ("w:br".equals(n.getNodeName())) {
                        sb.append('\n');
                    }
                }
                lines.add(sb.toString());
            }

            // 切 front matter / body
            // 算法:从前往后扫,
            //   - 空白行 → 跳过(不终止 front matter)
            //   - key: value → 收集
            //   - 列表项 → 收集(限前几行)
            //   - "短章节标题"(≤ 30 字,无句号/冒号)→ 跳过(视为装饰)
            //   - 其它(明显正文)→ 终止 front matter,从此行起开始 body
            StringBuilder fm = new StringBuilder();
            StringBuilder body = new StringBuilder();
            boolean pastFrontMatter = false;
            int consecListItems = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (!pastFrontMatter) {
                    if (trimmed.isEmpty()) {
                        // 还没 front matter 就遇空行:跳过
                        continue;
                    }
                    if (trimmed.matches("^[A-Za-z_][\\w-]*\\s*:.*")) {
                        // 进入 front matter
                        fm.append(trimmed).append('\n');
                        consecListItems = 0;
                        continue;
                    }
                    if (trimmed.matches("^[-*][\\s\\S].*") || trimmed.matches("^\\d+\\.[\\s\\S].*")) {
                        // 列表项续 front matter(只在前几行)
                        if (consecListItems++ < 8) {
                            fm.append(trimmed).append('\n');
                            continue;
                        }
                    }
                    // 短章节标题(如"文章元数据"、"正文"、"Metadata"):< 24 字、无句末标点
                    if (trimmed.length() < 24
                            && !trimmed.matches(".*[.!?。！？;；,].*")
                            && !trimmed.contains(":")) {
                        continue; // 跳过,继续往下找 key:value
                    }
                    // 其它行:front matter 结束,从这里起开始 body
                    pastFrontMatter = true;
                }
                body.append(line).append('\n');
            }
            out.frontMatterRaw = fm.toString().trim();
            out.bodyRaw = body.toString().trim();
        } catch (Exception ex) {
            // 解析失败直接当成空,留给上一层兜底
        }
        return out;
    }

    /** 单文件批量导入入口(供 controller 直接调用) */
    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public ImportResult.Item upsertFromBytes(byte[] content, String filename) {
        Object[] prep = prepareBytesForParsing(content, filename);
        ParseResult r = parseOne((byte[]) prep[0], (String) prep[1]);
        // 复用 parseOne 里已解析好的 front matter,避免再 yaml.load 一次(否则包含 #/装饰段的原始文本会再次解析失败)
        Map<String, Object> fm = r.parsedFrontMatter == null ? Collections.emptyMap() : r.parsedFrontMatter;
        Article saved = upsertWithFrontMatter(r, fm);
        boolean updated = r.existed;
        return new ImportResult.Item(filename, saved.getTitle(), saved.getId(), updated);
    }

    /** 供 controller 解析预览(不写库)—— 给前端弹窗展示 */
    public ParsePreview preview(byte[] content, String filename) {
        Object[] prep = prepareBytesForParsing(content, filename);
        ParseResult r = parseOne((byte[]) prep[0], (String) prep[1]);
        Map<String, Object> fm = r.parsedFrontMatter == null ? Collections.emptyMap() : r.parsedFrontMatter;
        ParsePreview p = new ParsePreview();
        p.title = r.dto.getTitle();
        p.slug = r.dto.getSlug();
        p.category = fm.get("category") == null ? null : fm.get("category").toString();
        p.tags = new ArrayList<>();
        Object t = fm.get("tags");
        if (t instanceof List) {
            for (Object x : (List<?>) t) if (x != null) p.tags.add(x.toString());
        } else if (t != null) {
            for (String s : t.toString().split("[,,,;\\s]+")) {
                String name = s.trim();
                if (StringUtils.hasText(name)) p.tags.add(name);
            }
        }
        p.status = r.dto.getStatus();
        p.top = r.dto.getIsTop();
        p.featured = r.dto.getIsFeatured();
        p.allowComment = r.dto.getAllowComment();
        p.bodyLength = r.bodyRaw == null ? 0 : r.bodyRaw.length();
        p.existed = r.existed;
        p.existingId = r.existingId;
        p.sourceFilename = filename;
        return p;
    }

    public static class ParsePreview {
        public String title;
        public String slug;
        public String category;
        public List<String> tags = new ArrayList<>();
        public Integer status;
        public Integer top;
        public Integer featured;
        public Integer allowComment;
        public int bodyLength;
        public boolean existed;
        public Long existingId;
        public String sourceFilename;
    }

    // ====================== 工具方法 ======================

    private Long findOrCreateCategory(String name) {
        Category c = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name));
        if (c != null) return c.getId();
        c = new Category();
        c.setName(name);
        c.setSlug(slugify(name));
        categoryMapper.insert(c);
        return c.getId();
    }

    private Long findOrCreateTag(String name) {
        Tag t = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, name));
        if (t != null) return t.getId();
        t = new Tag();
        t.setName(name);
        t.setSlug(slugify(name));
        tagMapper.insert(t);
        return t.getId();
    }

    private String slugify(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^\\u4e00-\\u9fa5a-z0-9]+", "-");
    }

    /**
     * 从文本开头扫描"宽松 front matter"(适用于 docx→md 后无 --- 围栏的情况)。
     * 命中规则:
     *   - 跳过开头的"装饰段落":空行、Markdown 标题(以 # 开头)、引用块(以 > 开头)、列表项
     *   - 第一个 key: value 行就是 front matter 的起点
     *   - 接下来连续匹配 key: value,或列表项 - item /  * item / 数字. item (indent 0~2 空格)
     *   - 直到第一个非空、非上述模式的行结束,返回该位置(绝对偏移)
     * 失败:返回 0(整篇当 body,标题用文件名兜底)。
     */
    private static int scanLooseFrontMatter(String text) {
        if (text == null || text.isEmpty()) return 0;

        // 1) 跳过开头的装饰段落,直到遇到第一个 key:value 行
        int pos = 0;
        int nl = -1;
        while (pos < text.length()) {
            nl = text.indexOf('\n', pos);
            String line = nl < 0 ? text.substring(pos) : text.substring(pos, nl);
            String trimmed = line.trim();
            // 跳过:空行 / markdown 标题 / 引用块 / 列表项
            // 列表项必须是 "- " / "* " / "1. " 后跟空格(避免吞掉 markdown 加粗 "**foo**")
            if (trimmed.isEmpty()
                    || trimmed.startsWith("#")
                    || trimmed.startsWith(">")
                    || trimmed.matches("^[ ]{0,2}(-|\\d+\\.) .*")
                    || (trimmed.startsWith("* ") && !trimmed.startsWith("**"))) {
                pos = nl < 0 ? text.length() : nl + 1;
                continue;
            }
            // 第一个"非装饰"行:它必须是 key:value 形式,否则放弃
            if (!isFrontMatterLine(trimmed)) return 0;
            break;
        }
        if (pos >= text.length()) return 0;

        // 2) 从 pos 开始,连续 key:value / 列表项 段;空行可能是 key 之间的间隔,也允许。
        //    终止条件:碰到 markdown 标题(以 # 开头)或其他非 fm 行,返回该位置
        while (pos < text.length()) {
            int eol = text.indexOf('\n', pos);
            String line = nl < 0 ? text.substring(pos) : text.substring(pos, eol);
            String trimmed = line.trim();
            // 空行:可能是 fm 段之间的间隔,继续往下扫
            if (trimmed.isEmpty()) {
                pos = eol < 0 ? text.length() : eol + 1;
                continue;
            }
            // key: value(可被加粗)
            if (isFrontMatterLine(trimmed)) {
                pos = eol < 0 ? text.length() : eol + 1;
                continue;
            }
            // 列表项:也是 fm 的一部分(YAML 数组延续)
            if (trimmed.matches("^[ ]{0,2}(-|\\d+\\.) .*")
                    || (trimmed.startsWith("* ") && !trimmed.startsWith("**"))) {
                pos = eol < 0 ? text.length() : eol + 1;
                continue;
            }
            // 碰到 markdown 标题(正文一级标题)或其他行 → fm 结束
            return pos;
        }
        return pos;
    }

    /**
     * 宽松 front matter 行判定:支持
     *   - 纯文本: `title: 我的文章`
     *   - 加粗 key+冒号(docx 加粗整段 → md 表现): `**title:** 我的文章`
     * 后续会剥掉 `**` 再喂 yaml.load。
     */
    private static boolean isFrontMatterLine(String line) {
        if (line == null) return false;
        if (line.matches("^\\*\\*[A-Za-z_][\\w-]*:\\*\\*.*")) return true;
        return line.matches("^[A-Za-z_][\\w-]*\\s*:.*");
    }

    private String toSlug(String title) {
        if (title == null) return "";
        String s = title.toLowerCase().replaceAll("[^\\u4e00-\\u9fa5a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return (s.length() > 50 ? s.substring(0, 50) : s) + "-" + System.currentTimeMillis();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Integer toInt(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (Exception e) {
            return def;
        }
    }
}