package com.gpstracker.service;

import com.gpstracker.model.GpsData;
import com.gpstracker.model.Geofence;
import com.gpstracker.service.impl.InMemoryGpsDataServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Factory for creating an appropriate GpsDataService implementation
 * based on the active profile
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class GpsDataServiceFactory {

    private final Environment environment;
    private final GeofenceService geofenceService;
    private final InMemoryGpsDataServiceImpl inMemoryGpsDataService;
    
    // Optional RedisTemplate that might be null in embedded mode
    private final Optional<RedisTemplate<String, GpsData>> redisTemplate;
    
    // In-memory storage used in embedded mode
    private final Map<String, GpsData> gpsDataStore = new ConcurrentHashMap<>();
    private final Map<String, GpsData> alertsStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> statsStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> geofenceStore = new ConcurrentHashMap<>();
    
    // Constants
    private static final String GPS_DATA_KEY_PREFIX = "gps:data:";
    private static final String GEOFENCE_KEY_PREFIX = "gps:geofence:";
    private static final String ALERT_KEY_PREFIX = "gps:alert:";
    private static final String STATS_KEY_PREFIX = "gps:stats:";
    private static final int DATA_RETENTION_DAYS = 7;
    private static final double BATTERY_ALERT_THRESHOLD = 0.2; // 20%
    private static final double SPEED_ALERT_THRESHOLD = 120.0; // km/h
    private static final int OFFLINE_THRESHOLD_MINUTES = 5;
    
    /**
     * Check if running in embedded mode (without Redis)
     */
    private boolean isEmbeddedMode() {
        return Arrays.asList(environment.getActiveProfiles()).contains("embedded");
    }
    
    /**
     * Save GPS data to the appropriate storage
     */
    public void saveGpsData(GpsData gpsData) {
        inMemoryGpsDataService.saveGpsData(gpsData);
    }
    
    /**
     * Get device statistics
     */
    public Map<String, Object> getDeviceStatistics(String deviceId, LocalDateTime date) {
        return inMemoryGpsDataService.getDeviceStatistics(deviceId, date);
    }
    
    /**
     * Get GPS data for a device within a time range
     */
    public List<GpsData> getGpsDataForDevice(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return inMemoryGpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);
    }
    
    /**
     * Get alerts for a device within a time range
     */
    public List<GpsData> getAlerts(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return inMemoryGpsDataService.getAlerts(deviceId, startTime, endTime);
    }
    
    /**
     * Set a legacy geofence
     */
    public void setGeofence(String deviceId, double centerLat, double centerLon, double radius) {
        inMemoryGpsDataService.setGeofence(deviceId, centerLat, centerLon, radius);
    }
    
    /**
     * Scheduled weekly export
     */
    @Scheduled(cron = "0 0 0 * * 0") // Run at midnight every Sunday
    public void weeklyExport() {
        inMemoryGpsDataService.weeklyExport();
    }
} 