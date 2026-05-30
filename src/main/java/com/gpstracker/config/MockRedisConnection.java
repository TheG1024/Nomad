package com.gpstracker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.data.redis.connection.RedisHyperLogLogCommands;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.connection.RedisPubSubCommands;
import org.springframework.data.redis.connection.RedisScriptingCommands;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.RedisSetCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.RedisTxCommands;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.types.RedisClientInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * A mock implementation of RedisConnection for embedded mode.
 * This provides a no-op implementation that doesn't require an actual Redis server.
 */
@Slf4j
public class MockRedisConnection implements RedisConnection {
    
    public MockRedisConnection() {
        log.debug("Creating MockRedisConnection");
    }

    @Override
    public RedisCommands commands() {
        return this;
    }

    @Override
    public RedisGeoCommands geoCommands() {
        return null;
    }

    @Override
    public RedisHashCommands hashCommands() {
        return null;
    }

    @Override
    public RedisHyperLogLogCommands hyperLogLogCommands() {
        return null;
    }

    @Override
    public RedisKeyCommands keyCommands() {
        return null;
    }

    @Override
    public RedisListCommands listCommands() {
        return null;
    }

    @Override
    public RedisSetCommands setCommands() {
        return null;
    }

    @Override
    public RedisScriptingCommands scriptingCommands() {
        return null;
    }

    @Override
    public RedisServerCommands serverCommands() {
        return null;
    }

    @Override
    public RedisStringCommands stringCommands() {
        return null;
    }

    @Override
    public RedisZSetCommands zSetCommands() {
        return null;
    }

    @Override
    public RedisStreamCommands streamCommands() {
        return null;
    }

    @Override
    public void close() throws DataAccessException {
        // No-op
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public Object getNativeConnection() {
        return null;
    }

    @Override
    public boolean isQueueing() {
        return false;
    }

    @Override
    public boolean isPipelined() {
        return false;
    }

    @Override
    public void openPipeline() {
        // No-op
    }

    @Override
    public List<Object> closePipeline() {
        return new ArrayList<>();
    }

    @Override
    public void multi() {
        // No-op
    }

    @Override
    public List<Object> exec() {
        return new ArrayList<>();
    }

    @Override
    public void discard() {
        // No-op
    }

    @Override
    public void watch(byte[]... keys) {
        // No-op
    }

    @Override
    public void unwatch() {
        // No-op
    }

    @Override
    public boolean isSubscribed() {
        return false;
    }

    @Override
    public Subscription getSubscription() {
        return null;
    }

    @Override
    public Long publish(byte[] channel, byte[] message) {
        return 0L;
    }

    @Override
    public void subscribe(MessageListener listener, byte[]... channels) {
        // No-op
    }

    @Override
    public void pSubscribe(MessageListener listener, byte[]... patterns) {
        // No-op
    }

    @Override
    public void select(int dbIndex) {
        // No-op
    }

    @Override
    public byte[] echo(byte[] message) {
        return message;
    }

    @Override
    public String ping() {
        return "PONG";
    }

    @Override
    public void flushDb() {
        // No-op
    }

    @Override
    public void flushAll() {
        // No-op
    }

    @Override
    public Properties info() {
        Properties props = new Properties();
        props.setProperty("redis_version", "0.0.0");
        props.setProperty("redis_mode", "standalone");
        props.setProperty("os", "mock");
        props.setProperty("tcp_port", "0");
        props.setProperty("uptime_in_seconds", "0");
        props.setProperty("connected_clients", "1");
        return props;
    }

    @Override
    public Properties info(String section) {
        return info();
    }

    @Override
    public void shutdown() {
        // No-op
    }

    @Override
    public void bgSave() {
        // No-op
    }

    @Override
    public void bgReWriteAof() {
        // No-op
    }

    @Override
    public void save() {
        // No-op
    }

    @Override
    public Long lastSave() {
        return System.currentTimeMillis();
    }

    @Override
    public void resetConfigStats() {
        // No-op
    }

    @Override
    public Long time() {
        return System.currentTimeMillis();
    }

    @Override
    public Long time(TimeUnit timeUnit) {
        return timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public List<RedisClientInfo> getClientList() {
        return Collections.emptyList();
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        throw new UnsupportedOperationException("Sentinel connections not supported in mock mode");
    }

    @Override
    public Object execute(String command, byte[]... args) {
        log.debug("Mock executing command: {}", command);
        return null;
    }
} 