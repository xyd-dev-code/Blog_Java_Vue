package com.blog.service;

import com.blog.mapper.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ImportParsingRegressionTest {
    private final ArticleImportExportService service = new ArticleImportExportService(
            mock(ArticleMapper.class),mock(ArticleService.class),mock(CategoryMapper.class),mock(TagMapper.class),mock(PandocRunner.class));
    @Test void looseFrontMatterAcceptsMissingFinalNewline() {
        var parsed=service.parseOne("title: demo\nbody with no final newline".getBytes(StandardCharsets.UTF_8),"demo.md");
        assertEquals("demo",parsed.dto.getTitle());
        assertTrue(parsed.dto.getContent().contains("body with no final newline"));
    }
    @Test void parallelYamlParsingUsesIndependentParsers() {
        java.util.stream.IntStream.range(0,40).parallel().forEach(i -> {
            var parsed=service.parseOne(("---\ntitle: article-"+i+"\n---\nbody").getBytes(StandardCharsets.UTF_8),"article.md");
            assertEquals("article-"+i,parsed.dto.getTitle());
        });
    }
}
