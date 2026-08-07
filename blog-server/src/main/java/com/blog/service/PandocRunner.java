package com.blog.service;

import com.blog.common.BizException;
import com.blog.common.PandocUnavailableException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 调用 pandoc 把 markdown → docx/pdf。
 * - 二进制路径由 ${BLOG_PANDOC_BIN} 控制,默认 /usr/bin/pandoc
 * - reference.docx 在 @PostConstruct 时从 classpath 复制到本地 template 目录,后续复用
 * - 找不到二进制 / ProcessBuilder.start() 抛 IOException → PandocUnavailableException(503)
 * - 其余非零退出 / 超时 → BizException(400)
 */
@Component
public class PandocRunner {

    private static final Logger log = LoggerFactory.getLogger(PandocRunner.class);

    private final String pandocBinConfig;
    private final String templateDirConfig;

    private Path referenceDocx;
    private Path fontsDir;       // 解包后的字体目录
    private String pdfEngine = "xelatex";   // 默认 + 启动时检测覆盖

    public PandocRunner(@Value("${blog.export.pandoc-bin:/usr/bin/pandoc}") String pandocBin,
                        @Value("${blog.export.template-dir:/tmp/blog-export-templates}") String templateDir) {
        this.pandocBinConfig = pandocBin;
        this.templateDirConfig = templateDir;
    }

    @PostConstruct
    void init() {
        try {
            Path dir = Paths.get(templateDirConfig);
            Files.createDirectories(dir);
            Path target = dir.resolve("blog-docx-reference.docx");
            if (!Files.exists(target)) {
                try (var in = getClass().getClassLoader().getResourceAsStream("templates/blog-docx-reference.docx")) {
                    if (in == null) {
                        log.info("classpath 无 templates/blog-docx-reference.docx,DOCX 导出将回退到 pandoc 内置默认");
                    } else {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                        log.info("解包 blog-docx-reference.docx → {}", target);
                    }
                }
            }
            referenceDocx = Files.exists(target) ? target.toAbsolutePath() : null;

            // 解包内嵌的中文字体到 fontsDir,生成 LaTeX preamble 引用文件路径
            fontsDir = dir.resolve("fonts");
            Files.createDirectories(fontsDir);
            String[] bundledFonts = { "NotoSerifSC-VF.ttf" };
            for (String name : bundledFonts) {
                Path target2 = fontsDir.resolve(name);
                if (!Files.exists(target2)) {
                    try (InputStream in = getClass().getClassLoader().getResourceAsStream("fonts/" + name)) {
                        if (in != null) {
                            Files.copy(in, target2, StandardCopyOption.REPLACE_EXISTING);
                            log.info("解包 {} → {}", name, target2);
                        } else {
                            log.warn("classpath 无 fonts/{}", name);
                        }
                    }
                }
            }
            // 生成 fontspec preamble,告诉 xelatex 从字体文件路径加载
            // 用文件名(不含扩展名)+ Path,不要写 Extension/font family name;
            // fontspec auto-detect by magic bytes,这套在 Windows MiKTeX + Linux TeXLive 都过
            // fontspec Path 字段会把 \ 当 TeX 转义,windows 路径必须转成正斜杠
            Path preamble = fontsDir.resolve("fontspec.tex");
            try {
                String fontsDirStr = fontsDir.toAbsolutePath().toString().replace('\\', '/');
                String content = "\\usepackage{fontspec}\n"
                        + "\\setmainfont{NotoSerifSC-VF.ttf}[\n"
                        + "  Path = " + fontsDirStr + "/ ,\n"
                        + "  UprightFont = * ,\n"
                        + "  BoldFont = * ,\n"
                        + "  ItalicFont = * ,\n"
                        + "  BoldItalicFont = *\n"
                        + "]\n";
                Files.writeString(preamble, content, StandardCharsets.UTF_8);
                log.info("生成 fontspec preamble → {}", preamble);
            } catch (IOException e) {
                log.warn("生成 fontspec preamble 失败: {}", e.getMessage());
            }

            // 探测 PDF engine 优先级:env override > xelatex > weasyprint > 兜底
            String envOverride = System.getenv("BLOG_PANDOC_PDF_ENGINE");
            if (envOverride != null && !envOverride.isBlank()) {
                pdfEngine = envOverride.trim();
            } else if (which("xelatex") == null) {
                if (which("weasyprint") != null) pdfEngine = "weasyprint";
                else pdfEngine = "";   // 让 pandoc 用默认
            }
            log.info("Pandoc 初始化完成,PDF engine = {}", pdfEngine.isEmpty() ? "<default>" : pdfEngine);
        } catch (IOException e) {
            log.warn("PandocRunner 初始化失败(非致命): {}", e.getMessage());
        }
    }

