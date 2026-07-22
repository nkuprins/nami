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
    public static final String PROPERTY_TRANSLATION = "propertyTranslation";
    public static final String PROPERTY_LIST = "propertyList";
    public static final String PROPERTY_MAP = "propertyMap";
    public static final String PROPERTY_KIND_COUNTS = "propertyKindCounts";
    public static final String ADDRESS_STREETS = "addressStreets";
    public static final String ADDRESS_BUILDINGS = "addressBuildings";

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache(PROPERTY_DETAIL, Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        manager.registerCustomCache(PROPERTY_TRANSLATION, Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        manager.registerCustomCache(PROPERTY_LIST, Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        manager.registerCustomCache(PROPERTY_MAP, Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        manager.registerCustomCache(PROPERTY_KIND_COUNTS, Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        // Typeahead results are tiny (≤20 rows); keystroke-level keys keep Neon
        // idle between searches for the popular prefixes.
        manager.registerCustomCache(ADDRESS_STREETS, Caffeine.newBuilder()
                .maximumSize(20_000)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        manager.registerCustomCache(ADDRESS_BUILDINGS, Caffeine.newBuilder()
                .maximumSize(20_000)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        return manager;
    }
}
