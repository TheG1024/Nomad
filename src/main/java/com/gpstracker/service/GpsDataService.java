package com.gpstracker.service;

import com.gpstracker.model.GpsData;
import com.gpstracker.model.Geofence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for handling GPS data operations
 * This class delegates to the GpsDataServiceFactory which will use either
 * in-memory storage or Redis based on the active profile
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GpsDataService {

    private static final String GPS_DATA_KEY_PREFIX = "gps:data:";
    private static final String GEOFENCE_KEY_PREFIX = "gps:geofence:";
    private static final String ALERT_KEY_PREFIX = "gps:alert:";
    private static final String STATS_KEY_PREFIX = "gps:stats:";
    private static final int DATA_RETENTION_DAYS = 7;

    public static final double BATTERY_ALERT_THRESHOLD = 20.0; // 20% (batteryLevel is 0-100 integer scale)
    public static final double SPEED_ALERT_THRESHOLD = 120.0; // km/h
    public static final int OFFLINE_THRESHOLD_MINUTES = 5;

    @Autowired(required = false)
    private RedisTemplate<String, GpsData> redisTemplate;

    @Autowired(required = false)
    private GeofenceService geofenceService;

    private final GpsDataServiceFactory factory;

    /**
     * Save GPS data to the appropriate storage
     */
    public void saveGpsData(GpsData gpsData) {
        factory.saveGpsData(gpsData);
    }

    /**
     * Get device statistics
     */
    public Map<String, Object> getDeviceStatistics(String deviceId, LocalDateTime date) {
        return factory.getDeviceStatistics(deviceId, date);
    }

    /**
     * Get GPS data for a device within a time range
     */
    public List<GpsData> getGpsDataForDevice(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return factory.getGpsDataForDevice(deviceId, startTime, endTime);
    }

    /**
     * Get alerts for a device within a time range
     */
    public List<GpsData> getAlerts(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return factory.getAlerts(deviceId, startTime, endTime);
    }

    /**
     * Set a legacy geofence
     */
    public void setGeofence(String deviceId, double centerLat, double centerLon, double radius) {
        factory.setGeofence(deviceId, centerLat, centerLon, radius);
    }

    /**
     * Weekly export (delegated to factory)
     */
    public void weeklyExport() {
        factory.weeklyExport();
    }
}
