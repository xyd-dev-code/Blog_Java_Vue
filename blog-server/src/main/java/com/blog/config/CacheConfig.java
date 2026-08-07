package com.blog.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // 注意：带 cacheNames 的构造器会关闭 dynamic 创建，
        // 未在此登记的缓存名会让 @Cacheable 抛 "Cannot find cache named ..."。
        CaffeineCacheManager mgr = new CaffeineCacheManager(
                "articles", "categories", "tags", "projectCategories");
        mgr.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500));
        return mgr;
    }
}