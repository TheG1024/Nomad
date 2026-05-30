package com.gpstracker.service.ai;

import com.gpstracker.model.GpsData;
import com.gpstracker.service.GpsDataService;
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
import java.util.stream.Collectors;

/**
 * Service that applies machine learning techniques to learn user movement
 * patterns
 * Used as a foundation for other AI features like recommendations, predictions,
 * and optimizations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatternLearningService {

    private final GpsDataService gpsDataService;

    // Cache of learned patterns by device ID
    private final Map<String, DevicePattern> devicePatterns = new HashMap<>();

    // Cache of recently updated devices to avoid redundant processing
    private final Set<String> recentlyProcessedDevices = new HashSet<>();

    /**
     * Update patterns for a specific device
     * 
     * @param deviceId ID of the device to analyze
     * @return The updated pattern
     */
    public DevicePattern updatePatternForDevice(String deviceId) {
        if (recentlyProcessedDevices.contains(deviceId)) {
            log.debug("Device {} was recently processed, skipping", deviceId);
            return devicePatterns.get(deviceId);
        }

        log.info("Updating movement patterns for device {}", deviceId);

        // Get 30 days of historical data
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minus(30, ChronoUnit.DAYS);
        List<GpsData> historicalData = gpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);

        if (historicalData.isEmpty()) {
            log.warn("No historical data found for device {}", deviceId);
            return null;
        }

        DevicePattern pattern = devicePatterns.computeIfAbsent(deviceId, k -> new DevicePattern());
        pattern.setDeviceId(deviceId);

        // Update pattern with historical data
        pattern.setHomeLocation(findHomeLocation(historicalData));
        pattern.setWorkLocation(findWorkLocation(historicalData));
        pattern.setFrequentLocations(findFrequentLocations(historicalData));
        pattern.setRoutines(findDailyRoutines(historicalData));
        pattern.setLastUpdated(LocalDateTime.now());

        // Add to recently processed set with 1-hour expiry
        recentlyProcessedDevices.add(deviceId);
        scheduleRemovalFromRecentlyProcessed(deviceId);

        return pattern;
    }

    /**
     * Get the current pattern for a device
     * Updates pattern if it doesn't exist or is outdated
     */
    public DevicePattern getPatternForDevice(String deviceId) {
        DevicePattern pattern = devicePatterns.get(deviceId);

        // If pattern doesn't exist or is outdated (older than 24 hours), update it
        if (pattern == null || pattern.getLastUpdated().isBefore(LocalDateTime.now().minus(24, ChronoUnit.HOURS))) {
            pattern = updatePatternForDevice(deviceId);
        }

        return pattern;
    }

    /**
     * Clears all cached device patterns and the recently-processed set.
     * Useful for testing and for forcing a full re-analysis.
     */
    public void clearPatterns() {
        devicePatterns.clear();
        recentlyProcessedDevices.clear();
        log.info("Cleared all cached device patterns");
    }

    /**
     * Process patterns for all devices on a schedule
     */
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    public void updateAllDevicePatterns() {
        log.info("Starting scheduled update of all device patterns");

        // In a real implementation, this would fetch from a device registry
        // For now, we'll use the keys from the existing patterns map
        Set<String> deviceIds = new HashSet<>(devicePatterns.keySet());

        // Add some sample device IDs for testing if map is empty
        if (deviceIds.isEmpty()) {
            deviceIds.add("device123");
            deviceIds.add("device456");
        }

        for (String deviceId : deviceIds) {
            try {
                updatePatternForDevice(deviceId);
            } catch (Exception e) {
                log.error("Error updating pattern for device {}", deviceId, e);
            }
        }

        log.info("Completed scheduled update of all device patterns");
    }

    // Helper methods

    private void scheduleRemovalFromRecentlyProcessed(final String deviceId) {
        // In a production implementation, this would use a scheduled task or expiring
        // cache
        // For simplicity, we're using a basic approach here
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                recentlyProcessedDevices.remove(deviceId);
            }
        }, 3600000); // 1 hour in milliseconds
    }

    private Location findHomeLocation(List<GpsData> data) {
        // Home is typically where the device spends most time during night hours
        List<GpsData> nightData = data.stream()
                .filter(d -> {
                    int hour = d.getTimestamp().getHour();
                    return hour >= 22 || hour <= 6; // Between 10 PM and 6 AM
                })
                .collect(Collectors.toList());

        return findMostFrequentLocation(nightData, 0.0001); // Approximately 11 meters
    }

    private Location findWorkLocation(List<GpsData> data) {
        // Work is typically where the device spends most time during working hours on
        // weekdays
        List<GpsData> workData = data.stream()
                .filter(d -> {
                    DayOfWeek day = d.getTimestamp().getDayOfWeek();
                    int hour = d.getTimestamp().getHour();
                    // Monday to Friday, 9 AM to 5 PM
                    return day.getValue() <= 5 && hour >= 9 && hour <= 17;
                })
                .collect(Collectors.toList());

        return findMostFrequentLocation(workData, 0.0001);
    }

    private List<Location> findFrequentLocations(List<GpsData> data) {
        // Group GPS points into clusters based on proximity
        Map<String, List<GpsData>> clusters = new HashMap<>();

        for (GpsData point : data) {
            // Create a grid-based key for clustering (approximately 50 meters)
            String key = String.format("%.3f_%.3f",
                    Math.round(point.getLatitude() * 1000) / 1000.0,
                    Math.round(point.getLongitude() * 1000) / 1000.0);

            clusters.computeIfAbsent(key, k -> new ArrayList<>()).add(point);
        }

        // Create locations from clusters with at least 5 points
        return clusters.entrySet().stream()
                .filter(e -> e.getValue().size() >= 5)
                .map(e -> {
                    GpsData center = calculateClusterCenter(e.getValue());
                    return new Location(
                            center.getLatitude(),
                            center.getLongitude(),
                            e.getValue().size(),
                            determineLocationType(e.getValue()));
                })
                .sorted((a, b) -> Integer.compare(b.getVisitCount(), a.getVisitCount())) // Sort by visit count
                                                                                         // descending
                .limit(10) // Top 10 locations
                .collect(Collectors.toList());
    }

    private List<DailyRoutine> findDailyRoutines(List<GpsData> data) {
        // Group data by day of week
        Map<DayOfWeek, List<GpsData>> dataByDay = data.stream()
                .collect(Collectors.groupingBy(d -> d.getTimestamp().getDayOfWeek()));

        List<DailyRoutine> routines = new ArrayList<>();

        // Analyze each day
        for (DayOfWeek day : DayOfWeek.values()) {
            List<GpsData> dayData = dataByDay.getOrDefault(day, Collections.emptyList());
            if (dayData.isEmpty())
                continue;

            DailyRoutine routine = new DailyRoutine();
            routine.setDayOfWeek(day);

            // Analyze morning routine (first significant movement)
            GpsData firstMovement = findFirstSignificantMovement(dayData);
            if (firstMovement != null) {
                routine.setFirstActivityTime(firstMovement.getTimestamp().toLocalTime());
            }

            // Analyze evening routine (last significant movement)
            GpsData lastMovement = findLastSignificantMovement(dayData);
            if (lastMovement != null) {
                routine.setLastActivityTime(lastMovement.getTimestamp().toLocalTime());
            }

            // Add common places visited on this day
            routine.setCommonPlacesVisited(findPlacesVisitedOnDay(dayData));

            routines.add(routine);
        }

        return routines;
    }

    // Utility methods

    private Location findMostFrequentLocation(List<GpsData> data, double proximityThreshold) {
        if (data.isEmpty()) {
            return null;
        }

        // Group data points by proximity
        Map<String, List<GpsData>> locationClusters = new HashMap<>();

        for (GpsData point : data) {
            boolean added = false;

            // Check if point belongs to an existing cluster
            for (Map.Entry<String, List<GpsData>> entry : locationClusters.entrySet()) {
                GpsData clusterCenter = calculateClusterCenter(entry.getValue());
                double distance = calculateDistance(
                        point.getLatitude(), point.getLongitude(),
                        clusterCenter.getLatitude(), clusterCenter.getLongitude());

                if (distance <= proximityThreshold) {
                    entry.getValue().add(point);
                    added = true;
                    break;
                }
            }

            // If not added to any cluster, create a new one
            if (!added) {
                String key = UUID.randomUUID().toString();
                locationClusters.put(key, new ArrayList<>(Collections.singletonList(point)));
            }
        }

        // Find the cluster with the most points
        Map.Entry<String, List<GpsData>> largestCluster = null;
        for (Map.Entry<String, List<GpsData>> entry : locationClusters.entrySet()) {
            if (largestCluster == null || entry.getValue().size() > largestCluster.getValue().size()) {
                largestCluster = entry;
            }
        }

        if (largestCluster != null) {
            GpsData center = calculateClusterCenter(largestCluster.getValue());
            return new Location(
                    center.getLatitude(),
                    center.getLongitude(),
                    largestCluster.getValue().size(),
                    determineLocationType(largestCluster.getValue()));
        }

        return null;
    }

    private GpsData calculateClusterCenter(List<GpsData> points) {
        double sumLat = 0, sumLon = 0;
        for (GpsData point : points) {
            sumLat += point.getLatitude();
            sumLon += point.getLongitude();
        }

        return GpsData.builder()
                .latitude(sumLat / points.size())
                .longitude(sumLon / points.size())
                .build();
    }

    private String determineLocationType(List<GpsData> points) {
        // Determine location type based on visit patterns
        // This is a simplified version - a real implementation would be more
        // sophisticated

        // Get average hour of the day
        double avgHour = points.stream()
                .mapToInt(p -> p.getTimestamp().getHour())
                .average()
                .orElse(12);

        // Get most common day of week
        Map<DayOfWeek, Long> dayOfWeekCounts = points.stream()
                .collect(Collectors.groupingBy(p -> p.getTimestamp().getDayOfWeek(), Collectors.counting()));

        DayOfWeek mostCommonDay = dayOfWeekCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(DayOfWeek.MONDAY);

        // Decide based on time and day patterns
        if (avgHour >= 22 || avgHour <= 6) {
            return "HOME";
        } else if (avgHour >= 9 && avgHour <= 17 && mostCommonDay.getValue() <= 5) {
            return "WORK";
        } else if (avgHour >= 17 && avgHour <= 21) {
            return "LEISURE";
        } else {
            return "OTHER";
        }
    }

    private GpsData findFirstSignificantMovement(List<GpsData> dayData) {
        // Sort by timestamp
        List<GpsData> sorted = new ArrayList<>(dayData);
        sorted.sort(Comparator.comparing(GpsData::getTimestamp));

        // Find first significant movement (speed > 1.0 m/s)
        return sorted.stream()
                .filter(d -> d.getSpeed() > 1.0)
                .findFirst()
                .orElse(null);
    }

    private GpsData findLastSignificantMovement(List<GpsData> dayData) {
        // Sort by timestamp descending
        List<GpsData> sorted = new ArrayList<>(dayData);
        sorted.sort(Comparator.comparing(GpsData::getTimestamp).reversed());

        // Find first (which is last chronologically) significant movement
        return sorted.stream()
                .filter(d -> d.getSpeed() > 1.0)
                .findFirst()
                .orElse(null);
    }

    private List<Location> findPlacesVisitedOnDay(List<GpsData> dayData) {
        // Group into stays (periods of low movement)
        List<List<GpsData>> stays = new ArrayList<>();
        List<GpsData> currentStay = new ArrayList<>();

        // Sort by timestamp
        List<GpsData> sorted = new ArrayList<>(dayData);
        sorted.sort(Comparator.comparing(GpsData::getTimestamp));

        for (GpsData point : sorted) {
            if (currentStay.isEmpty()) {
                currentStay.add(point);
            } else {
                GpsData last = currentStay.get(currentStay.size() - 1);

                // Check if this point is close to the last one
                double distance = calculateDistance(
                        point.getLatitude(), point.getLongitude(),
                        last.getLatitude(), last.getLongitude());

                if (distance < 0.05) { // Less than 50 meters
                    currentStay.add(point);
                } else {
                    // If stay is long enough (more than 5 minutes), add it
                    if (currentStay.size() >= 5) {
                        stays.add(new ArrayList<>(currentStay));
                    }
                    currentStay.clear();
                    currentStay.add(point);
                }
            }
        }

        // Add the last stay if it's significant
        if (currentStay.size() >= 5) {
            stays.add(currentStay);
        }

        // Convert stays to locations
        return stays.stream()
                .map(stay -> {
                    GpsData center = calculateClusterCenter(stay);
                    return new Location(
                            center.getLatitude(),
                            center.getLongitude(),
                            stay.size(),
                            determineLocationType(stay));
                })
                .collect(Collectors.toList());
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula for distance calculation
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    // Data classes

    @Data
    public static class DevicePattern {
        private String deviceId;
        private Location homeLocation;
        private Location workLocation;
        private List<Location> frequentLocations = new ArrayList<>();
        private List<DailyRoutine> routines = new ArrayList<>();
        private LocalDateTime lastUpdated;
    }

    @Data
    public static class Location {
        private final double latitude;
        private final double longitude;
        private final int visitCount;
        private final String locationType; // HOME, WORK, LEISURE, OTHER
    }

    @Data
    public static class DailyRoutine {
        private DayOfWeek dayOfWeek;
        private LocalTime firstActivityTime;
        private LocalTime lastActivityTime;
        private List<Location> commonPlacesVisited = new ArrayList<>();
    }
}