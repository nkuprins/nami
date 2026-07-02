package com.app.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PROPERTY_DETAIL = "propertyDetail";
    public static final String PROPERTY_LIST = "propertyList";

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache(PROPERTY_DETAIL, Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        manager.registerCustomCache(PROPERTY_LIST, Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        return manager;
    }
}
