package com.blog.service;

import com.blog.entity.Article;
import com.blog.entity.EmailSubscription;
import com.blog.mapper.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.function.BooleanSupplier;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotifyServiceTest {
    @Test void emptyAudiencePreservesPendingContentAndNextScanResumesDelivery() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                        new com.baomidou.mybatisplus.core.MybatisConfiguration(), "notify-test"), Article.class);
        ArticleMapper articles = mock(ArticleMapper.class);
        ProjectMapper projects = mock(ProjectMapper.class);
        ToolMapper tools = mock(ToolMapper.class);
        EmailSubscriptionMapper subscribers = mock(EmailSubscriptionMapper.class);
        MailService mail = mock(MailService.class);
        NotificationDeliveryService deliveries = mock(NotificationDeliveryService.class);
        NotifyService service = new NotifyService(articles, projects, tools, subscribers,
                mail, deliveries, true, "https://example.com", "Blog");
        EmailSubscription subscriber = new EmailSubscription();
        subscriber.setId(1L);
        subscriber.setStatus(1);
        subscriber.setEmail("reader@example.com");
        subscriber.setToken("unsubscribe-token");
        when(subscribers.selectList(any())).thenReturn(List.of(), List.of(subscriber));

        service.notifyPending();
        verifyNoInteractions(articles, projects, tools, mail, deliveries);

        Article article = new Article();
        article.setId(19L);
        article.setTitle("New article");
        when(articles.selectList(any())).thenReturn(List.of(article));
        when(projects.selectList(any())).thenReturn(List.of());
        when(tools.selectList(any())).thenReturn(List.of());
        when(subscribers.selectById(1L)).thenReturn(subscriber);
        when(mail.sendSync(eq("reader@example.com"), anyString(), anyString(), anyString())).thenReturn(true);
        when(deliveries.deliver(eq("ARTICLE"), eq(19L), eq(1L), any()))
                .thenAnswer(call -> call.<BooleanSupplier>getArgument(3).getAsBoolean());

        service.notifyPending();
        verify(mail).sendSync(eq("reader@example.com"), anyString(), anyString(), anyString());
        verify(articles).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
}
