package com.gpstracker.service;

import com.gpstracker.model.GpsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GpsDataService {

    private static final String GPS_DATA_KEY_PREFIX = "gps:data:";
    private static final String GEOFENCE_KEY_PREFIX = "gps:geofence:";
    private static final String ALERT_KEY_PREFIX = "gps:alert:";
    private static final String STATS_KEY_PREFIX = "gps:stats:";
    private static final int DATA_RETENTION_DAYS = 7;
    
    private static final double BATTERY_ALERT_THRESHOLD = 0.2; // 20%
    private static final double SPEED_ALERT_THRESHOLD = 120.0; // km/h
    private static final int OFFLINE_THRESHOLD_MINUTES = 5;

    @Autowired
    private RedisTemplate<String, GpsData> redisTemplate;

    public void saveGpsData(GpsData gpsData) {
        if (gpsData.getTimestamp() == null) {
            gpsData.setTimestamp(LocalDateTime.now());
        }

        // Update device status
        updateDeviceStatus(gpsData);
        
        // Check for alerts
        checkAlerts(gpsData);
        
        // Check geofence
        checkGeofence(gpsData);
        
        // Save data
        String key = GPS_DATA_KEY_PREFIX + gpsData.getDeviceId() + ":" + 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        redisTemplate.opsForValue().set(key, gpsData);
        redisTemplate.expire(key, DATA_RETENTION_DAYS, TimeUnit.DAYS);
        
        // Update statistics
        updateStatistics(gpsData);

        String lastDataKey = getLastDataKey(gpsData.getDeviceId());
        redisTemplate.opsForValue().set(lastDataKey, gpsData);
        redisTemplate.expire(lastDataKey, DATA_RETENTION_DAYS, TimeUnit.DAYS);
    }

    private void updateDeviceStatus(GpsData gpsData) {
        // Get last known data
        String lastDataKey = getLastDataKey(gpsData.getDeviceId());
        GpsData lastData = redisTemplate.opsForValue().get(lastDataKey);

        gpsData.setDeviceStatus(determineDeviceStatus(gpsData, lastData));
    }

    private String determineDeviceStatus(GpsData currentData, GpsData lastData) {
        if (lastData != null && lastData.getTimestamp() != null) {
            Duration timeSinceLastUpdate = Duration.between(lastData.getTimestamp(), currentData.getTimestamp());

            if (timeSinceLastUpdate.toMinutes() > OFFLINE_THRESHOLD_MINUTES) {
                return "OFFLINE";
            }
        }

        if (currentData.getSpeed() < 1.0) {
            return "IDLE";
        }

        return "ACTIVE";
    }

    private void checkAlerts(GpsData gpsData) {
        // Battery alert
        gpsData.setLowBattery(gpsData.getBatteryLevel() < BATTERY_ALERT_THRESHOLD);
        
        // Speed alert
        gpsData.setSpeedAlert(gpsData.getSpeed() > SPEED_ALERT_THRESHOLD);
        
        // Malfunction alert (based on accuracy and signal strength)
        gpsData.setMalfunctionAlert(gpsData.getAccuracy() > 100 || gpsData.getSignalStrength() < 2);
        
        if (gpsData.isLowBattery() || gpsData.isSpeedAlert() || gpsData.isMalfunctionAlert()) {
            String alertKey = ALERT_KEY_PREFIX + gpsData.getDeviceId();
            redisTemplate.opsForList().leftPush(alertKey, gpsData);
            redisTemplate.expire(alertKey, DATA_RETENTION_DAYS, TimeUnit.DAYS);
        }
    }

    private void checkGeofence(GpsData gpsData) {
        String geofenceKey = GEOFENCE_KEY_PREFIX + gpsData.getDeviceId();
        Map<Object, Object> geofenceData = redisTemplate.opsForHash().entries(geofenceKey);
        
        if (!geofenceData.isEmpty()) {
            double centerLat = Double.parseDouble(geofenceData.get("centerLat").toString());
            double centerLon = Double.parseDouble(geofenceData.get("centerLon").toString());
            double radius = Double.parseDouble(geofenceData.get("radius").toString());
            
            double distance = calculateDistance(
                centerLat, centerLon,
                gpsData.getLatitude(), gpsData.getLongitude()
            );
            
            gpsData.setGeofenceAlert(distance > radius);
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    private void updateStatistics(GpsData gpsData) {
        String statsKey = STATS_KEY_PREFIX + gpsData.getDeviceId() + ":" 
                         + gpsData.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        redisTemplate.opsForHash().increment(statsKey, "totalDistance", calculateDistanceFromLast(gpsData));
        redisTemplate.opsForHash().increment(statsKey, "dataPoints", 1);
        redisTemplate.opsForHash().increment(statsKey, "alerts", 
            (gpsData.isLowBattery() || gpsData.isSpeedAlert() || gpsData.isGeofenceAlert()) ? 1 : 0);
        
        // Update max speed
        Double currentMaxSpeed = (Double) redisTemplate.opsForHash().get(statsKey, "maxSpeed");
        if (currentMaxSpeed == null || gpsData.getSpeed() > currentMaxSpeed) {
            redisTemplate.opsForHash().put(statsKey, "maxSpeed", gpsData.getSpeed());
        }
        
        redisTemplate.expire(statsKey, DATA_RETENTION_DAYS, TimeUnit.DAYS);
    }

    private double calculateDistanceFromLast(GpsData gpsData) {
        String lastDataKey = getLastDataKey(gpsData.getDeviceId());
        GpsData lastData = redisTemplate.opsForValue().get(lastDataKey);
        
        if (lastData != null) {
            return calculateDistance(
                lastData.getLatitude(), lastData.getLongitude(),
                gpsData.getLatitude(), gpsData.getLongitude()
            );
        }
        return 0.0;
    }

    private String getLastDataKey(String deviceId) {
        return GPS_DATA_KEY_PREFIX + deviceId + ":last";
    }

    public Map<String, Object> getDeviceStatistics(String deviceId, LocalDate date) {
        String statsKey = STATS_KEY_PREFIX + deviceId + ":" + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        Map<Object, Object> rawData = redisTemplate.opsForHash().entries(statsKey);

        Map<String, Object> result = new HashMap<>();
        rawData.forEach((key, value) -> result.put(key.toString(), value));
        return result;
    }

    public Optional<GpsData> getLatestGpsData(String deviceId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(getLastDataKey(deviceId)));
    }

    public int getRecentAlertCount(String deviceId, Duration lookbackWindow) {
        String alertKey = ALERT_KEY_PREFIX + deviceId;
        LocalDateTime cutoff = LocalDateTime.now().minus(lookbackWindow);
        return (int) Optional.ofNullable(redisTemplate.opsForList().range(alertKey, 0, -1))
                .orElse(Collections.emptyList())
                .stream()
                .filter(data -> data != null && data.getTimestamp() != null)
                .filter(data -> !data.getTimestamp().isBefore(cutoff))
                .count();
    }

    public List<GpsData> getGpsDataForDevice(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        String keyPattern = GPS_DATA_KEY_PREFIX + deviceId + ":*";
        Set<String> keys = redisTemplate.keys(keyPattern);

        return Optional.ofNullable(keys)
                .orElse(Collections.emptySet())
                .stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .filter(data -> data.getTimestamp() != null)
                .filter(data -> !data.getTimestamp().isBefore(startTime) && !data.getTimestamp().isAfter(endTime))
                .sorted(Comparator.comparing(GpsData::getTimestamp))
                .collect(Collectors.toList());
    }

    public List<GpsData> getAlerts(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        String alertKey = ALERT_KEY_PREFIX + deviceId;
        return Optional.ofNullable(redisTemplate.opsForList().range(alertKey, 0, -1))
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(data -> data.getTimestamp() != null)
                .filter(data -> !data.getTimestamp().isBefore(startTime) && !data.getTimestamp().isAfter(endTime))
                .collect(Collectors.toList());
    }

    public void setGeofence(String deviceId, double centerLat, double centerLon, double radius) {
        String geofenceKey = GEOFENCE_KEY_PREFIX + deviceId;
        Map<String, Object> geofence = new HashMap<>();
        geofence.put("centerLat", centerLat);
        geofence.put("centerLon", centerLon);
        geofence.put("radius", radius);
        
        redisTemplate.opsForHash().putAll(geofenceKey, geofence);
    }

    @Scheduled(cron = "0 0 0 * * 0") // Run at midnight every Sunday
    public void weeklyExport() {
        log.info("Starting weekly GPS data export");
        // Implementation for weekly export will be added
    }
}
