package com.gpstracker.service.impl;

import com.gpstracker.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis implementation of CacheService
 */
@Slf4j
@Service
@Profile("!embedded & !test")
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Added to Redis cache: {}", key);
        } catch (Exception e) {
            log.error("Error storing value in Redis cache: {}", key, e);
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            if (ttl != null) {
                redisTemplate.opsForValue().set(key, value, ttl);
                log.debug("Added to Redis cache with TTL: {} ({}ms)", key, ttl.toMillis());
            } else {
                put(key, value);
            }
        } catch (Exception e) {
            log.error("Error storing value in Redis cache with TTL: {}", key, e);
        }
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.debug("Redis cache miss: {}", key);
                return null;
            }

            if (type.isInstance(value)) {
                log.debug("Redis cache hit: {}", key);
                return type.cast(value);
            } else {
                log.warn("Redis cache hit but type mismatch for key: {}, expected: {}, got: {}",
                        key, type.getName(), value.getClass().getName());
                return null;
            }
        } catch (Exception e) {
            log.error("Error retrieving value from Redis cache: {}", key, e);
            return null;
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Removed from Redis cache: {}", key);
        } catch (Exception e) {
            log.error("Error deleting value from Redis cache: {}", key, e);
        }
    }

    @Override
    public void deleteAll() {
        try {
            // This is a potentially dangerous operation, so log it prominently
            log.warn("Clearing entire Redis cache");
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            log.info("Redis cache cleared");
        } catch (Exception e) {
            log.error("Error clearing Redis cache", e);
        }
    }
}