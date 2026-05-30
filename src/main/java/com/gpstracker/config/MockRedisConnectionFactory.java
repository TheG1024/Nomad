package com.gpstracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;

/**
 * A mock implementation of RedisConnectionFactory for embedded mode.
 * This provides a no-op implementation that doesn't require an actual Redis server.
 */
@Slf4j
public class MockRedisConnectionFactory implements RedisConnectionFactory {

    public MockRedisConnectionFactory() {
        log.info("Creating MockRedisConnectionFactory");
    }

    @Override
    public RedisConnection getConnection() {
        return new MockRedisConnection();
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        throw new UnsupportedOperationException("Cluster not supported in mock mode");
    }

    @Override
    public boolean getConvertPipelineAndTxResults() {
        return false;
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        throw new UnsupportedOperationException("Sentinel not supported in mock mode");
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return null;
    }
} 