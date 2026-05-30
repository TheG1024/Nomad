package com.gpstracker.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for caching.
 * Provides an in-memory cache manager when the 'embedded' profile is active.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Create an in-memory cache manager for embedded mode
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        log.info("Initializing simple in-memory cache manager");
        return new ConcurrentMapCacheManager(
            "geofences", 
            "devices", 
            "alerts", 
            "statistics"
        );
    }
} 