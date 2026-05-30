package com.gpstracker.service.ai;

import com.gpstracker.model.GpsData;
import com.gpstracker.service.GpsDataService;
import com.gpstracker.service.ai.PatternLearningService.DevicePattern;
import com.gpstracker.service.ai.PatternLearningService.Location;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that analyzes movement data and classifies trips into different categories
 * Uses machine learning techniques to identify patterns and determine trip purpose
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripClassificationService {

    private final GpsDataService gpsDataService;
    private final PatternLearningService patternLearningService;
    
    // Cache trip classifications to avoid reprocessing
    private final Map<String, Map<String, Trip>> deviceTripCache = new HashMap<>();

    /**
     * Identify and classify a specific trip by ID
     * 
     * @param deviceId Device that made the trip
     * @param tripId Unique trip identifier
     * @return Classified trip or null if not found
     */
    public Trip getTrip(String deviceId, String tripId) {
        if (!deviceTripCache.containsKey(deviceId)) {
            return null;
        }
        return deviceTripCache.get(deviceId).get(tripId);
    }

    /**
     * Analyze device data to identify and classify trips
     * 
     * @param deviceId Device to analyze
     * @param startTime Start of analysis period
     * @param endTime End of analysis period
     * @return List of identified and classified trips
     */
    public List<Trip> classifyTrips(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Classifying trips for device {} from {} to {}", deviceId, startTime, endTime);
        
        // Get the GPS data for the specified period
        List<GpsData> data = gpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);
        if (data.isEmpty()) {
            log.warn("No GPS data found for device {} in the specified period", deviceId);
            return Collections.emptyList();
        }
        
        // Get the device's movement patterns for reference
        DevicePattern pattern = patternLearningService.getPatternForDevice(deviceId);
        if (pattern == null) {
            log.warn("No pattern data available for device {}, classification accuracy will be reduced", deviceId);
        }
        
        // Identify individual trips from the data
        List<TripSegment> segments = identifyTripSegments(data);
        
        // Classify each trip segment
        List<Trip> trips = new ArrayList<>();
        for (TripSegment segment : segments) {
            Trip trip = classifyTripSegment(segment, pattern);
            trips.add(trip);
            
            // Cache the trip
            deviceTripCache
                .computeIfAbsent(deviceId, k -> new HashMap<>())
                .put(trip.getTripId(), trip);
        }
        
        log.info("Classified {} trips for device {}", trips.size(), deviceId);
        return trips;
    }
    
    /**
     * Get all trips for a device that match a specific classification
     * 
     * @param deviceId Device to query
     * @param classification Type of trips to find
     * @param limit Maximum number of trips to return
     * @return List of matching trips
     */
    public List<Trip> getTripsByClassification(String deviceId, TripClassification classification, int limit) {
        // Try to get from cache first
        Map<String, Trip> deviceTrips = deviceTripCache.get(deviceId);
        if (deviceTrips != null && !deviceTrips.isEmpty()) {
            return deviceTrips.values().stream()
                    .filter(trip -> trip.getClassification() == classification)
                    .sorted(Comparator.comparing(Trip::getStartTime).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        
        // If not in cache, analyze the last 30 days
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minus(30, ChronoUnit.DAYS);
        List<Trip> allTrips = classifyTrips(deviceId, startTime, endTime);
        
        return allTrips.stream()
                .filter(trip -> trip.getClassification() == classification)
                .sorted(Comparator.comparing(Trip::getStartTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Get trip statistics for a specific device
     * 
     * @param deviceId Device to analyze
     * @return Statistics about the device's trips
     */
    public TripStatistics getTripStatistics(String deviceId) {
        // Ensure we have trip data
        if (!deviceTripCache.containsKey(deviceId) || deviceTripCache.get(deviceId).isEmpty()) {
            // Analyze last 30 days to populate cache
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minus(30, ChronoUnit.DAYS);
            classifyTrips(deviceId, startTime, endTime);
            
            if (!deviceTripCache.containsKey(deviceId) || deviceTripCache.get(deviceId).isEmpty()) {
                log.warn("No trip data available for device {}", deviceId);
                return new TripStatistics();
            }
        }
        
        Collection<Trip> trips = deviceTripCache.get(deviceId).values();
        
        // Build statistics
        TripStatistics stats = new TripStatistics();
        stats.setTotalTrips(trips.size());
        
        // Count by classification
        Map<TripClassification, Integer> countByClass = new EnumMap<>(TripClassification.class);
        for (Trip trip : trips) {
            countByClass.merge(trip.getClassification(), 1, Integer::sum);
        }
        stats.setTripCountByClassification(countByClass);
        
        // Calculate average speeds
        Map<TripClassification, Double> avgSpeedByClass = new EnumMap<>(TripClassification.class);
        Map<TripClassification, List<Double>> speedsByClass = new EnumMap<>(TripClassification.class);
        
        for (Trip trip : trips) {
            speedsByClass.computeIfAbsent(trip.getClassification(), k -> new ArrayList<>())
                          .add(trip.getAverageSpeed());
        }
        
        for (Map.Entry<TripClassification, List<Double>> entry : speedsByClass.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            avgSpeedByClass.put(entry.getKey(), avg);
        }
        stats.setAverageSpeedByClassification(avgSpeedByClass);
        
        // Calculate common times
        Map<TripClassification, Integer> commonHourByClass = new EnumMap<>(TripClassification.class);
        for (TripClassification classification : TripClassification.values()) {
            List<Trip> classTrips = trips.stream()
                    .filter(t -> t.getClassification() == classification)
                    .collect(Collectors.toList());
            
            if (!classTrips.isEmpty()) {
                Map<Integer, Integer> hourCounts = new HashMap<>();
                for (Trip trip : classTrips) {
                    int hour = trip.getStartTime().getHour();
                    hourCounts.merge(hour, 1, Integer::sum);
                }
                
                // Find most common hour
                int mostCommonHour = hourCounts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(-1);
                
                commonHourByClass.put(classification, mostCommonHour);
            }
        }
        stats.setMostCommonStartHourByClassification(commonHourByClass);
        
        return stats;
    }

    // Helper methods

    private List<TripSegment> identifyTripSegments(List<GpsData> data) {
        List<TripSegment> segments = new ArrayList<>();
        
        if (data.isEmpty()) {
            return segments;
        }
        
        // Sort data by timestamp
        data.sort(Comparator.comparing(GpsData::getTimestamp));
        
        GpsData previousPoint = null;
        List<GpsData> currentSegment = new ArrayList<>();
        boolean inSegment = false;
        
        for (GpsData point : data) {
            if (previousPoint == null) {
                previousPoint = point;
                currentSegment.add(point);
                continue;
            }
            
            // Calculate time difference
            long minutesBetween = ChronoUnit.MINUTES.between(previousPoint.getTimestamp(), point.getTimestamp());
            
            if (!inSegment && point.getSpeed() > 1.5) {
                // Start of a new trip segment
                inSegment = true;
                if (currentSegment.isEmpty()) {
                    currentSegment.add(point);
                }
            } else if (inSegment && (point.getSpeed() < 0.5 || minutesBetween > 10)) {
                // End of current segment
                if (currentSegment.size() >= 5) {
                    // Only consider segments with at least 5 points
                    segments.add(new TripSegment(new ArrayList<>(currentSegment)));
                }
                currentSegment.clear();
                inSegment = false;
            }
            
            // Add point to current segment if in a trip or if this point might be the start of one
            if (inSegment || point.getSpeed() > 1.0) {
                currentSegment.add(point);
            }
            
            previousPoint = point;
        }
        
        // Add the last segment if still in one
        if (inSegment && currentSegment.size() >= 5) {
            segments.add(new TripSegment(new ArrayList<>(currentSegment)));
        }
        
        return segments;
    }
    
    private Trip classifyTripSegment(TripSegment segment, DevicePattern pattern) {
        GpsData start = segment.points.get(0);
        GpsData end = segment.points.get(segment.points.size() - 1);
        
        // Calculate trip metrics
        double distance = calculateTripDistance(segment.points);
        Duration duration = Duration.between(start.getTimestamp(), end.getTimestamp());
        double averageSpeed = distance / (duration.getSeconds() / 3600.0); // km/h
        
        // Generate a unique trip ID
        String tripId = UUID.randomUUID().toString();
        
        // Default classification
        TripClassification classification = TripClassification.OTHER;
        String purpose = "Unknown trip purpose";
        
        if (pattern != null) {
            // Try to classify based on endpoints and patterns
            classification = determineClassification(segment, pattern, start.getTimestamp().getDayOfWeek());
            purpose = generatePurposeDescription(classification, segment, pattern);
        } else {
            // Basic classification based on time and speed
            classification = basicClassification(segment, averageSpeed);
            purpose = "Trip classified based on speed and time";
        }
        
        return Trip.builder()
                .tripId(tripId)
                .startTime(start.getTimestamp())
                .endTime(end.getTimestamp())
                .startLatitude(start.getLatitude())
                .startLongitude(start.getLongitude())
                .endLatitude(end.getLatitude())
                .endLongitude(end.getLongitude())
                .distanceKm(distance)
                .durationMinutes(duration.toMinutes())
                .averageSpeed(averageSpeed)
                .maxSpeed(calculateMaxSpeed(segment.points))
                .classification(classification)
                .purpose(purpose)
                .waypoints(extractSignificantWaypoints(segment.points))
                .build();
    }
    
    private TripClassification determineClassification(TripSegment segment, DevicePattern pattern, DayOfWeek dayOfWeek) {
        GpsData start = segment.points.get(0);
        GpsData end = segment.points.get(segment.points.size() - 1);
        
        // Check if trip starts from home
        boolean startsFromHome = isNearLocation(start, pattern.getHomeLocation());
        
        // Check if trip ends at home
        boolean endsAtHome = isNearLocation(end, pattern.getHomeLocation());
        
        // Check if trip involves work
        boolean startsFromWork = isNearLocation(start, pattern.getWorkLocation());
        boolean endsAtWork = isNearLocation(end, pattern.getWorkLocation());
        
        // Check if trip starts or ends at a leisure location
        boolean involvesLeisure = pattern.getFrequentLocations().stream()
                .anyMatch(loc -> "LEISURE".equals(loc.getLocationType()) &&
                        (isNearLocation(start, loc) || isNearLocation(end, loc)));
        
        // Determine if weekend
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        
        // Speed characteristics
        double averageSpeed = calculateAverageSpeed(segment.points);
        boolean isHighSpeed = averageSpeed > 50; // km/h
        
        // Trip duration
        Duration duration = Duration.between(
                segment.points.get(0).getTimestamp(),
                segment.points.get(segment.points.size() - 1).getTimestamp());
        boolean isShortTrip = duration.toMinutes() < 15;
        
        // Classification logic
        if (startsFromHome && endsAtWork && !isWeekend) {
            return TripClassification.COMMUTE_TO_WORK;
        } else if (startsFromWork && endsAtHome && !isWeekend) {
            return TripClassification.COMMUTE_FROM_WORK;
        } else if (isWeekend && isHighSpeed) {
            return TripClassification.LEISURE;
        } else if (involvesLeisure) {
            return TripClassification.LEISURE;
        } else if (startsFromHome && endsAtHome && isShortTrip) {
            return TripClassification.ERRAND;
        } else if (isHighSpeed && duration.toHours() >= 2) {
            return TripClassification.TRAVEL;
        } else {
            return TripClassification.OTHER;
        }
    }
    
    private TripClassification basicClassification(TripSegment segment, double averageSpeed) {
        GpsData start = segment.points.get(0);
        DayOfWeek dayOfWeek = start.getTimestamp().getDayOfWeek();
        int hour = start.getTimestamp().getHour();
        
        // Duration
        Duration duration = Duration.between(
                segment.points.get(0).getTimestamp(),
                segment.points.get(segment.points.size() - 1).getTimestamp());
        
        // Basic classification based on time and speed
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        boolean isMorningRushHour = hour >= 7 && hour <= 9 && !isWeekend;
        boolean isEveningRushHour = hour >= 16 && hour <= 19 && !isWeekend;
        
        if (isMorningRushHour) {
            return TripClassification.COMMUTE_TO_WORK;
        } else if (isEveningRushHour) {
            return TripClassification.COMMUTE_FROM_WORK;
        } else if (isWeekend) {
            return TripClassification.LEISURE;
        } else if (duration.toMinutes() < 20) {
            return TripClassification.ERRAND;
        } else if (averageSpeed > 80 && duration.toHours() >= 2) {
            return TripClassification.TRAVEL;
        } else {
            return TripClassification.OTHER;
        }
    }
    
    private String generatePurposeDescription(TripClassification classification, TripSegment segment, DevicePattern pattern) {
        switch (classification) {
            case COMMUTE_TO_WORK:
                return "Morning commute to work";
                
            case COMMUTE_FROM_WORK:
                return "Evening commute from work to home";
                
            case LEISURE:
                // Try to find nearby leisure location
                for (Location location : pattern.getFrequentLocations()) {
                    if ("LEISURE".equals(location.getLocationType())) {
                        GpsData end = segment.points.get(segment.points.size() - 1);
                        if (isNearLocation(end, location)) {
                            return "Leisure trip to a frequently visited location";
                        }
                    }
                }
                return "Leisure trip during free time";
                
            case ERRAND:
                return "Short errand trip, likely for quick tasks or shopping";
                
            case TRAVEL:
                return "Extended travel, possibly for business or vacation";
                
            default:
                return "Trip with unclassified purpose";
        }
    }
    
    private double calculateTripDistance(List<GpsData> points) {
        double totalDistance = 0.0;
        
        for (int i = 0; i < points.size() - 1; i++) {
            GpsData p1 = points.get(i);
            GpsData p2 = points.get(i + 1);
            
            double segmentDistance = calculateDistance(
                    p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude());
            
            totalDistance += segmentDistance;
        }
        
        return totalDistance / 1000.0; // Convert to kilometers
    }
    
    private double calculateAverageSpeed(List<GpsData> points) {
        return points.stream()
                .mapToDouble(GpsData::getSpeed)
                .filter(speed -> speed > 0) // Filter out zero speeds
                .average()
                .orElse(0.0) * 3.6; // Convert m/s to km/h
    }
    
    private double calculateMaxSpeed(List<GpsData> points) {
        return points.stream()
                .mapToDouble(GpsData::getSpeed)
                .max()
                .orElse(0.0) * 3.6; // Convert m/s to km/h
    }
    
    private List<Waypoint> extractSignificantWaypoints(List<GpsData> points) {
        List<Waypoint> waypoints = new ArrayList<>();
        
        // Add start point
        GpsData start = points.get(0);
        waypoints.add(new Waypoint(
                start.getLatitude(),
                start.getLongitude(),
                start.getTimestamp(),
                WaypointType.START,
                "Trip start"
        ));
        
        // Look for significant points (high acceleration, sharp turns, stops)
        for (int i = 1; i < points.size() - 1; i++) {
            GpsData prev = points.get(i - 1);
            GpsData curr = points.get(i);
            GpsData next = points.get(i + 1);
            
            // Check for significant speed changes
            double speedChangePrev = Math.abs(curr.getSpeed() - prev.getSpeed());
            double speedChangeNext = Math.abs(next.getSpeed() - curr.getSpeed());
            
            if (speedChangePrev > 5.0 && speedChangeNext > 5.0) {
                // Significant acceleration or deceleration
                WaypointType type = curr.getSpeed() < 1.0 ? WaypointType.STOP : WaypointType.SPEED_CHANGE;
                waypoints.add(new Waypoint(
                        curr.getLatitude(),
                        curr.getLongitude(),
                        curr.getTimestamp(),
                        type,
                        type == WaypointType.STOP ? "Temporary stop" : "Speed change"
                ));
            }
            
            // Check for sharp turns
            if (i > 1 && i < points.size() - 2) {
                double headingChange = Math.abs(next.getHeading() - prev.getHeading());
                if (headingChange > 45.0) { // More than 45 degree turn
                    waypoints.add(new Waypoint(
                            curr.getLatitude(),
                            curr.getLongitude(),
                            curr.getTimestamp(),
                            WaypointType.TURN,
                            "Sharp turn"
                    ));
                }
            }
        }
        
        // Add end point
        GpsData end = points.get(points.size() - 1);
        waypoints.add(new Waypoint(
                end.getLatitude(),
                end.getLongitude(),
                end.getTimestamp(),
                WaypointType.END,
                "Trip end"
        ));
        
        return waypoints;
    }
    
    private boolean isNearLocation(GpsData point, Location location) {
        if (location == null) return false;
        
        double distance = calculateDistance(
                point.getLatitude(), point.getLongitude(),
                location.getLatitude(), location.getLongitude());
        
        return distance < 200; // Within 200 meters
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

    private static class TripSegment {
        private final List<GpsData> points;
        
        public TripSegment(List<GpsData> points) {
            this.points = points;
        }
    }
    
    @Data
    @Builder
    public static class Trip {
        private String tripId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private double startLatitude;
        private double startLongitude;
        private double endLatitude;
        private double endLongitude;
        private double distanceKm;
        private long durationMinutes;
        private double averageSpeed; // km/h
        private double maxSpeed;     // km/h
        private TripClassification classification;
        private String purpose;
        private List<Waypoint> waypoints;
    }
    
    @Data
    public static class Waypoint {
        private final double latitude;
        private final double longitude;
        private final LocalDateTime timestamp;
        private final WaypointType type;
        private final String description;
    }
    
    public enum TripClassification {
        COMMUTE_TO_WORK,
        COMMUTE_FROM_WORK,
        ERRAND,
        LEISURE,
        TRAVEL,
        OTHER
    }
    
    public enum WaypointType {
        START,
        END,
        STOP,
        TURN,
        SPEED_CHANGE
    }
    
    @Data
    public static class TripStatistics {
        private int totalTrips;
        private Map<TripClassification, Integer> tripCountByClassification = new EnumMap<>(TripClassification.class);
        private Map<TripClassification, Double> averageSpeedByClassification = new EnumMap<>(TripClassification.class);
        private Map<TripClassification, Integer> mostCommonStartHourByClassification = new EnumMap<>(TripClassification.class);
    }
} 