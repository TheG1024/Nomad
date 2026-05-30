package com.gpstracker.service.ai;

import com.gpstracker.model.GpsData;
import com.gpstracker.service.GpsDataService;
import com.gpstracker.service.ai.PatternLearningService.DevicePattern;
import com.gpstracker.service.ai.PatternLearningService.Location;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI-driven service that optimizes battery usage by adapting device tracking settings
 * based on movement patterns, activity levels, and predicted routes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatteryOptimizationService {

    private final GpsDataService gpsDataService;
    private final PatternLearningService patternLearningService;
    private final PredictionService predictionService;
    
    // Cache of device optimization profiles
    private final Map<String, DeviceOptimizationProfile> deviceProfiles = new ConcurrentHashMap<>();
    
    // Cache of location-specific polling rates
    private final Map<String, Map<String, PollingRate>> locationPollingRates = new ConcurrentHashMap<>();
    
    /**
     * Get the optimal polling interval for a device at a specific time and location
     * 
     * @param deviceId Device ID
     * @param latitude Current latitude
     * @param longitude Current longitude
     * @param batteryLevel Current battery percentage (0-100)
     * @param isMoving Whether the device is currently moving (speed > 0)
     * @return Optimal polling interval in seconds
     */
    public int getOptimalPollingInterval(String deviceId, double latitude, double longitude, 
                                        double batteryLevel, boolean isMoving) {
        
        DeviceOptimizationProfile profile = getOrCreateDeviceProfile(deviceId);
        
        // Check if we're in a known location with a specific polling rate
        String locationKey = getLocationKey(latitude, longitude);
        PollingRate locationRate = getLocationPollingRate(deviceId, locationKey);
        
        // Base polling interval depends on several factors
        int baseInterval;
        
        // 1. If we have a location-specific rate, use it
        if (locationRate != null) {
            baseInterval = locationRate.getIntervalSeconds();
            log.debug("Using location-specific polling rate of {} seconds for device {} at {}", 
                baseInterval, deviceId, locationKey);
        } 
        // 2. Otherwise use activity-based rates
        else if (isMoving) {
            // Moving - use movement mode
            baseInterval = profile.getMovingPollingInterval();
            log.debug("Device {} is moving, using movement polling rate of {} seconds", 
                deviceId, baseInterval);
        } else {
            // Stationary - use stationary mode
            baseInterval = profile.getStationaryPollingInterval();
            log.debug("Device {} is stationary, using stationary polling rate of {} seconds", 
                deviceId, baseInterval);
        }
        
        // Apply battery-level adjustment (increase interval when battery is low)
        if (batteryLevel < 20) {
            // Critically low battery - significantly reduce polling
            return Math.max(300, baseInterval * 3); // At least 5 minutes
        } else if (batteryLevel < 40) {
            // Low battery - reduce polling
            return Math.max(120, baseInterval * 2); // At least 2 minutes
        }
        
        // Check if we should apply time-based optimization
        return applyTimeBasedOptimization(deviceId, baseInterval);
    }
    
    /**
     * Recommend optimal tracking settings for a device based on usage patterns
     * 
     * @param deviceId Device ID to generate recommendations for
     * @return Device optimization settings
     */
    public DeviceOptimizationRecommendation getOptimizationRecommendations(String deviceId) {
        log.info("Generating optimization recommendations for device {}", deviceId);
        
        DeviceOptimizationProfile profile = getOrCreateDeviceProfile(deviceId);
        DevicePattern pattern = patternLearningService.getPatternForDevice(deviceId);
        
        // Analyze tracking data
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minus(14, ChronoUnit.DAYS);
        List<GpsData> recentData = gpsDataService.getGpsDataForDevice(deviceId, startTime, now);
        
        if (recentData.isEmpty() || pattern == null) {
            log.warn("Insufficient data for device {} to generate meaningful recommendations", deviceId);
            return createDefaultRecommendation(deviceId);
        }
        
        // Analyze battery usage patterns
        Map<Integer, Double> batteryDrainByHour = calculateBatteryDrainByHour(recentData);
        
        // Find typical sleep hours (no movement)
        List<Integer> inactiveHours = findInactiveHours(recentData);
        
        // Count total points collected and calculate average intervals
        double averagePointsPerDay = (double) recentData.size() / 14;
        long idealPointsPerDay = calculateIdealPointsPerDay(pattern);
        
        // Generate recommendations
        DeviceOptimizationRecommendation recommendation = new DeviceOptimizationRecommendation();
        recommendation.setDeviceId(deviceId);
        
        // Moving interval recommendation
        int recommendedMovingInterval = determineOptimalMovingInterval(
                recentData, pattern, idealPointsPerDay, batteryDrainByHour);
        recommendation.setRecommendedMovingPollingInterval(recommendedMovingInterval);
        
        // Stationary interval recommendation
        int recommendedStationaryInterval = determineOptimalStationaryInterval(
                recentData, pattern, idealPointsPerDay, batteryDrainByHour);
        recommendation.setRecommendedStationaryPollingInterval(recommendedStationaryInterval);
        
        // Sleep mode recommendation
        if (!inactiveHours.isEmpty()) {
            recommendation.setHasSleepHoursRecommendation(true);
            recommendation.setSleepHoursStart(inactiveHours.get(0));
            recommendation.setSleepHoursEnd(inactiveHours.get(inactiveHours.size() - 1) + 1);
            recommendation.setRecommendedSleepPollingInterval(Math.max(900, recommendedStationaryInterval * 4));
        }
        
        // Location-specific recommendations
        List<LocationOptimizationRecommendation> locationRecommendations = 
                generateLocationOptimizationRecommendations(deviceId, pattern, recentData);
        recommendation.setLocationRecommendations(locationRecommendations);
        
        // Calculate estimated battery savings
        double currentDailyPoints = averagePointsPerDay;
        double recommendedDailyPoints = 
                (24 - inactiveHours.size()) * (12.0 / recommendedMovingInterval + 12.0 / recommendedStationaryInterval);
        double estimatedSavings = 1.0 - (recommendedDailyPoints / currentDailyPoints);
        recommendation.setEstimatedBatterySavingsPercent((int) Math.round(estimatedSavings * 100));
        
        log.info("Generated optimization recommendations for device {}: moving={}s, stationary={}s, savings={}%",
                deviceId, recommendedMovingInterval, recommendedStationaryInterval, 
                recommendation.getEstimatedBatterySavingsPercent());
        
        return recommendation;
    }
    
    /**
     * Apply optimization recommendations
     * 
     * @param deviceId Device ID to apply recommendations to
     * @param applyMoving Whether to apply moving interval recommendation
     * @param applyStationary Whether to apply stationary interval recommendation
     * @param applySleep Whether to apply sleep mode recommendation
     * @param applyLocationBased Whether to apply location-based recommendations
     * @return True if settings were applied successfully
     */
    public boolean applyOptimizationRecommendations(String deviceId, boolean applyMoving, 
                                                  boolean applyStationary, boolean applySleep,
                                                  boolean applyLocationBased) {
        log.info("Applying optimization recommendations for device {}", deviceId);
        
        DeviceOptimizationRecommendation recommendations = getOptimizationRecommendations(deviceId);
        DeviceOptimizationProfile profile = getOrCreateDeviceProfile(deviceId);
        
        if (applyMoving) {
            profile.setMovingPollingInterval(recommendations.getRecommendedMovingPollingInterval());
        }
        
        if (applyStationary) {
            profile.setStationaryPollingInterval(recommendations.getRecommendedStationaryPollingInterval());
        }
        
        if (applySleep && recommendations.isHasSleepHoursRecommendation()) {
            profile.setSleepModeEnabled(true);
            profile.setSleepModeStartHour(recommendations.getSleepHoursStart());
            profile.setSleepModeEndHour(recommendations.getSleepHoursEnd());
            profile.setSleepModePollingInterval(recommendations.getRecommendedSleepPollingInterval());
        }
        
        if (applyLocationBased) {
            Map<String, PollingRate> locationRates = locationPollingRates.computeIfAbsent(deviceId, k -> new HashMap<>());
            
            for (LocationOptimizationRecommendation locRec : recommendations.getLocationRecommendations()) {
                String locationKey = getLocationKey(locRec.getLatitude(), locRec.getLongitude());
                
                locationRates.put(locationKey, new PollingRate(
                        locRec.getRecommendedPollingInterval(),
                        200.0, // 200 meter radius
                        locRec.getLocationType()
                ));
            }
        }
        
        // Save the updated profile
        deviceProfiles.put(deviceId, profile);
        log.info("Applied optimization settings for device {}", deviceId);
        
        return true;
    }
    
    /**
     * Update device optimization profiles on a schedule
     */
    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    public void updateAllDeviceProfiles() {
        log.info("Starting scheduled update of device optimization profiles");
        
        // Get a list of known devices (in production, this would come from a device registry)
        Set<String> knownDevices = new HashSet<>(deviceProfiles.keySet());
        
        // Ensure we have at least our test devices
        if (knownDevices.isEmpty()) {
            knownDevices.add("device123");
            knownDevices.add("device456");
        }
        
        // Update each device profile
        for (String deviceId : knownDevices) {
            try {
                DeviceOptimizationRecommendation recommendation = getOptimizationRecommendations(deviceId);
                DeviceOptimizationProfile profile = deviceProfiles.get(deviceId);
                
                // Only apply recommendations if the savings are significant (>10%)
                if (recommendation.getEstimatedBatterySavingsPercent() > 10) {
                    // Auto-apply recommendations
                    applyOptimizationRecommendations(deviceId, true, true, true, true);
                }
            } catch (Exception e) {
                log.error("Error updating optimization profile for device {}", deviceId, e);
            }
        }
        
        log.info("Completed scheduled update of device optimization profiles");
    }
    
    // Helper methods
    
    private DeviceOptimizationProfile getOrCreateDeviceProfile(String deviceId) {
        return deviceProfiles.computeIfAbsent(deviceId, id -> {
            log.info("Creating new optimization profile for device {}", id);
            DeviceOptimizationProfile profile = new DeviceOptimizationProfile();
            profile.setDeviceId(id);
            profile.setMovingPollingInterval(30); // 30 seconds default for moving
            profile.setStationaryPollingInterval(300); // 5 minutes default for stationary
            profile.setSleepModeEnabled(false);
            return profile;
        });
    }
    
    private PollingRate getLocationPollingRate(String deviceId, String locationKey) {
        Map<String, PollingRate> deviceLocationRates = locationPollingRates.get(deviceId);
        if (deviceLocationRates == null) return null;
        
        return deviceLocationRates.get(locationKey);
    }
    
    private String getLocationKey(double latitude, double longitude) {
        // Create a grid-based key (approximately 100 meters)
        return String.format("%.4f_%.4f", 
                Math.round(latitude * 10000) / 10000.0,
                Math.round(longitude * 10000) / 10000.0);
    }
    
    private int applyTimeBasedOptimization(String deviceId, int baseInterval) {
        DeviceOptimizationProfile profile = getOrCreateDeviceProfile(deviceId);
        
        // Apply sleep mode if enabled and current time is within sleep hours
        if (profile.isSleepModeEnabled()) {
            LocalDateTime now = LocalDateTime.now();
            int currentHour = now.getHour();
            
            boolean isInSleepHours;
            if (profile.getSleepModeStartHour() <= profile.getSleepModeEndHour()) {
                // Regular sleep pattern (e.g., 23:00 - 07:00)
                isInSleepHours = currentHour >= profile.getSleepModeStartHour() && 
                                currentHour < profile.getSleepModeEndHour();
            } else {
                // Overnight sleep pattern (e.g., 23:00 - 07:00)
                isInSleepHours = currentHour >= profile.getSleepModeStartHour() || 
                                currentHour < profile.getSleepModeEndHour();
            }
            
            if (isInSleepHours) {
                log.debug("Device {} is in sleep mode, using sleep polling rate of {} seconds", 
                    deviceId, profile.getSleepModePollingInterval());
                return profile.getSleepModePollingInterval();
            }
        }
        
        return baseInterval;
    }
    
    private Map<Integer, Double> calculateBatteryDrainByHour(List<GpsData> data) {
        Map<Integer, List<Double>> batteryLevelsByHour = new HashMap<>();
        Map<Integer, Double> batteryDrainByHour = new HashMap<>();
        
        // Group battery levels by hour
        for (GpsData point : data) {
            int hour = point.getTimestamp().getHour();
            batteryLevelsByHour.computeIfAbsent(hour, k -> new ArrayList<>()).add(point.getBatteryLevel());
        }
        
        // Calculate average drain by comparing consecutive hours
        for (int hour = 0; hour < 24; hour++) {
            int nextHour = (hour + 1) % 24;
            
            List<Double> currentHourLevels = batteryLevelsByHour.get(hour);
            List<Double> nextHourLevels = batteryLevelsByHour.get(nextHour);
            
            if (currentHourLevels != null && !currentHourLevels.isEmpty() && 
                nextHourLevels != null && !nextHourLevels.isEmpty()) {
                
                double currentAvg = currentHourLevels.stream().mapToDouble(d -> d).average().orElse(0);
                double nextAvg = nextHourLevels.stream().mapToDouble(d -> d).average().orElse(0);
                
                // Calculate drain (positive value means battery decreasing)
                double drain = currentAvg - nextAvg;
                if (drain > 0) {
                    batteryDrainByHour.put(hour, drain);
                }
            }
        }
        
        return batteryDrainByHour;
    }
    
    private List<Integer> findInactiveHours(List<GpsData> data) {
        // Count movements by hour
        Map<Integer, Integer> movementCountByHour = new HashMap<>();
        
        for (int i = 0; i < data.size(); i++) {
            GpsData point = data.get(i);
            
            if (point.getSpeed() > 0.5) {
                int hour = point.getTimestamp().getHour();
                movementCountByHour.merge(hour, 1, Integer::sum);
            }
        }
        
        // Find hours with minimal movement
        List<Integer> inactiveHours = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            int count = movementCountByHour.getOrDefault(hour, 0);
            if (count < 10) { // Very few movements
                inactiveHours.add(hour);
            }
        }
        
        // Find consecutive ranges of inactive hours
        List<Integer> result = new ArrayList<>();
        if (!inactiveHours.isEmpty()) {
            Collections.sort(inactiveHours);
            
            // Find the longest consecutive range that includes hours between 22-06
            List<Integer> longestRange = new ArrayList<>();
            List<Integer> currentRange = new ArrayList<>();
            
            for (int i = 0; i < inactiveHours.size(); i++) {
                int hour = inactiveHours.get(i);
                
                if (currentRange.isEmpty() || hour == currentRange.get(currentRange.size() - 1) + 1 ||
                    (currentRange.get(currentRange.size() - 1) == 23 && hour == 0)) {
                    currentRange.add(hour);
                } else {
                    if (currentRange.size() > longestRange.size() && 
                        (currentRange.contains(23) || currentRange.contains(0) || 
                         currentRange.contains(1) || currentRange.contains(2))) {
                        longestRange = new ArrayList<>(currentRange);
                    }
                    currentRange.clear();
                    currentRange.add(hour);
                }
            }
            
            if (currentRange.size() > longestRange.size() && 
                (currentRange.contains(23) || currentRange.contains(0) || 
                 currentRange.contains(1) || currentRange.contains(2))) {
                longestRange = currentRange;
            }
            
            result = longestRange;
        }
        
        return result;
    }
    
    private long calculateIdealPointsPerDay(DevicePattern pattern) {
        // This is a simplified heuristic that could be more sophisticated in a real implementation
        // It estimates how many GPS points are needed per day based on the user's activity patterns
        
        // Base number of points needed per day
        double basePoints = 720; // One point every two minutes on average
        
        // Adjust based on number of frequent locations
        int locationCount = pattern.getFrequentLocations().size();
        double locationFactor = 1.0 + (locationCount * 0.1); // More locations = more tracking needed
        
        // Adjust based on active hours
        long activeHours = pattern.getRoutines().stream()
                .filter(r -> r.getFirstActivityTime() != null && r.getLastActivityTime() != null)
                .mapToLong(r -> {
                    LocalTime first = r.getFirstActivityTime();
                    LocalTime last = r.getLastActivityTime();
                    return ChronoUnit.HOURS.between(first, last);
                })
                .average()
                .orElse(16); // Default to 16 active hours
        
        double hourFactor = activeHours / 16.0;
        
        return (long)(basePoints * locationFactor * hourFactor);
    }
    
    private int determineOptimalMovingInterval(List<GpsData> data, DevicePattern pattern, 
                                             long idealPointsPerDay, Map<Integer, Double> batteryDrainByHour) {
        // Calculate average speed when moving
        double avgSpeed = data.stream()
                .filter(d -> d.getSpeed() > 1.0)
                .mapToDouble(GpsData::getSpeed)
                .average()
                .orElse(5.0) * 3.6; // Convert to km/h
        
        // More speed = need more frequent updates
        int baseInterval;
        if (avgSpeed > 80) {
            baseInterval = 15; // Highway speeds - 15 seconds
        } else if (avgSpeed > 40) {
            baseInterval = 20; // Urban driving - 20 seconds
        } else if (avgSpeed > 15) {
            baseInterval = 30; // Slow urban traffic - 30 seconds
        } else {
            baseInterval = 60; // Walking/biking - 60 seconds
        }
        
        // Check battery drain patterns
        if (!batteryDrainByHour.isEmpty()) {
            double avgDrain = batteryDrainByHour.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(1.0);
            
            if (avgDrain > 3.0) {
                // High battery drain - increase interval
                baseInterval = (int) (baseInterval * 1.5);
            } else if (avgDrain < 0.5) {
                // Low battery drain - can decrease interval
                baseInterval = (int) (baseInterval * 0.8);
            }
        }
        
        return baseInterval;
    }
    
    private int determineOptimalStationaryInterval(List<GpsData> data, DevicePattern pattern, 
                                                 long idealPointsPerDay, Map<Integer, Double> batteryDrainByHour) {
        // Base interval depends on how long user typically stays in one place
        Map<String, List<GpsData>> staysByLocation = identifyStationaryPeriods(data);
        
        // Calculate median stay duration
        List<Long> stayDurations = staysByLocation.values().stream()
                .filter(list -> list.size() >= 2) // Need at least 2 points to calculate duration
                .map(list -> {
                    LocalDateTime start = list.get(0).getTimestamp();
                    LocalDateTime end = list.get(list.size() - 1).getTimestamp();
                    return ChronoUnit.MINUTES.between(start, end);
                })
                .filter(duration -> duration > 0)
                .collect(Collectors.toList());
        
        long medianStayDuration = 60; // Default to 1 hour
        if (!stayDurations.isEmpty()) {
            Collections.sort(stayDurations);
            medianStayDuration = stayDurations.get(stayDurations.size() / 2);
        }
        
        // Calculate base interval based on median stay duration
        int baseInterval;
        if (medianStayDuration > 240) {
            baseInterval = 600; // Very long stays (>4h) - 10 minutes
        } else if (medianStayDuration > 120) {
            baseInterval = 450; // Long stays (2-4h) - 7.5 minutes
        } else if (medianStayDuration > 60) {
            baseInterval = 300; // Medium stays (1-2h) - 5 minutes
        } else if (medianStayDuration > 30) {
            baseInterval = 180; // Short stays (30m-1h) - 3 minutes
        } else {
            baseInterval = 120; // Very short stays (<30m) - 2 minutes
        }
        
        // Adjust based on battery drain
        if (!batteryDrainByHour.isEmpty()) {
            double avgDrain = batteryDrainByHour.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(1.0);
            
            if (avgDrain > 2.0) {
                // High battery drain - increase interval
                baseInterval = (int) (baseInterval * 1.5);
            } else if (avgDrain < 0.5 && baseInterval > 180) {
                // Low battery drain - can decrease interval slightly
                baseInterval = (int) (baseInterval * 0.9);
            }
        }
        
        return baseInterval;
    }
    
    private Map<String, List<GpsData>> identifyStationaryPeriods(List<GpsData> data) {
        Map<String, List<GpsData>> stationaryPeriods = new HashMap<>();
        
        List<GpsData> currentPeriod = new ArrayList<>();
        String currentLocationKey = null;
        
        // Sort by timestamp
        List<GpsData> sortedData = new ArrayList<>(data);
        sortedData.sort(Comparator.comparing(GpsData::getTimestamp));
        
        for (GpsData point : sortedData) {
            String locationKey = getLocationKey(point.getLatitude(), point.getLongitude());
            
            if (currentLocationKey == null) {
                // First point
                currentLocationKey = locationKey;
                currentPeriod.add(point);
            } else if (locationKey.equals(currentLocationKey)) {
                // Still at same location
                currentPeriod.add(point);
            } else {
                // Location changed
                if (currentPeriod.size() >= 3) {
                    // Only consider periods with at least 3 points
                    stationaryPeriods.put(currentLocationKey, new ArrayList<>(currentPeriod));
                }
                currentLocationKey = locationKey;
                currentPeriod.clear();
                currentPeriod.add(point);
            }
        }
        
        // Add the last period
        if (currentPeriod.size() >= 3) {
            stationaryPeriods.put(currentLocationKey, currentPeriod);
        }
        
        return stationaryPeriods;
    }
    
    private List<LocationOptimizationRecommendation> generateLocationOptimizationRecommendations(
            String deviceId, DevicePattern pattern, List<GpsData> recentData) {
        
        List<LocationOptimizationRecommendation> recommendations = new ArrayList<>();
        
        // Add home location recommendation
        if (pattern.getHomeLocation() != null) {
            recommendations.add(new LocationOptimizationRecommendation(
                    pattern.getHomeLocation().getLatitude(),
                    pattern.getHomeLocation().getLongitude(),
                    600, // 10 minutes at home
                    "HOME"
            ));
        }
        
        // Add work location recommendation
        if (pattern.getWorkLocation() != null) {
            recommendations.add(new LocationOptimizationRecommendation(
                    pattern.getWorkLocation().getLatitude(),
                    pattern.getWorkLocation().getLongitude(),
                    300, // 5 minutes at work
                    "WORK"
            ));
        }
        
        // Add frequent locations with appropriate intervals
        for (Location location : pattern.getFrequentLocations()) {
            if ("HOME".equals(location.getLocationType()) || "WORK".equals(location.getLocationType())) {
                continue; // Skip home/work (already added)
            }
            
            int recommendedInterval;
            switch (location.getLocationType()) {
                case "LEISURE":
                    recommendedInterval = 240; // 4 minutes
                    break;
                default:
                    recommendedInterval = 180; // 3 minutes
            }
            
            recommendations.add(new LocationOptimizationRecommendation(
                    location.getLatitude(),
                    location.getLongitude(),
                    recommendedInterval,
                    location.getLocationType()
            ));
        }
        
        return recommendations;
    }
    
    private DeviceOptimizationRecommendation createDefaultRecommendation(String deviceId) {
        DeviceOptimizationRecommendation recommendation = new DeviceOptimizationRecommendation();
        recommendation.setDeviceId(deviceId);
        recommendation.setRecommendedMovingPollingInterval(30);
        recommendation.setRecommendedStationaryPollingInterval(300);
        recommendation.setHasSleepHoursRecommendation(false);
        recommendation.setEstimatedBatterySavingsPercent(0);
        recommendation.setLocationRecommendations(Collections.emptyList());
        return recommendation;
    }
    
    // Data classes
    
    @Data
    public static class DeviceOptimizationProfile {
        private String deviceId;
        private int movingPollingInterval = 30; // seconds
        private int stationaryPollingInterval = 300; // seconds
        private boolean sleepModeEnabled = false;
        private int sleepModeStartHour = 22; // 10 PM
        private int sleepModeEndHour = 7;   // 7 AM
        private int sleepModePollingInterval = 1800; // 30 minutes
    }
    
    @Data
    private static class PollingRate {
        private final int intervalSeconds;
        private final double radiusMeters;
        private final String locationType;
    }
    
    @Data
    public static class DeviceOptimizationRecommendation {
        private String deviceId;
        private int recommendedMovingPollingInterval;
        private int recommendedStationaryPollingInterval;
        private boolean hasSleepHoursRecommendation;
        private int sleepHoursStart;
        private int sleepHoursEnd;
        private int recommendedSleepPollingInterval;
        private int estimatedBatterySavingsPercent;
        private List<LocationOptimizationRecommendation> locationRecommendations = new ArrayList<>();
    }
    
    @Data
    public static class LocationOptimizationRecommendation {
        private final double latitude;
        private final double longitude;
        private final int recommendedPollingInterval;
        private final String locationType;
    }
} 