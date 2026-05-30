package com.gpstracker.service.impl;

import com.gpstracker.model.GpsData;
import com.gpstracker.model.Geofence;
import com.gpstracker.service.GeofenceService;
import com.gpstracker.service.GpsDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the GpsDataService that stores data in memory
 * Used when the application is in embedded mode without Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("embedded | test")
public class InMemoryGpsDataServiceImpl {

    private final GeofenceService geofenceService;

    // In-memory storage
    private final Map<String, GpsData> gpsDataStore = new ConcurrentHashMap<>();
    private final Map<String, GpsData> alertsStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> statsStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> geofenceStore = new ConcurrentHashMap<>();

    // Constants
    private static final String GPS_DATA_KEY_PREFIX = "gps:data:";
    private static final String ALERT_KEY_PREFIX = "gps:alert:";
    private static final String STATS_KEY_PREFIX = "gps:stats:";
    private static final String GEOFENCE_KEY_PREFIX = "gps:geofence:";
    private static final int DATA_RETENTION_DAYS = 90;

    /**
     * Save GPS data
     */
    public void saveGpsData(GpsData gpsData) {
        // Update device status
        updateDeviceStatus(gpsData);

        // Check for alerts
        checkAlerts(gpsData);

        // Check geofence
        checkGeofence(gpsData);

        // Save data
        String key = GPS_DATA_KEY_PREFIX + gpsData.getDeviceId() + ":" +
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        gpsDataStore.put(key, gpsData);

        // Update statistics
        updateStatistics(gpsData);

        log.debug("Saved GPS data for device {}: {}", gpsData.getDeviceId(), gpsData);
    }

    private void updateDeviceStatus(GpsData gpsData) {
        // Set low battery alert
        if (gpsData.getBatteryLevel() < GpsDataService.BATTERY_ALERT_THRESHOLD) {
            gpsData.setLowBattery(true);
        }

        // Set speed alert
        if (gpsData.getSpeed() > GpsDataService.SPEED_ALERT_THRESHOLD) {
            gpsData.setSpeedAlert(true);
        }

        // Check last activity time to determine if device was previously offline
        String lastKey = getLastDataKey(gpsData.getDeviceId());
        if (lastKey != null) {
            GpsData lastData = gpsDataStore.get(lastKey);
            if (lastData != null && "OFFLINE".equals(lastData.getDeviceStatus())) {
                // Update device status to ACTIVE if it was offline
                gpsData.setDeviceStatus("ACTIVE");
            }
        } else {
            // First data point for this device
            gpsData.setDeviceStatus("ACTIVE");
        }
    }

    private void checkAlerts(GpsData gpsData) {
        // Check for alert conditions
        gpsData.setLowBattery(gpsData.getBatteryLevel() < GpsDataService.BATTERY_ALERT_THRESHOLD);
        gpsData.setSpeedAlert(gpsData.getSpeed() > GpsDataService.SPEED_ALERT_THRESHOLD);

        // If any alerts are triggered, save to alerts collection
        if (gpsData.isLowBattery() || gpsData.isSpeedAlert() ||
                gpsData.isGeofenceAlert() || gpsData.isMalfunctionAlert()) {

            String alertKey = ALERT_KEY_PREFIX + gpsData.getDeviceId() + ":" +
                    gpsData.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            alertsStore.put(alertKey, gpsData);

            log.info("Alert triggered for device {}: {}", gpsData.getDeviceId(), gpsData);
        }
    }

    private void checkGeofence(GpsData gpsData) {
        try {
            // Use the GeofenceService to check if the current position is within any
            // geofence
            List<Geofence> triggeredGeofences = geofenceService.checkGeofences(
                    gpsData.getDeviceId(),
                    gpsData.getLatitude(),
                    gpsData.getLongitude());

            // Set geofence alert based on the highest alert level found
            if (!triggeredGeofences.isEmpty()) {
                // Find the highest alert level among triggered geofences
                int highestAlertLevel = triggeredGeofences.stream()
                        .mapToInt(Geofence::getAlertLevel)
                        .max()
                        .orElse(0);

                // Set metadata about triggered geofences
                if (gpsData.getMetadata() == null) {
                    gpsData.setMetadata(new HashMap<>());
                }

                List<String> geofenceNames = triggeredGeofences.stream()
                        .map(Geofence::getName)
                        .collect(Collectors.toList());

                gpsData.getMetadata().put("triggeredGeofences", geofenceNames);
                gpsData.getMetadata().put("highestGeofenceAlertLevel", highestAlertLevel);

                // Set the alert flag if alert level is 2 (warning) or 3 (critical)
                gpsData.setGeofenceAlert(highestAlertLevel >= 2);

                log.debug("Device {} triggered geofences: {}", gpsData.getDeviceId(), geofenceNames);
            } else {
                gpsData.setGeofenceAlert(false);
            }
        } catch (Exception e) {
            log.error("Error checking geofences for device {}: {}", gpsData.getDeviceId(), e.getMessage());
            // Fall back to legacy geofence checking
            legacyCheckGeofence(gpsData);
        }
    }

