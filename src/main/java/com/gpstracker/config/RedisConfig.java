package com.gpstracker.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gpstracker.model.GpsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisSentinelConnection;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis configuration for the application.
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    // In-memory storage for fallback mode
    private static final Map<String, Object> IN_MEMORY_STORAGE = new ConcurrentHashMap<>();

    // Status indicator for the Redis connection
    private volatile boolean redisAvailable = false;

    /**
     * Redis connection factory for non-embedded mode
     */
    @Bean
    @Profile("!embedded & !test")
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            // Default connection to localhost:6379
            RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration("localhost", 6379);
            JedisConnectionFactory factory = new JedisConnectionFactory(redisConfig);
            factory.afterPropertiesSet();

            // Test connection
            factory.getConnection().ping();
            log.info("Successfully connected to Redis");
            redisAvailable = true;
            return factory;
        } catch (Exception e) {
            log.warn("Redis connection failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Mock Redis connection factory for embedded mode
     */
    @Bean
    @Profile("embedded | test")
    public RedisConnectionFactory mockRedisConnectionFactory() {
        log.info("Creating mock Redis connection factory for embedded profile");
        return new MockRedisConnectionFactory();
    }

    /**
     * Redis template for non-embedded mode
     */
    @Bean("redisTemplate")
    @Profile("!embedded & !test")
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory == null) {
            return null;
        }

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * GPS data-specific Redis template for non-embedded mode
     */
    @Bean("gpsDataRedisTemplate")
    @Profile("!embedded & !test")
    public RedisTemplate<String, GpsData> gpsDataRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory == null) {
            return null;
        }

        RedisTemplate<String, GpsData> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Create mock RedisTemplate for GPS data when in embedded mode
     */
    @Bean("gpsDataRedisTemplate")
    @Profile("embedded | test")
    @ConditionalOnMissingBean(name = "gpsDataRedisTemplate")
    public RedisTemplate<String, GpsData> mockGpsDataRedisTemplate(RedisConnectionFactory mockRedisConnectionFactory) {
        log.info("Creating mock GPS data Redis template for embedded profile");
        RedisTemplate<String, GpsData> template = new RedisTemplate<>();
        template.setConnectionFactory(mockRedisConnectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Create mock RedisTemplate for Object when in embedded mode
     */
    @Bean("redisTemplate")
    @Profile("embedded | test")
    @Primary
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> mockRedisTemplate(RedisConnectionFactory mockRedisConnectionFactory) {
        log.info("Creating mock Redis template for embedded profile");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(mockRedisConnectionFactory);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cache manager for embedded profile
     */
    @Bean
    @Profile("embedded | test")
    public CacheManager cacheManager() {
        log.info("Creating in-memory cache manager for embedded profile");
        return new ConcurrentMapCacheManager();
    }

    /**
     * Public method to get an object from the in-memory storage (used in fallback
     * mode)
     */
    public static Object getFromMemory(String key) {
        return IN_MEMORY_STORAGE.get(key);
    }

    /**
     * Public method to set an object in the in-memory storage (used in fallback
     * mode)
     */
    public static void setInMemory(String key, Object value) {
        IN_MEMORY_STORAGE.put(key, value);
    }

    /**
     * Public method to check if the fallback mode is active
     */
    public boolean isFallbackActive() {
        return !redisAvailable;
    }

    /**
     * Check if Redis is available
     */
    public boolean isRedisAvailable() {
        return redisAvailable;
    }
}
