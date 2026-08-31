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
import java.util.List;

/** Converts documents only in a read-only, network-isolated, resource-limited Docker container. */
@Component
public class PandocRunner {

    private static final Logger log = LoggerFactory.getLogger(PandocRunner.class);

    private final String templateDirConfig;

    @Value("${blog.export.docker-bin:docker}")
    private String dockerBin = "docker";
    @Value("${blog.export.container-image:pandoc/latex:3.6.4}")
    private String containerImage = "pandoc/latex:3.6.4";
    private static final java.util.concurrent.Semaphore CONVERSIONS = new java.util.concurrent.Semaphore(2);
    private Path referenceDocx;
    private Path fontsDir;       // 解包后的字体目录

    public PandocRunner(@Value("${blog.export.template-dir:/tmp/blog-export-templates}") String templateDir) {
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
                        log.info("已解包 blog-docx-reference.docx");
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
                            log.info("已解包字体资源 {}", name);
                        } else {
                            log.warn("classpath 无 fonts/{}", name);
                        }
                    }
                }
            }
            // 生成 fontspec preamble,告诉 xelatex 从字体文件路径加载
            // 用文件名(不含扩展名)+ Path,不要写 Extension/font family name;
            // 字体通过只读挂载提供，preamble 仅使用容器内路径。
            Path preamble = fontsDir.resolve("fontspec.tex");
            try {
                String fontsDirStr = "/fonts";
                String content = "\\usepackage{fontspec}\n"
                        + "\\setmainfont{NotoSerifSC-VF.ttf}[\n"
                        + "  Path = " + fontsDirStr + "/ ,\n"
                        + "  UprightFont = * ,\n"
                        + "  BoldFont = * ,\n"
                        + "  ItalicFont = * ,\n"
                        + "  BoldItalicFont = *\n"
                        + "]\n";
                Files.writeString(preamble, content, StandardCharsets.UTF_8);
                log.info("已生成 fontspec preamble");
            } catch (IOException e) {
                log.warn("生成 fontspec preamble 失败: {}", e.getClass().getSimpleName());
            }


        } catch (IOException e) {
            log.warn("PandocRunner 初始化失败（非致命）: {}", e.getClass().getSimpleName());
        }
    }

    /** Markdown export is local; document converters never inherit the application's filesystem or network. */
    public byte[] convert(String markdown, ExportFormat format) {
        if (markdown == null || markdown.length() > 250_000) throw new BizException("文档正文过大或为空");
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);
        if (format == ExportFormat.MD) return bytes;
        return convertInContainer(bytes, "in.md", "out." + format.extension(), format);
    }

    public String docxToMarkdown(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > 5 * 1024 * 1024) throw new BizException("DOCX 文件不能为空或超过 5MB");
        return new String(convertInContainer(bytes, "in.docx", "out.md", ExportFormat.MD), StandardCharsets.UTF_8);
    }

    private byte[] convertInContainer(byte[] bytes, String inputName, String outputName, ExportFormat format) {
        if (!CONVERSIONS.tryAcquire()) throw new BizException(429, "转换任务繁忙，请稍后重试");
        Path temp = null;
        String container = "blog-convert-" + java.util.UUID.randomUUID();
        try {
            temp = Files.createTempDirectory("blog-convert-").toAbsolutePath().normalize();
            // This unique, disposable directory is the only writable host mount.
            try { Files.setPosixFilePermissions(temp, java.nio.file.attribute.PosixFilePermissions.fromString("rwxrwxrwx")); }
            catch (UnsupportedOperationException ignored) { /* Windows uses its directory ACL. */ }
            Path input = temp.resolve(inputName);
            Files.write(input, bytes);
            try { Files.setPosixFilePermissions(input, java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--")); }
            catch (UnsupportedOperationException ignored) { }

            List<String> cmd = containerCommand(temp, container);
            cmd.add(containerImage);
            cmd.add("/work/" + inputName);
            cmd.add("-o");
            cmd.add("/work/" + outputName);
            cmd.add("--sandbox");
            if (inputName.endsWith(".docx")) {
                cmd.add("--from=docx");
                cmd.add("--to=markdown+yaml_metadata_block");
            } else {
                cmd.add("--from=markdown-raw_html-raw_tex+pipe_tables+strikeout+task_lists");
                if (format == ExportFormat.DOCX && referenceDocx != null) cmd.add("--reference-doc=/reference.docx");
                if (format == ExportFormat.PDF) {
                    cmd.add("--pdf-engine=xelatex");
                    cmd.add("-V"); cmd.add("geometry:margin=1in");
                    if (fontsDir != null && Files.exists(fontsDir.resolve("NotoSerifSC-VF.ttf"))) {
                        cmd.add("--include-in-header=/fonts/fontspec.tex");
                    }
                }
            }
            BoundedProcessRunner.run(new ProcessBuilder(cmd), java.time.Duration.ofSeconds(30), 256 * 1024);
            Path output = temp.resolve(outputName);
            if (!Files.isRegularFile(output, java.nio.file.LinkOption.NOFOLLOW_LINKS) || Files.size(output) > 20 * 1024 * 1024) throw new BizException("转换产物缺失或超过 20MB");
            return Files.readAllBytes(output);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("文档转换已取消");
        } catch (IOException ex) {
            throw new PandocUnavailableException("隔离转换容器不可用，请配置 Docker 并预先拉取转换镜像");
        } finally {
            // Killing only the docker CLI would leave its container running.
            try { BoundedProcessRunner.run(new ProcessBuilder(dockerBin, "rm", "-f", container), java.time.Duration.ofSeconds(5), 8192); }
            catch (Exception ignored) { log.debug("转换容器已退出或 Docker 不可用"); }
            if (temp != null) bestEffortDelete(temp);
            CONVERSIONS.release();
        }
    }

    List<String> containerCommand(Path temp, String name) {
        List<String> command = new ArrayList<>(List.of(dockerBin, "run", "--rm", "--pull=never", "--name", name,
                "--network=none", "--read-only", "--cap-drop=ALL", "--security-opt=no-new-privileges",
                "--memory=512m", "--cpus=1", "--pids-limit=64", "--ulimit=fsize=20971520:20971520", "--user=65534:65534",
                "--tmpfs=/tmp:rw,nosuid,nodev,size=64m", "--workdir=/work",
                "--mount", "type=bind,source=" + temp + ",target=/work"));
        if (referenceDocx != null) {
            command.add("--mount"); command.add("type=bind,source=" + referenceDocx + ",target=/reference.docx,readonly");
        }
        if (fontsDir != null) {
            command.add("--mount"); command.add("type=bind,source=" + fontsDir.toAbsolutePath() + ",target=/fonts,readonly");
        }
        return command;
    }

    private static void bestEffortDelete(Path directory) {
        Path root = directory.toAbsolutePath().normalize();
        Path tempRoot = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        if (!root.startsWith(tempRoot) || root.equals(tempRoot)) throw new IllegalArgumentException("Unsafe temporary path");
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ex) { log.warn("转换临时文件清理失败: {}", ex.getClass().getSimpleName()); }
            });
        } catch (IOException ex) { log.warn("转换临时目录清理失败: {}", ex.getClass().getSimpleName()); }
    }

}
