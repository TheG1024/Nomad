package com.gpstracker.service;

import java.time.Duration;

/**
 * Service for caching operations
 */
public interface CacheService {

    /**
     * Store a value in the cache with no expiration
     *
     * @param key   the cache key
     * @param value the value to cache
     */
    void put(String key, Object value);

    /**
     * Store a value in the cache with expiration
     *
     * @param key   the cache key
     * @param value the value to cache
     * @param ttl   time to live
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Retrieve a value from the cache
     *
     * @param key  the cache key
     * @param type the expected type
     * @param <T>  the type parameter
     * @return the cached value, or null if not found
     */
    <T> T get(String key, Class<T> type);

    /**
     * Delete a value from the cache
     *
     * @param key the cache key
     */
    void delete(String key);

    /**
     * Clear the entire cache
     */
    void deleteAll();
} 