    // Legacy geofence checking method
    private void legacyCheckGeofence(GpsData gpsData) {
        String geofenceKey = GEOFENCE_KEY_PREFIX + gpsData.getDeviceId();
        Map<String, Object> geofenceData = geofenceStore.get(geofenceKey);

        if (geofenceData != null && !geofenceData.isEmpty()) {
            double centerLat = Double.parseDouble(geofenceData.get("centerLat").toString());
            double centerLon = Double.parseDouble(geofenceData.get("centerLon").toString());
            double radius = Double.parseDouble(geofenceData.get("radius").toString());

            double distance = calculateDistance(
                    centerLat, centerLon,
                    gpsData.getLatitude(), gpsData.getLongitude());

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
        String dateStr = gpsData.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String statsKey = STATS_KEY_PREFIX + gpsData.getDeviceId() + ":" + dateStr;

        // Calculate additional distance
        double additionalDistance = calculateDistanceFromLast(gpsData);

        // Get or create stats map
        Map<String, Object> stats = statsStore.computeIfAbsent(statsKey, k -> new HashMap<>());

        // Update total distance
        double currentDistance = Optional.ofNullable(stats.get("totalDistance"))
                .map(val -> Double.parseDouble(val.toString()))
                .orElse(0.0);

        stats.put("totalDistance", currentDistance + additionalDistance);
        stats.put("lastSpeed", gpsData.getSpeed());
        stats.put("lastBatteryLevel", gpsData.getBatteryLevel());
        stats.put("lastUpdated", LocalDateTime.now().toString());
    }

    private double calculateDistanceFromLast(GpsData gpsData) {
        String lastKey = getLastDataKey(gpsData.getDeviceId());
        if (lastKey != null) {
            GpsData lastData = gpsDataStore.get(lastKey);
            if (lastData != null) {
                return calculateDistance(
                        lastData.getLatitude(), lastData.getLongitude(),
                        gpsData.getLatitude(), gpsData.getLongitude());
            }
        }
        return 0.0;
    }

    private String getLastDataKey(String deviceId) {
        return gpsDataStore.keySet().stream()
                .filter(k -> k.startsWith(GPS_DATA_KEY_PREFIX + deviceId + ":"))
                .max(String::compareTo)
                .orElse(null);
    }

    /**
     * Get device statistics
     */
    public Map<String, Object> getDeviceStatistics(String deviceId, LocalDateTime date) {
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String statsKey = STATS_KEY_PREFIX + deviceId + ":" + dateStr;

        return statsStore.getOrDefault(statsKey, new HashMap<>());
    }

    /**
     * Get GPS data for a device within a time range
     */
    public List<GpsData> getGpsDataForDevice(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        String startTimeStr = startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTimeStr = endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return gpsDataStore.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    if (!key.startsWith(GPS_DATA_KEY_PREFIX + deviceId + ":")) {
                        return false;
                    }

                    String timeStr = key.substring((GPS_DATA_KEY_PREFIX + deviceId + ":").length());
                    return timeStr.compareTo(startTimeStr) >= 0 && timeStr.compareTo(endTimeStr) <= 0;
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Get alerts for a device within a time range
     */
    public List<GpsData> getAlerts(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        String startTimeStr = startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTimeStr = endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return alertsStore.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    if (!key.startsWith(ALERT_KEY_PREFIX + deviceId + ":")) {
                        return false;
                    }

                    String timeStr = key.substring((ALERT_KEY_PREFIX + deviceId + ":").length());
                    return timeStr.compareTo(startTimeStr) >= 0 && timeStr.compareTo(endTimeStr) <= 0;
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Set a legacy geofence
     */
    public void setGeofence(String deviceId, double centerLat, double centerLon, double radius) {
        // Call the new method to create a circle geofence
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("autoGenerated", true);
        geofenceService.createCircleGeofence(
                deviceId,
                "Auto-fence",
                "Auto-generated fence for " + deviceId,
                centerLat,
                centerLon,
                radius * 1000, // Convert km to meters
                "1",
                "auto-fence",
                metadata);

        // Also maintain the legacy format
        String geofenceKey = GEOFENCE_KEY_PREFIX + deviceId;
        Map<String, Object> geofence = new HashMap<>();
        geofence.put("centerLat", centerLat);
        geofence.put("centerLon", centerLon);
        geofence.put("radius", radius);

        geofenceStore.put(geofenceKey, geofence);
    }

    /**
     * Scheduled daily purge of old data based on retention policy
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run at 1:00 AM every day
    public void purgeOldData() {
        log.info("Starting periodic data purge (Retention: {} days)", DATA_RETENTION_DAYS);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(DATA_RETENTION_DAYS);
        String cutoffStr = cutoff.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Purge GPS data
        int gpsPurged = purgeStore(gpsDataStore, GPS_DATA_KEY_PREFIX, cutoffStr);

        // Purge Alerts
        int alertsPurged = purgeStore(alertsStore, ALERT_KEY_PREFIX, cutoffStr);

        log.info("Purge completed: {} GPS points and {} alerts removed", gpsPurged, alertsPurged);
    }

    private int purgeStore(Map<String, GpsData> store, String prefix, String cutoffStr) {
        List<String> keysToPurge = store.keySet().stream()
                .filter(key -> {
                    if (!key.startsWith(prefix))
                        return false;
                    String timestamp = key.substring(key.lastIndexOf(":") + 1);
                    return timestamp.compareTo(cutoffStr) < 0;
                })
                .collect(Collectors.toList());

        keysToPurge.forEach(store::remove);
        return keysToPurge.size();
    }

    /**
     * Scheduled weekly export (placeholder)
     */
    public void weeklyExport() {
        log.info("Weekly GPS data export triggered (InMemory implementation)");
        // Logic handled by ExportService
    }
}