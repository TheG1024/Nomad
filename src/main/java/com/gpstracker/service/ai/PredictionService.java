package com.gpstracker.service.ai;

import com.gpstracker.model.GpsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class PredictionService {
    
    @Autowired
    private com.gpstracker.service.GpsDataService gpsDataService;
    
    private Map<String, List<PredictedRoute>> routePredictions = new HashMap<>();
    private Map<String, MovementPattern> devicePatterns = new HashMap<>();

    /**
     * Predicts the likely route for a device based on historical patterns
     */
    public List<PredictedRoute> predictRoute(String deviceId, LocalDateTime startTime) {
        List<GpsData> historicalData = gpsDataService.getGpsDataForDevice(
            deviceId,
            startTime.minus(7, ChronoUnit.DAYS),
            startTime
        );
        
        // Group data by day of week and time
        Map<Integer, List<GpsData>> dayPatterns = new HashMap<>();
        historicalData.forEach(data -> {
            int dayOfWeek = data.getTimestamp().getDayOfWeek().getValue();
            dayPatterns.computeIfAbsent(dayOfWeek, k -> new ArrayList<>()).add(data);
        });
        
        // Find similar patterns for current day
        int currentDayOfWeek = startTime.getDayOfWeek().getValue();
        List<GpsData> similarDayData = dayPatterns.get(currentDayOfWeek);
        
        if (similarDayData == null || similarDayData.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Calculate likely routes based on historical patterns
        List<PredictedRoute> predictions = new ArrayList<>();
        Map<RouteSegment, Integer> frequentSegments = analyzeRouteSegments(similarDayData);
        
        // Convert frequent segments to predictions
        frequentSegments.forEach((segment, frequency) -> {
            if (frequency >= 3) { // Only consider segments seen 3 or more times
                predictions.add(new PredictedRoute(
                    segment.startLat,
                    segment.startLon,
                    segment.endLat,
                    segment.endLon,
                    calculateProbability(frequency, similarDayData.size())
                ));
            }
        });
        
        routePredictions.put(deviceId, predictions);
        return predictions;
    }

    /**
     * Detects anomalies in device behavior
     */
    public List<Anomaly> detectAnomalies(String deviceId, GpsData currentData) {
        List<Anomaly> anomalies = new ArrayList<>();
        MovementPattern pattern = devicePatterns.get(deviceId);
        
        if (pattern != null) {
            // Check for speed anomalies
            if (currentData.getSpeed() > pattern.averageSpeed * 2) {
                anomalies.add(new Anomaly(
                    AnomalyType.SPEED,
                    "Unusual speed detected",
                    calculateAnomalyScore(currentData.getSpeed(), pattern.averageSpeed)
                ));
            }
            
            // Check for route deviation
            double routeDeviation = calculateRouteDeviation(currentData, pattern.commonLocations);
            if (routeDeviation > pattern.maxDeviation * 1.5) {
                anomalies.add(new Anomaly(
                    AnomalyType.ROUTE,
                    "Significant route deviation detected",
                    calculateAnomalyScore(routeDeviation, pattern.maxDeviation)
                ));
            }
            
            // Check for unusual timing
            if (!isWithinUsualTimeRange(currentData.getTimestamp(), pattern.activeHours)) {
                anomalies.add(new Anomaly(
                    AnomalyType.TIMING,
                    "Activity outside usual hours",
                    0.8
                ));
            }
        }
        
        return anomalies;
    }

    /**
     * Updates movement patterns based on new data
     */
    @Scheduled(cron = "0 0 * * * *") // Run hourly
    public void updateMovementPatterns() {
        log.info("Updating movement patterns");
        
        // Get data for the last 30 days for pattern analysis
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minus(30, ChronoUnit.DAYS);
        
        // For each known device
        Set<String> knownDevices = new HashSet<>(); // In production, this would come from a device registry
        knownDevices.add("device123"); // Example device
        
        for (String deviceId : knownDevices) {
            List<GpsData> monthData = gpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);
            
            MovementPattern pattern = new MovementPattern();
            pattern.averageSpeed = calculateAverageSpeed(monthData);
            pattern.commonLocations = findCommonLocations(monthData);
            pattern.activeHours = analyzeActiveHours(monthData);
            pattern.maxDeviation = calculateMaxDeviation(monthData);
            
            devicePatterns.put(deviceId, pattern);
        }
    }

    // Helper classes and methods

    @lombok.Data
    public static class PredictedRoute {
        private final double startLat;
        private final double startLon;
        private final double endLat;
        private final double endLon;
        private final double probability;
    }

    @lombok.Data
    public static class Anomaly {
        private final AnomalyType type;
        private final String description;
        private final double severity;
    }

    public enum AnomalyType {
        SPEED, ROUTE, TIMING, BEHAVIOR
    }

    @lombok.Data
    private static class MovementPattern {
        private double averageSpeed;
        private List<Location> commonLocations;
        private Map<Integer, TimeRange> activeHours; // Hour -> TimeRange
        private double maxDeviation;
    }

    @lombok.Data
    private static class Location {
        private final double latitude;
        private final double longitude;
        private int frequency;
    }

    @lombok.Data
    private static class TimeRange {
        private final LocalDateTime start;
        private final LocalDateTime end;
    }

    @lombok.Data
    private static class RouteSegment {
        private final double startLat;
        private final double startLon;
        private final double endLat;
        private final double endLon;
    }

    // Private helper methods

    private Map<RouteSegment, Integer> analyzeRouteSegments(List<GpsData> data) {
        Map<RouteSegment, Integer> segments = new HashMap<>();
        for (int i = 0; i < data.size() - 1; i++) {
            RouteSegment segment = new RouteSegment(
                data.get(i).getLatitude(),
                data.get(i).getLongitude(),
                data.get(i + 1).getLatitude(),
                data.get(i + 1).getLongitude()
            );
            segments.merge(segment, 1, Integer::sum);
        }
        return segments;
    }

    private double calculateProbability(int frequency, int total) {
        return (double) frequency / total;
    }

    private double calculateRouteDeviation(GpsData current, List<Location> commonLocations) {
        double minDeviation = Double.MAX_VALUE;
        for (Location location : commonLocations) {
            double deviation = calculateDistance(
                current.getLatitude(),
                current.getLongitude(),
                location.getLatitude(),
                location.getLongitude()
            );
            minDeviation = Math.min(minDeviation, deviation);
        }
        return minDeviation;
    }

    private boolean isWithinUsualTimeRange(LocalDateTime time, Map<Integer, TimeRange> activeHours) {
        TimeRange range = activeHours.get(time.getHour());
        return range != null &&
               !time.isBefore(range.start) &&
               !time.isAfter(range.end);
    }

    private double calculateAnomalyScore(double current, double baseline) {
        return Math.min(1.0, Math.abs(current - baseline) / baseline);
    }

    private double calculateAverageSpeed(List<GpsData> data) {
        return data.stream()
                  .mapToDouble(GpsData::getSpeed)
                  .average()
                  .orElse(0.0);
    }

    private List<Location> findCommonLocations(List<GpsData> data) {
        Map<String, Location> locations = new HashMap<>();
        
        data.forEach(gps -> {
            String key = String.format("%.4f_%.4f", gps.getLatitude(), gps.getLongitude());
            locations.computeIfAbsent(
                key,
                k -> new Location(gps.getLatitude(), gps.getLongitude())
            ).frequency++;
        });
        
        return locations.values().stream()
                       .filter(loc -> loc.frequency >= 5)
                       .toList();
    }

    private Map<Integer, TimeRange> analyzeActiveHours(List<GpsData> data) {
        Map<Integer, List<LocalDateTime>> hourlyData = new HashMap<>();
        
        data.forEach(gps -> {
            int hour = gps.getTimestamp().getHour();
            hourlyData.computeIfAbsent(hour, k -> new ArrayList<>())
                     .add(gps.getTimestamp());
        });
        
        Map<Integer, TimeRange> activeHours = new HashMap<>();
        hourlyData.forEach((hour, times) -> {
            if (times.size() >= 5) { // Consider hour active if seen 5+ times
                activeHours.put(hour, new TimeRange(
                    times.stream().min(LocalDateTime::compareTo).orElse(null),
                    times.stream().max(LocalDateTime::compareTo).orElse(null)
                ));
            }
        });
        
        return activeHours;
    }

    private double calculateMaxDeviation(List<GpsData> data) {
        if (data.isEmpty()) return 0.0;
        
        double maxDev = 0.0;
        GpsData center = calculateCenterPoint(data);
        
        for (GpsData point : data) {
            double deviation = calculateDistance(
                point.getLatitude(),
                point.getLongitude(),
                center.getLatitude(),
                center.getLongitude()
            );
            maxDev = Math.max(maxDev, deviation);
        }
        
        return maxDev;
    }

    private GpsData calculateCenterPoint(List<GpsData> data) {
        double sumLat = 0, sumLon = 0;
        for (GpsData point : data) {
            sumLat += point.getLatitude();
            sumLon += point.getLongitude();
        }
        
        GpsData center = new GpsData();
        center.setLatitude(sumLat / data.size());
        center.setLongitude(sumLon / data.size());
        return center;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
