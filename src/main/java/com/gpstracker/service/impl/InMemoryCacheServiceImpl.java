package com.gpstracker.service.impl;

import com.gpstracker.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory implementation of CacheService for embedded mode
 */
@Slf4j
@Service
@Primary
@Profile("embedded | test")
public class InMemoryCacheServiceImpl implements CacheService {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Map<String, Long> expirations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);

    public InMemoryCacheServiceImpl() {
        // Schedule cleanup task to run every minute
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                1,
                1,
                TimeUnit.MINUTES);
        log.info("In-memory cache service initialized");
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
        log.debug("Added to cache: {}", key);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        cache.put(key, value);
        if (ttl != null) {
            expirations.put(key, System.currentTimeMillis() + ttl.toMillis());
        }
        log.debug("Added to cache with TTL: {} ({}ms)", key, ttl != null ? ttl.toMillis() : "indefinite");
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        // Check if key exists and hasn't expired
        if (!cache.containsKey(key)) {
            log.debug("Cache miss: {}", key);
            return null;
        }

        // Check expiration
        Long expiration = expirations.get(key);
        if (expiration != null && System.currentTimeMillis() > expiration) {
            cache.remove(key);
            expirations.remove(key);
            log.debug("Cache entry expired: {}", key);
            return null;
        }

        Object value = cache.get(key);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            log.debug("Cache hit: {}", key);
            return type.cast(value);
        } else {
            log.warn("Cache hit but type mismatch for key: {}, expected: {}, got: {}",
                    key, type.getName(), value.getClass().getName());
            return null;
        }
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
        expirations.remove(key);
        log.debug("Removed from cache: {}", key);
    }

    @Override
    public void deleteAll() {
        cache.clear();
        expirations.clear();
        log.debug("Cache cleared");
    }

    /**
     * Clean up expired entries
     */
    private void cleanupExpiredEntries() {
        try {
            long now = System.currentTimeMillis();
            int count = 0;

            for (Map.Entry<String, Long> entry : expirations.entrySet()) {
                if (entry.getValue() <= now) {
                    String key = entry.getKey();
                    cache.remove(key);
                    expirations.remove(key);
                    count++;
                }
            }

            if (count > 0) {
                log.debug("Cleaned up {} expired cache entries", count);
            }
        } catch (Exception e) {
            log.error("Error during cache cleanup", e);
        }
    }
}