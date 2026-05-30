package com.gpstracker.service.ai;

import com.gpstracker.model.GpsData;
import com.gpstracker.service.GpsDataService;
import com.gpstracker.service.ai.PatternLearningService.DevicePattern;
import com.gpstracker.service.ai.PredictionService.Anomaly;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service that provides context-aware intelligent alerting 
 * by combining AI pattern analysis, anomaly detection, and user preferences
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligentAlertingService {

    private final GpsDataService gpsDataService;
    private final PatternLearningService patternLearningService;
    private final PredictionService predictionService;
    
    // Cache of alerting configurations by device
    private final Map<String, AlertingConfiguration> alertConfigurations = new ConcurrentHashMap<>();
    
    // Cache of alert history to prevent duplicate alerts
    private final Map<String, Set<String>> recentAlertsByDevice = new ConcurrentHashMap<>();
    
    /**
     * Process a new GPS data point and generate intelligent alerts if needed
     * 
     * @param gpsData The new GPS data point
     * @return List of alerts generated from this data point
     */
    public List<Alert> processGpsDataPoint(GpsData gpsData) {
        String deviceId = gpsData.getDeviceId();
        log.debug("Processing GPS data point for intelligent alerting: {}", deviceId);
        
        // Get device configuration or use defaults
        AlertingConfiguration config = getOrCreateConfiguration(deviceId);
        
        // Skip processing if intelligent alerting is disabled
        if (!config.isEnabled()) {
            return Collections.emptyList();
        }
        
        List<Alert> alerts = new ArrayList<>();
        
        // Check for speed alerts with context awareness
        if (config.isSpeedAlertsEnabled()) {
            Alert speedAlert = checkForSpeedAlert(gpsData, config);
            if (speedAlert != null) {
                alerts.add(speedAlert);
            }
        }
        
        // Check for geofence violations with context awareness
        if (config.isGeofenceAlertsEnabled() && gpsData.isGeofenceAlert()) {
            Alert geofenceAlert = enhanceGeofenceAlert(gpsData, config);
            if (geofenceAlert != null) {
                alerts.add(geofenceAlert);
            }
        }
        
        // Check for battery alerts with context awareness
        if (config.isBatteryAlertsEnabled()) {
            Alert batteryAlert = checkForBatteryAlert(gpsData, config);
            if (batteryAlert != null) {
                alerts.add(batteryAlert);
            }
        }
        
        // Check for unusual behavior/anomalies with context awareness
        if (config.isAnomalyAlertsEnabled()) {
            List<Alert> anomalyAlerts = checkForAnomalies(gpsData, config);
            alerts.addAll(anomalyAlerts);
        }
        
        // Record alerts to prevent duplicates
        recordAlerts(deviceId, alerts);
        
        return alerts;
    }
    
    /**
     * Configure alert preferences for a device
     * 
     * @param deviceId Device ID to configure
     * @param configuration Alert configuration settings
     */
    public void configureAlerts(String deviceId, AlertingConfiguration configuration) {
        log.info("Updating alert configuration for device {}", deviceId);
        alertConfigurations.put(deviceId, configuration);
    }
    
    /**
     * Get the current alert configuration for a device
     * 
     * @param deviceId Device ID to get configuration for
     * @return The current configuration
     */
    public AlertingConfiguration getAlertConfiguration(String deviceId) {
        return getOrCreateConfiguration(deviceId);
    }
    
    /**
     * Clear alert history for a device
     * 
     * @param deviceId Device ID to clear history for
     */
    public void clearAlertHistory(String deviceId) {
        recentAlertsByDevice.remove(deviceId);
    }
    
    /**
     * Get alert recommendations for a device based on usage patterns
     * 
     * @param deviceId Device ID to generate recommendations for
     * @return Recommended alert configuration
     */
    public AlertRecommendation getAlertRecommendations(String deviceId) {
        log.info("Generating alert recommendations for device {}", deviceId);
        
        DevicePattern pattern = patternLearningService.getPatternForDevice(deviceId);
        AlertingConfiguration currentConfig = getOrCreateConfiguration(deviceId);
        
        // Get recent data
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minus(30, ChronoUnit.DAYS);
        List<GpsData> recentData = gpsDataService.getGpsDataForDevice(deviceId, startTime, now);
        
        // Create recommendation
        AlertRecommendation recommendation = new AlertRecommendation();
        recommendation.setDeviceId(deviceId);
        
        if (pattern == null || recentData.isEmpty()) {
            log.warn("Insufficient data for device {} to generate alert recommendations", deviceId);
            return createDefaultRecommendation(deviceId, currentConfig);
        }
        
        // Analyze patterns to determine appropriate alert configurations
        
        // 1. Speed alerts - look at typical speeds
        double maxRegularSpeed = recentData.stream()
                .mapToDouble(GpsData::getSpeed)
                .filter(s -> s > 0)
                .sorted()
                .skip((long) (recentData.size() * 0.95)) // 95th percentile
                .findFirst()
                .orElse(80.0) * 1.2; // 20% buffer
        
        recommendation.setRecommendedSpeedThreshold((int)Math.round(maxRegularSpeed));
        recommendation.setShouldEnableSpeedAlerts(true);
        
        // 2. Battery alert thresholds - analyze battery usage patterns
        Map<Double, Long> batteryLevelCounts = recentData.stream()
                .collect(Collectors.groupingBy(
                        GpsData::getBatteryLevel,
                        Collectors.counting()));
        
        // Find a good battery threshold (default to 15%)
        int batteryThreshold = 15;
        if (!batteryLevelCounts.isEmpty()) {
            // Determine if device regularly operates at low battery
            long lowBatteryPoints = batteryLevelCounts.entrySet().stream()
                    .filter(e -> e.getKey() <= 20.0)
                    .mapToLong(Map.Entry::getValue)
                    .sum();
            
            double lowBatteryRatio = (double) lowBatteryPoints / recentData.size();
            
            // If user often has low battery, set a lower threshold to avoid too many alerts
            if (lowBatteryRatio > 0.15) {
                batteryThreshold = 10;
            } else if (lowBatteryRatio < 0.05) {
                // User rarely has low battery, can set higher threshold
                batteryThreshold = 20;
            }
        }
        recommendation.setRecommendedBatteryThreshold(batteryThreshold);
        recommendation.setShouldEnableBatteryAlerts(true);
        
        // 3. Geofence alerts
        recommendation.setShouldEnableGeofenceAlerts(true);
        
        // 4. Anomaly detection - determine if the device has regular patterns
        boolean hasRegularPattern = pattern.getRoutines().size() > 2 && 
                                   !pattern.getFrequentLocations().isEmpty();
        
        recommendation.setShouldEnableAnomalyAlerts(hasRegularPattern);
        recommendation.setRecommendedAnomalySensitivity(
                hasRegularPattern ? "MEDIUM" : "LOW");
        
        // 5. Quiet hours recommendation based on inactivity times
        List<DayOfWeek> workdays = Arrays.asList(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        
        LocalTime earliestActivityTime = pattern.getRoutines().stream()
                .filter(r -> r.getDaysOfWeek().stream().anyMatch(workdays::contains))
                .map(r -> r.getFirstActivityTime())
                .filter(Objects::nonNull)
                .min(LocalTime::compareTo)
                .orElse(LocalTime.of(7, 0));
        
        LocalTime latestActivityTime = pattern.getRoutines().stream()
                .filter(r -> r.getDaysOfWeek().stream().anyMatch(workdays::contains))
                .map(r -> r.getLastActivityTime())
                .filter(Objects::nonNull)
                .max(LocalTime::compareTo)
                .orElse(LocalTime.of(22, 0));
        
        // Round to nearest hour for quiet hours
        int quietHoursStart = (latestActivityTime.getHour() + 1) % 24;
        int quietHoursEnd = earliestActivityTime.getHour();
        
        // Only recommend quiet hours if we have at least 4 hours of quiet time
        boolean hasQuietPeriod = false;
        if (quietHoursStart < quietHoursEnd) {
            hasQuietPeriod = (quietHoursEnd - quietHoursStart) >= 4;
        } else {
            hasQuietPeriod = (quietHoursEnd + (24 - quietHoursStart)) >= 4;
        }
        
        recommendation.setShouldEnableQuietHours(hasQuietPeriod);
        recommendation.setRecommendedQuietHoursStart(quietHoursStart);
        recommendation.setRecommendedQuietHoursEnd(quietHoursEnd);
        
        return recommendation;
    }
    
    /**
     * Apply alert recommendations to a device
     * 
     * @param deviceId Device ID to apply recommendations to
     * @param recommendation Recommendations to apply
     * @return Updated configuration
     */
    public AlertingConfiguration applyRecommendations(String deviceId, AlertRecommendation recommendation) {
        AlertingConfiguration config = getOrCreateConfiguration(deviceId);
        
        // Apply recommendations to configuration
        config.setEnabled(true);
        config.setSpeedAlertsEnabled(recommendation.isShouldEnableSpeedAlerts());
        config.setSpeedThreshold(recommendation.getRecommendedSpeedThreshold());
        
        config.setBatteryAlertsEnabled(recommendation.isShouldEnableBatteryAlerts());
        config.setBatteryThreshold(recommendation.getRecommendedBatteryThreshold());
        
        config.setGeofenceAlertsEnabled(recommendation.isShouldEnableGeofenceAlerts());
        
        config.setAnomalyAlertsEnabled(recommendation.isShouldEnableAnomalyAlerts());
        config.setAnomalySensitivity(recommendation.getRecommendedAnomalySensitivity());
        
        config.setQuietHoursEnabled(recommendation.isShouldEnableQuietHours());
        config.setQuietHoursStart(recommendation.getRecommendedQuietHoursStart());
        config.setQuietHoursEnd(recommendation.getRecommendedQuietHoursEnd());
        
        // Save configuration
        alertConfigurations.put(deviceId, config);
        
        log.info("Applied alert recommendations for device {}", deviceId);
        return config;
    }
    
    // Helper methods
    
    private AlertingConfiguration getOrCreateConfiguration(String deviceId) {
        return alertConfigurations.computeIfAbsent(deviceId, id -> {
            log.info("Creating default alert configuration for device {}", id);
            AlertingConfiguration config = new AlertingConfiguration();
            config.setDeviceId(id);
            config.setEnabled(true);
            config.setSpeedAlertsEnabled(true);
            config.setSpeedThreshold(120); // 120 km/h default
            config.setBatteryAlertsEnabled(true);
            config.setBatteryThreshold(15); // 15% default
            config.setGeofenceAlertsEnabled(true);
            config.setAnomalyAlertsEnabled(false); // Off by default
            config.setAnomalySensitivity("MEDIUM");
            config.setQuietHoursEnabled(false); // Off by default
            config.setQuietHoursStart(22); // 10 PM default
            config.setQuietHoursEnd(7);    // 7 AM default
            return config;
        });
    }
    
    private Alert checkForSpeedAlert(GpsData data, AlertingConfiguration config) {
        // Don't alert if speed is below threshold
        if (data.getSpeed() * 3.6 <= config.getSpeedThreshold()) {
            return null;
        }
        
        // Get context for this speed alert
        DevicePattern pattern = patternLearningService.getPatternForDevice(data.getDeviceId());
        
        // Alert ID to prevent duplicates
        String alertId = String.format("speed_%s_%s_%d", 
                data.getDeviceId(), 
                data.getTimestamp().toLocalDate(),
                Math.round(data.getSpeed() * 3.6 / 10) * 10); // Round to nearest 10 km/h
        
        // Check if we've already sent this alert recently
        if (hasRecentlySentAlert(data.getDeviceId(), alertId)) {
            return null;
        }
        
        // Detect if in quiet hours
        if (isInQuietHours(config)) {
            // Only alert during quiet hours if speed is critically high (50% above threshold)
            if (data.getSpeed() * 3.6 <= config.getSpeedThreshold() * 1.5) {
                return null;
            }
        }
        
        // Create basic alert
        Alert alert = new Alert();
        alert.setDeviceId(data.getDeviceId());
        alert.setTimestamp(data.getTimestamp());
        alert.setType("SPEED");
        alert.setSeverity("HIGH");
        alert.setId(alertId);
        
        double speedKmh = data.getSpeed() * 3.6;
        alert.setMessage(String.format("Speed alert: %.1f km/h exceeds threshold of %d km/h", 
                speedKmh, config.getSpeedThreshold()));
        
        // Add context when available
        if (pattern != null) {
            // Check if this is a usual route where the user drives fast
            boolean isKnownFastRoute = false;
            if (pattern.getRoutines().stream()
                    .anyMatch(r -> r.getDaysOfWeek().contains(data.getTimestamp().getDayOfWeek()))) {
                // This is a day when the user typically travels
                isKnownFastRoute = true;
            }
            
            if (isKnownFastRoute) {
                // Lower severity for known fast routes
                alert.setSeverity("MEDIUM");
                alert.setMessage(alert.getMessage() + " on a regular route");
            }
        }
        
        log.info("Generated speed alert for device {}: {} km/h", 
                data.getDeviceId(), Math.round(speedKmh));
        
        return alert;
    }
    
    private Alert enhanceGeofenceAlert(GpsData data, AlertingConfiguration config) {
        String deviceId = data.getDeviceId();
        
        // Alert ID to prevent duplicates
        String alertId = String.format("geofence_%s_%s", 
                deviceId,
                data.getTimestamp().truncatedTo(ChronoUnit.HOURS));
        
        // Check if we've already sent this alert recently
        if (hasRecentlySentAlert(deviceId, alertId)) {
            return null;
        }
        
        // Detect if in quiet hours
        if (isInQuietHours(config)) {
            return null; // No geofence alerts during quiet hours
        }
        
        // Get context for this geofence alert
        DevicePattern pattern = patternLearningService.getPatternForDevice(deviceId);
        
        // Create alert with context
        Alert alert = new Alert();
        alert.setDeviceId(deviceId);
        alert.setTimestamp(data.getTimestamp());
        alert.setType("GEOFENCE");
        alert.setSeverity("MEDIUM");
        alert.setId(alertId);
        
        // Default message
        alert.setMessage("Geofence boundary crossed");
        
        // Add context when available
        if (pattern != null) {
            // Check if this is an unusual time or day for this device to be moving
            boolean isExpectedMovement = pattern.getRoutines().stream()
                    .anyMatch(r -> {
                        // Check if this day and time matches the routine
                        if (!r.getDaysOfWeek().contains(data.getTimestamp().getDayOfWeek())) {
                            return false;
                        }
                        
                        LocalTime time = data.getTimestamp().toLocalTime();
                        return r.getFirstActivityTime() != null && 
                               r.getLastActivityTime() != null &&
                               !time.isBefore(r.getFirstActivityTime()) &&
                               !time.isAfter(r.getLastActivityTime());
                    });
            
            if (!isExpectedMovement) {
                // Movement at unusual time
                alert.setSeverity("HIGH");
                alert.setMessage("Unexpected geofence boundary crossed at unusual time");
            }
        }
        
        log.info("Generated geofence alert for device {}: {}", deviceId, alert.getMessage());
        
        return alert;
    }
    
    private Alert checkForBatteryAlert(GpsData data, AlertingConfiguration config) {
        // Only alert if battery is below threshold
        if (data.getBatteryLevel() >= config.getBatteryThreshold()) {
            return null;
        }
        
        // Alert ID to prevent duplicates - only alert once per 5% battery drop
        String alertId = String.format("battery_%s_%d", 
                data.getDeviceId(),
                Math.round(data.getBatteryLevel() / 5) * 5);
        
        // Check if we've already sent this alert recently
        if (hasRecentlySentAlert(data.getDeviceId(), alertId)) {
            return null;
        }
        
        // Decide on severity
        String severity;
        if (data.getBatteryLevel() <= 5) {
            severity = "CRITICAL";
        } else if (data.getBatteryLevel() <= 10) {
            severity = "HIGH";
        } else {
            severity = "MEDIUM";
        }
        
        // Create alert
        Alert alert = new Alert();
        alert.setDeviceId(data.getDeviceId());
        alert.setTimestamp(data.getTimestamp());
        alert.setType("BATTERY");
        alert.setSeverity(severity);
        alert.setId(alertId);
        
        alert.setMessage(String.format("Battery level low: %.1f%%", data.getBatteryLevel()));
        
        // Add context - estimate remaining time based on recent drain rate
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minus(6, ChronoUnit.HOURS);
        List<GpsData> recentData = gpsDataService.getGpsDataForDevice(
                data.getDeviceId(), startTime, now);
        
        if (recentData.size() >= 5) {
            // Calculate average battery drain per hour
            recentData.sort(Comparator.comparing(GpsData::getTimestamp));
            
            GpsData oldest = recentData.get(0);
            double hoursSinceOldest = ChronoUnit.MINUTES.between(
                    oldest.getTimestamp(), now) / 60.0;
            
            if (hoursSinceOldest > 0) {
                double drainPercent = oldest.getBatteryLevel() - data.getBatteryLevel();
                double drainPerHour = drainPercent / hoursSinceOldest;
                
                if (drainPerHour > 0) {
                    double hoursRemaining = data.getBatteryLevel() / drainPerHour;
                    
                    if (hoursRemaining < 24) {
                        alert.setMessage(String.format(
                                "Battery level low: %.1f%% (est. %.1f hours remaining)",
                                data.getBatteryLevel(), hoursRemaining));
                    }
                }
            }
        }
        
        log.info("Generated battery alert for device {}: {}%", 
                data.getDeviceId(), Math.round(data.getBatteryLevel()));
        
        return alert;
    }
    
    private List<Alert> checkForAnomalies(GpsData data, AlertingConfiguration config) {
        // Skip if in quiet hours
        if (isInQuietHours(config)) {
            return Collections.emptyList();
        }
        
        // Get anomalies
        List<Anomaly> anomalies = predictionService.detectAnomalies(
                data.getDeviceId(), data);
        
        if (anomalies.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Filter based on sensitivity setting
        double sensitivityThreshold;
        switch (config.getAnomalySensitivity()) {
            case "HIGH":
                sensitivityThreshold = 0.6;
                break;
            case "LOW":
                sensitivityThreshold = 0.9;
                break;
            case "MEDIUM":
            default:
                sensitivityThreshold = 0.8;
        }
        
        List<Alert> alerts = new ArrayList<>();
        
        for (Anomaly anomaly : anomalies) {
            // Skip if confidence is below threshold
            if (anomaly.getConfidence() < sensitivityThreshold) {
                continue;
            }
            
            // Generate alert ID
            String alertId = String.format("anomaly_%s_%s_%s", 
                    data.getDeviceId(), 
                    anomaly.getType(),
                    data.getTimestamp().truncatedTo(ChronoUnit.HOURS));
            
            // Check if we've already sent this alert recently
            if (hasRecentlySentAlert(data.getDeviceId(), alertId)) {
                continue;
            }
            
            // Create alert
            Alert alert = new Alert();
            alert.setDeviceId(data.getDeviceId());
            alert.setTimestamp(data.getTimestamp());
            alert.setType("ANOMALY");
            alert.setId(alertId);
            
            // Set severity based on anomaly type and confidence
            if (anomaly.getConfidence() > 0.95) {
                alert.setSeverity("HIGH");
            } else if (anomaly.getConfidence() > 0.85) {
                alert.setSeverity("MEDIUM");
            } else {
                alert.setSeverity("LOW");
            }
            
            // Create user-friendly message
            switch (anomaly.getType()) {
                case "SPEED":
                    alert.setMessage("Unusual speed detected for this location/time");
                    break;
                case "ROUTE":
                    alert.setMessage("Unusual route deviation detected");
                    break;
                case "TIMING":
                    alert.setMessage("Unusual activity timing detected");
                    break;
                case "BEHAVIOR":
                    alert.setMessage("Unusual movement behavior detected");
                    break;
                default:
                    alert.setMessage("Anomaly detected: " + anomaly.getType());
            }
            
            alerts.add(alert);
            log.info("Generated anomaly alert for device {}: {}", 
                    data.getDeviceId(), alert.getMessage());
        }
        
        return alerts;
    }
    
    private boolean isInQuietHours(AlertingConfiguration config) {
        if (!config.isQuietHoursEnabled()) {
            return false;
        }
        
        int currentHour = LocalDateTime.now().getHour();
        
        if (config.getQuietHoursStart() <= config.getQuietHoursEnd()) {
            // Regular quiet hours (e.g., 22:00 - 07:00)
            return currentHour >= config.getQuietHoursStart() && 
                   currentHour < config.getQuietHoursEnd();
        } else {
            // Overnight quiet hours (e.g., 22:00 - 07:00)
            return currentHour >= config.getQuietHoursStart() || 
                   currentHour < config.getQuietHoursEnd();
        }
    }
    
    private boolean hasRecentlySentAlert(String deviceId, String alertId) {
        Set<String> recentAlerts = recentAlertsByDevice.computeIfAbsent(
                deviceId, k -> new HashSet<>());
        
        // Check if alert was already sent
        if (recentAlerts.contains(alertId)) {
            return true;
        }
        
        // Limit set size to prevent memory growth
        if (recentAlerts.size() > 100) {
            // Remove random elements to make space
            Iterator<String> iterator = recentAlerts.iterator();
            for (int i = 0; i < 20 && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
        
        // Record this alert
        recentAlerts.add(alertId);
        return false;
    }
    
    private void recordAlerts(String deviceId, List<Alert> alerts) {
        if (alerts.isEmpty()) {
            return;
        }
        
        Set<String> recentAlerts = recentAlertsByDevice.computeIfAbsent(
                deviceId, k -> new HashSet<>());
        
        for (Alert alert : alerts) {
            recentAlerts.add(alert.getId());
        }
    }
    
    private AlertRecommendation createDefaultRecommendation(String deviceId, 
                                                           AlertingConfiguration currentConfig) {
        AlertRecommendation recommendation = new AlertRecommendation();
        recommendation.setDeviceId(deviceId);
        recommendation.setShouldEnableSpeedAlerts(true);
        recommendation.setRecommendedSpeedThreshold(120);
        recommendation.setShouldEnableBatteryAlerts(true);
        recommendation.setRecommendedBatteryThreshold(15);
        recommendation.setShouldEnableGeofenceAlerts(true);
        recommendation.setShouldEnableAnomalyAlerts(false);
        recommendation.setRecommendedAnomalySensitivity("MEDIUM");
        recommendation.setShouldEnableQuietHours(false);
        recommendation.setRecommendedQuietHoursStart(22);
        recommendation.setRecommendedQuietHoursEnd(7);
        return recommendation;
    }
    
    // Data classes
    
    @Data
    public static class AlertingConfiguration {
        private String deviceId;
        private boolean enabled = true;
        
        // Speed alerts
        private boolean speedAlertsEnabled = true;
        private int speedThreshold = 120; // km/h
        
        // Battery alerts
        private boolean batteryAlertsEnabled = true;
        private int batteryThreshold = 15; // percent
        
        // Geofence alerts
        private boolean geofenceAlertsEnabled = true;
        
        // Anomaly detection
        private boolean anomalyAlertsEnabled = false;
        private String anomalySensitivity = "MEDIUM"; // LOW, MEDIUM, HIGH
        
        // Quiet hours
        private boolean quietHoursEnabled = false;
        private int quietHoursStart = 22; // 10 PM
        private int quietHoursEnd = 7;    // 7 AM
    }
    
    @Data
    public static class AlertRecommendation {
        private String deviceId;
        
        // Speed alerts
        private boolean shouldEnableSpeedAlerts;
        private int recommendedSpeedThreshold;
        
        // Battery alerts
        private boolean shouldEnableBatteryAlerts;
        private int recommendedBatteryThreshold;
        
        // Geofence alerts
        private boolean shouldEnableGeofenceAlerts;
        
        // Anomaly detection
        private boolean shouldEnableAnomalyAlerts;
        private String recommendedAnomalySensitivity; // LOW, MEDIUM, HIGH
        
        // Quiet hours
        private boolean shouldEnableQuietHours;
        private int recommendedQuietHoursStart;
        private int recommendedQuietHoursEnd;
    }
    
    @Data
    public static class Alert {
        private String id; // Unique identifier for this alert
        private String deviceId;
        private LocalDateTime timestamp;
        private String type; // SPEED, GEOFENCE, BATTERY, ANOMALY
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String message;
    }
} 