    /** 把 markdown 转成目标格式字节流。MD 走 JDK 直接编码,字节相等。 */
    public byte[] convert(String markdown, ExportFormat fmt) {
        if (fmt == ExportFormat.MD) {
            return markdown.getBytes(StandardCharsets.UTF_8);
        }
        String bin = resolveBinary();
        if (bin == null) {
            throw new PandocUnavailableException(
                    "Pandoc 未找到(配置 blog.export.pandoc-bin=" + pandocBinConfig
                            + ")。请在服务器安装 pandoc 或设置 BLOG_PANDOC_BIN 环境变量。");
        }

        Path tmp = null;
        try {
            tmp = Files.createTempDirectory("blog-export-");
            Path in = tmp.resolve("in.md");
            Path out = tmp.resolve("out." + fmt.extension());
            Files.writeString(in, markdown, StandardCharsets.UTF_8);

            List<String> cmd = new ArrayList<>();
            cmd.add(bin);
            cmd.add(in.toString());
            cmd.add("-o");
            cmd.add(out.toString());
            cmd.add("--from=markdown+yaml_metadata_block+pipe_tables+strikeout+task_lists");

            if (fmt == ExportFormat.DOCX && referenceDocx != null) {
                cmd.add("--reference-doc=" + referenceDocx);
            }
            if (fmt == ExportFormat.PDF) {
                if (!pdfEngine.isEmpty()) {
                    cmd.add("--pdf-engine=" + pdfEngine);
                }
                cmd.addAll(Arrays.asList(
                        "-V", "geometry:margin=1in",
                        "-V", "lang=zh-CN"
                ));
                // 显式指定字体文件路径 + include-in-header,让 fontspec 从 classpath 解包的字体里取
                if (fontsDir != null) {
                    Path preamble = fontsDir.resolve("fontspec.tex");
                    if (Files.exists(preamble)) {
                        cmd.add("--include-in-header=" + preamble.toAbsolutePath());
                    }
                }
            }

            ProcessBuilder pb = new ProcessBuilder(cmd)
                    .directory(tmp.toFile())
                    .redirectErrorStream(true);
            Process p;
            try {
                p = pb.start();
            } catch (IOException ioe) {
                throw new PandocUnavailableException(
                        "Pandoc 二进制无法执行(" + bin + "): " + ioe.getMessage());
            }
            p.getOutputStream().close();

            // 异步 read,避免填满 pipe buffer 死锁
            byte[] outBytes;
            try (var stdout = p.getInputStream()) {
                outBytes = stdout.readAllBytes();
            }
            boolean finished = p.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new BizException("pandoc 转换超时(>30s)");
            }
            if (p.exitValue() != 0) {
                // stderr 写到服务端日志,对外只暴露简短原因(可能含绝对路径)
                log.warn("pandoc convert 失败(exit={}): {}", p.exitValue(),
                        new String(outBytes, StandardCharsets.UTF_8));
                throw new BizException("pandoc 转换失败");
            }
            if (!Files.exists(out)) {
                throw new BizException("pandoc 退出成功但未生成产物文件");
            }
            return Files.readAllBytes(out);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("pandoc 转换异常", e);
            throw new BizException("pandoc 转换失败");
        } finally {
            if (tmp != null) bestEffortDelete(tmp);
        }
    }

    /**
     * 把 .docx 字节流转成 markdown 字符串(走 pandoc -f docx -t markdown)。
     * 仅用于导入场景的预处理;失败抛 BizException(400)。
     */
    public String docxToMarkdown(byte[] docxBytes) {
        if (docxBytes == null || docxBytes.length == 0) {
            throw new BizException("docx 字节流为空");
        }
        String bin = resolveBinary();
        if (bin == null) {
            throw new PandocUnavailableException(
                    "Pandoc 未找到,无法解析 docx。请设置 BLOG_PANDOC_BIN。");
        }
        Path tmp = null;
        try {
            tmp = Files.createTempDirectory("blog-docx-import-");
            Path in = tmp.resolve("in.docx");
            Path out = tmp.resolve("out.md");
            Files.write(in, docxBytes);

            List<String> cmd = new ArrayList<>();
            cmd.add(bin);
            cmd.add(in.toString());
            cmd.add("-o");
            cmd.add(out.toString());
            cmd.add("--from=docx");
            cmd.add("--to=markdown+yaml_metadata_block");

            ProcessBuilder pb = new ProcessBuilder(cmd)
                    .directory(tmp.toFile())
                    .redirectErrorStream(true);
            Process p;
            try {
                p = pb.start();
            } catch (IOException ioe) {
                throw new PandocUnavailableException(
                        "Pandoc 二进制无法执行(" + bin + "): " + ioe.getMessage());
            }
            p.getOutputStream().close();
            byte[] outBytes;
            try (var stdout = p.getInputStream()) {
                outBytes = stdout.readAllBytes();
            }
            boolean finished = p.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new BizException("pandoc docx→md 转换超时(>30s)");
            }
            if (p.exitValue() != 0) {
                log.warn("pandoc docx→md 失败(exit={}): {}", p.exitValue(),
                        new String(outBytes, StandardCharsets.UTF_8));
                throw new BizException("pandoc docx→md 转换失败");
            }
            if (!Files.exists(out)) {
                throw new BizException("pandoc docx→md 退出成功但未生成产物文件");
            }
            return Files.readString(out, StandardCharsets.UTF_8);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("pandoc docx→md 异常", e);
            throw new BizException("pandoc docx→md 转换失败");
        } finally {
            if (tmp != null) bestEffortDelete(tmp);
        }
    }

    private static void bestEffortDelete(Path dir) {
        if (dir == null) return;
        try {
            Files.walk(dir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignore) {} });
        } catch (IOException ignore) {}
    }

    /** 探测 PATH 上的命令;命中返回绝对路径,否则 null */
    private static String which(String name) {
        String path = System.getenv("PATH");
        if (path == null) return null;
        for (String dir : path.split(java.io.File.pathSeparator)) {
            try {
                Path p = Paths.get(dir, name);
                if (Files.isExecutable(p)) return p.toAbsolutePath().toString();
            } catch (Exception ignore) {}
        }
        return null;
    }

    /** 解析二进制路径:env override > 配置值 > PATH 中的 pandoc。命中返回绝对路径,否则 null。 */
    private String resolveBinary() {
        String envOverride = System.getenv("BLOG_PANDOC_BIN");
        String[] candidates = envOverride != null && !envOverride.isBlank()
                ? new String[]{ envOverride.trim(), pandocBinConfig, "/usr/bin/pandoc", "/usr/local/bin/pandoc" }
                : new String[]{ pandocBinConfig, "/usr/bin/pandoc", "/usr/local/bin/pandoc" };
        for (String c : candidates) {
            try {
                Path p = Paths.get(c);
                // Windows 上 Files.isExecutable 对带空格/UNC 路径判定不准,
                // 改为"文件存在即可"——路径是用户显式配的,可信度高
                if (Files.exists(p)) return p.toAbsolutePath().toString();
            } catch (Exception ignore) {}
        }
        String pathHit = which("pandoc");
        return pathHit;
    }
}
