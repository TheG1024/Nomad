package com.gpstracker.service.alert;

import com.gpstracker.controller.WebSocketController;
import com.gpstracker.model.alert.UserReportedAlert;
import com.gpstracker.model.alert.UserReportedAlert.AlertStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserReportedAlertService {
    
    private static final String ALERTS_KEY_PREFIX = "nomad:alerts:";
    private static final int MAX_ALERTS_PER_AREA = 50;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private WebSocketController webSocketController;
    
    private final Map<String, UserReportedAlert> activeAlerts = new ConcurrentHashMap<>();
    
    /**
     * Report a new alert (Waze-style)
     */
    public UserReportedAlert reportAlert(UserReportedAlert alert) {
        alert.setId(UUID.randomUUID().toString());
        alert.setReportedAt(Instant.now());
        alert.setStatus(AlertStatus.ACTIVE);
        alert.setUpvotes(0);
        alert.setDownvotes(0);
        
        // Store in Redis and memory
        activeAlerts.put(alert.getId(), alert);
        storeAlertInRedis(alert);
        
        // Broadcast to all connected clients
        webSocketController.broadcastUserAlert(alert);
        
        log.info("New alert reported: {} at ({}, {})", alert.getType(), alert.getLatitude(), alert.getLongitude());
        return alert;
    }
    
    /**
     * Upvote an alert (confirm it exists)
     */
    public UserReportedAlert upvote(String alertId) {
        UserReportedAlert alert = activeAlerts.get(alertId);
        if (alert != null) {
            alert.setUpvotes(alert.getUpvotes() + 1);
            updateAlertInRedis(alert);
            webSocketController.broadcastUserAlert(alert);
            log.debug("Alert {} upvoted", alertId);
        }
        return alert;
    }
    
    /**
     * Downvote an alert (mark as incorrect)
     */
    public UserReportedAlert downvote(String alertId) {
        UserReportedAlert alert = activeAlerts.get(alertId);
        if (alert != null) {
            alert.setDownvotes(alert.getDownvotes() + 1);
            
            // Auto-expire if downvotes exceed threshold
            if (alert.getDownvotes() >= 3) {
                alert.setStatus(AlertStatus.REJECTED);
                activeAlerts.remove(alertId);
            } else {
                updateAlertInRedis(alert);
            }
            
            webSocketController.broadcastUserAlert(alert);
            log.debug("Alert {} downvoted", alertId);
        }
        return alert;
    }
    
    /**
     * Get alerts within a radius (for map viewport)
     */
    public List<UserReportedAlert> getAlertsInArea(double north, double south, double east, double west) {
        return activeAlerts.values().stream()
            .filter(alert -> alert.getStatus() == AlertStatus.ACTIVE || alert.getStatus() == AlertStatus.CONFIRMED)
            .filter(alert -> !alert.isExpired())
            .filter(alert -> alert.getLatitude() >= south && alert.getLatitude() <= north)
            .filter(alert -> alert.getLongitude() >= west && alert.getLongitude() <= east)
            .sorted(Comparator.comparingDouble(UserReportedAlert::getScore).reversed())
            .limit(MAX_ALERTS_PER_AREA)
            .collect(Collectors.toList());
    }
    
    /**
     * Get nearby alerts for a specific location
     */
    public List<UserReportedAlert> getNearbyAlerts(double latitude, double longitude, double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));
        
        return getAlertsInArea(
            latitude + latDelta,
            latitude - latDelta,
            longitude + lngDelta,
            longitude - lngDelta
        );
    }
    
    /**
     *Confirm an alert (after verification)
     */
    public UserReportedAlert confirmAlert(String alertId) {
        UserReportedAlert alert = activeAlerts.get(alertId);
        if (alert != null) {
            alert.setStatus(AlertStatus.CONFIRMED);
            alert.setConfidence(10);
            updateAlertInRedis(alert);
            webSocketController.broadcastUserAlert(alert);
            log.info("Alert {} confirmed", alertId);
        }
        return alert;
    }
    
    /**
     * Remove an alert
     */
    public void removeAlert(String alertId) {
        UserReportedAlert alert = activeAlerts.remove(alertId);
        if (alert != null) {
            alert.setStatus(AlertStatus.EXPIRED);
            removeFromRedis(alert);
            webSocketController.broadcastAlertRemoved(alertId);
            log.info("Alert {} removed", alertId);
        }
    }
    
    /**
     * Scheduled task to clean up expired alerts
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredAlerts() {
        List<String> expiredIds = activeAlerts.values().stream()
            .filter(UserReportedAlert::isExpired)
            .map(UserReportedAlert::getId)
            .collect(Collectors.toList());
        
        expiredIds.forEach(this::removeAlert);
        
        if (!expiredIds.isEmpty()) {
            log.info("Cleaned up {} expired alerts", expiredIds.size());
        }
    }
    
    private void storeAlertInRedis(UserReportedAlert alert) {
        try {
            String key = ALERTS_KEY_PREFIX + alert.getId();
            redisTemplate.opsForHash().putAll(key, convertToMap(alert));
            redisTemplate.expire(key, 2, java.util.concurrent.TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to store alert in Redis: {}", e.getMessage());
        }
    }
    
    private void updateAlertInRedis(UserReportedAlert alert) {
        storeAlertInRedis(alert);
    }
    
    private void removeFromRedis(UserReportedAlert alert) {
        try {
            String key = ALERTS_KEY_PREFIX + alert.getId();
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to remove alert from Redis: {}", e.getMessage());
        }
    }
    
    private Map<String, Object> convertToMap(UserReportedAlert alert) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", alert.getId());
        map.put("type", alert.getType().name());
        map.put("subtype", alert.getSubtype().name());
        map.put("latitude", alert.getLatitude());
        map.put("longitude", alert.getLongitude());
        map.put("street", alert.getStreet());
        map.put("reliability", alert.getReliability());
        map.put("confidence", alert.getConfidence());
        map.put("description", alert.getDescription());
        map.put("reportedBy", alert.getReportedBy());
        map.put("reportedAt", alert.getReportedAt().toEpochMilli());
        map.put("upvotes", alert.getUpvotes());
        map.put("downvotes", alert.getDownvotes());
        map.put("status", alert.getStatus().name());
        return map;
    }
}