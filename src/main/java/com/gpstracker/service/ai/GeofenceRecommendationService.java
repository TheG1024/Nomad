package com.gpstracker.service.ai;

import com.gpstracker.dto.CircleGeofenceDto;
import com.gpstracker.dto.GeoPointDto;
import com.gpstracker.dto.PolygonGeofenceDto;
import com.gpstracker.model.GpsData;
import com.gpstracker.service.GpsDataService;
import com.gpstracker.service.ai.PatternLearningService.DevicePattern;
import com.gpstracker.service.ai.PatternLearningService.Location;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that generates intelligent geofence recommendations based on user movement patterns
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeofenceRecommendationService {

    private final PatternLearningService patternLearningService;
    private final GpsDataService gpsDataService;
    
    // Cache for recommendations to avoid excessive processing
    private final Map<String, List<GeofenceRecommendation>> deviceRecommendations = new HashMap<>();
    private final Map<String, LocalDateTime> lastRecommendationUpdate = new HashMap<>();

    /**
     * Get geofence recommendations for a specific device
     * 
     * @param deviceId The device ID to generate recommendations for
     * @return List of geofence recommendations
     */
    public List<GeofenceRecommendation> getRecommendationsForDevice(String deviceId) {
        // Check if we have recent recommendations cached
        LocalDateTime lastUpdate = lastRecommendationUpdate.get(deviceId);
        if (lastUpdate != null && lastUpdate.isAfter(LocalDateTime.now().minus(24, ChronoUnit.HOURS))) {
            return deviceRecommendations.getOrDefault(deviceId, Collections.emptyList());
        }
        
        log.info("Generating geofence recommendations for device {}", deviceId);
        
        // Get the device's movement pattern
        DevicePattern pattern = patternLearningService.getPatternForDevice(deviceId);
        if (pattern == null) {
            log.warn("No pattern available for device {}", deviceId);
            return Collections.emptyList();
        }
        
        List<GeofenceRecommendation> recommendations = new ArrayList<>();
        
        // Add home geofence if detected
        if (pattern.getHomeLocation() != null) {
            recommendations.add(createHomeGeofenceRecommendation(deviceId, pattern.getHomeLocation()));
        }
        
        // Add work geofence if detected
        if (pattern.getWorkLocation() != null) {
            recommendations.add(createWorkGeofenceRecommendation(deviceId, pattern.getWorkLocation()));
        }
        
        // Add frequently visited locations
        if (pattern.getFrequentLocations() != null) {
            for (Location location : pattern.getFrequentLocations()) {
                // Skip if it's home or work (already added)
                if ("HOME".equals(location.getLocationType()) || "WORK".equals(location.getLocationType())) {
                    continue;
                }
                
                // Only add significant locations (visited often)
                if (location.getVisitCount() >= 5) {
                    recommendations.add(createLocationGeofenceRecommendation(deviceId, location));
                }
            }
        }
        
        // Add route-based geofences for common paths
        recommendations.addAll(createRouteGeofenceRecommendations(deviceId));
        
        // Add time-based recommendations for frequent activities
        recommendations.addAll(createTimeBasedRecommendations(deviceId, pattern));
        
        // Update cache
        deviceRecommendations.put(deviceId, recommendations);
        lastRecommendationUpdate.put(deviceId, LocalDateTime.now());
        
        return recommendations;
    }
    
    /**
     * Get the top recommendations for a device (limited to specified count)
     * 
     * @param deviceId The device ID
     * @param count Maximum number of recommendations to return
     * @return List of the top recommendations
     */
    public List<GeofenceRecommendation> getTopRecommendationsForDevice(String deviceId, int count) {
        return getRecommendationsForDevice(deviceId).stream()
                .sorted(Comparator.comparing(GeofenceRecommendation::getConfidence).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }
    
    /**
     * Clear the recommendations cache for a device
     * Forces regeneration on next request
     * 
     * @param deviceId The device ID to clear cache for
     */
    public void clearRecommendationsCache(String deviceId) {
        deviceRecommendations.remove(deviceId);
        lastRecommendationUpdate.remove(deviceId);
        log.info("Cleared recommendation cache for device {}", deviceId);
    }
    
    // Private helper methods
    
    private GeofenceRecommendation createHomeGeofenceRecommendation(String deviceId, Location homeLocation) {
        CircleGeofenceDto geofence = CircleGeofenceDto.builder()
                .deviceId(deviceId)
                .name("Home")
                .description("Automatically detected home location")
                .centerLatitude(homeLocation.getLatitude())
                .centerLongitude(homeLocation.getLongitude())
                .radiusMeters(100.0) // 100 meter radius for home
                .category("home")
                .alertLevel(1) // Low alert level
                .color("#4CAF50") // Green
                .build();
        
        return GeofenceRecommendation.builder()
                .type(RecommendationType.HOME)
                .circleGeofence(geofence)
                .confidence(calculateHomeConfidence(homeLocation))
                .reason("This appears to be your home location based on overnight stays")
                .build();
    }
    
    private GeofenceRecommendation createWorkGeofenceRecommendation(String deviceId, Location workLocation) {
        CircleGeofenceDto geofence = CircleGeofenceDto.builder()
                .deviceId(deviceId)
                .name("Work")
                .description("Automatically detected workplace")
                .centerLatitude(workLocation.getLatitude())
                .centerLongitude(workLocation.getLongitude())
                .radiusMeters(150.0) // 150 meter radius for work
                .category("work")
                .alertLevel(1) // Low alert level
                .color("#2196F3") // Blue
                .build();
        
        return GeofenceRecommendation.builder()
                .type(RecommendationType.WORK)
                .circleGeofence(geofence)
                .confidence(calculateWorkConfidence(workLocation))
                .reason("This appears to be your workplace based on weekday daytime patterns")
                .build();
    }
    
    private GeofenceRecommendation createLocationGeofenceRecommendation(String deviceId, Location location) {
        String name, category, color;
        double radius;
        
        // Set properties based on location type
        switch (location.getLocationType()) {
            case "LEISURE":
                name = "Leisure Spot";
                category = "leisure";
                color = "#FF9800"; // Orange
                radius = 120.0;
                break;
            case "OTHER":
            default:
                name = "Frequent Location";
                category = "frequent";
                color = "#9C27B0"; // Purple
                radius = 100.0;
        }
        
        CircleGeofenceDto geofence = CircleGeofenceDto.builder()
                .deviceId(deviceId)
                .name(name)
                .description("Frequently visited location")
                .centerLatitude(location.getLatitude())
                .centerLongitude(location.getLongitude())
                .radiusMeters(radius)
                .category(category)
                .alertLevel(1)
                .color(color)
                .build();
        
        return GeofenceRecommendation.builder()
                .type(RecommendationType.FREQUENT_LOCATION)
                .circleGeofence(geofence)
                .confidence(calculateLocationConfidence(location))
                .reason("You visit this location frequently (" + location.getVisitCount() + " visits detected)")
                .build();
    }
    
    private List<GeofenceRecommendation> createRouteGeofenceRecommendations(String deviceId) {
        List<GeofenceRecommendation> routeRecommendations = new ArrayList<>();
        
        // Get recent path data
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minus(14, ChronoUnit.DAYS);
        List<GpsData> recentData = gpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);
        
        if (recentData.size() < 50) {
            return routeRecommendations; // Not enough data
        }
        
        // Group data by day to find common routes
        Map<String, List<List<GpsData>>> routesByDay = new HashMap<>();
        
        // Sort by timestamp
        recentData.sort(Comparator.comparing(GpsData::getTimestamp));
        
        // Find continuous movement segments (likely routes)
        List<GpsData> currentRoute = new ArrayList<>();
        GpsData previousPoint = null;
        
        for (GpsData point : recentData) {
            String dayKey = point.getTimestamp().toLocalDate().toString();
            
            if (previousPoint == null) {
                currentRoute.add(point);
            } else {
                // Check if this is part of the same journey (within 5 minutes)
                long minutesBetween = ChronoUnit.MINUTES.between(previousPoint.getTimestamp(), point.getTimestamp());
                
                if (minutesBetween <= 5 && point.getSpeed() > 1.0) {
                    // Part of the same route
                    currentRoute.add(point);
                } else {
                    // End of route
                    if (currentRoute.size() >= 10) { // Only consider substantial routes
                        // Add to routes for this day
                        routesByDay.computeIfAbsent(dayKey, k -> new ArrayList<>()).add(new ArrayList<>(currentRoute));
                    }
                    currentRoute.clear();
                    currentRoute.add(point);
                }
            }
            
            previousPoint = point;
        }
        
        // Add the last route if significant
        if (currentRoute.size() >= 10) {
            String dayKey = currentRoute.get(0).getTimestamp().toLocalDate().toString();
            routesByDay.computeIfAbsent(dayKey, k -> new ArrayList<>()).add(currentRoute);
        }
        
        // Find similar routes across days
        List<List<GpsData>> commonRoutes = findCommonRoutes(routesByDay);
        
        // Create polygon geofences for the most common routes
        int routeCount = 0;
        for (List<GpsData> route : commonRoutes) {
            if (routeCount >= 3) break; // Limit to 3 route recommendations
            
            // Simplify route to reduce number of points
            List<GpsData> simplifiedRoute = simplifyRoute(route);
            
            // Create polygon vertices
            List<GeoPointDto> vertices = new ArrayList<>();
            for (GpsData point : simplifiedRoute) {
                vertices.add(GeoPointDto.builder()
                        .latitude(point.getLatitude())
                        .longitude(point.getLongitude())
                        .build());
            }
            
            if (vertices.size() < 3) continue; // Need at least 3 points for a polygon
            
            PolygonGeofenceDto geofence = PolygonGeofenceDto.builder()
                    .deviceId(deviceId)
                    .name("Common Route " + (routeCount + 1))
                    .description("Automatically detected travel route")
                    .vertices(vertices)
                    .category("route")
                    .alertLevel(1)
                    .color("#FF5722") // Deep Orange
                    .build();
            
            routeRecommendations.add(GeofenceRecommendation.builder()
                    .type(RecommendationType.COMMON_ROUTE)
                    .polygonGeofence(geofence)
                    .confidence(0.7) // Fixed confidence for routes
                    .reason("This appears to be a route you travel frequently")
                    .build());
            
            routeCount++;
        }
        
        return routeRecommendations;
    }
    
    private List<GeofenceRecommendation> createTimeBasedRecommendations(String deviceId, DevicePattern pattern) {
        List<GeofenceRecommendation> timeRecommendations = new ArrayList<>();
        
        // Check if pattern has routines
        if (pattern.getRoutines() == null || pattern.getRoutines().isEmpty()) {
            return timeRecommendations;
        }
        
        // Find common first activity time
        Map<Integer, Integer> firstActivityHours = new HashMap<>();
        for (PatternLearningService.DailyRoutine routine : pattern.getRoutines()) {
            if (routine.getFirstActivityTime() != null) {
                int hour = routine.getFirstActivityTime().getHour();
                firstActivityHours.merge(hour, 1, Integer::sum);
            }
        }
        
        // Find most common first activity hour
        Map.Entry<Integer, Integer> mostCommonFirstActivity = firstActivityHours.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        
        if (mostCommonFirstActivity != null && mostCommonFirstActivity.getValue() >= 3) {
            // Create a morning activity geofence at home
            if (pattern.getHomeLocation() != null) {
                CircleGeofenceDto geofence = CircleGeofenceDto.builder()
                        .deviceId(deviceId)
                        .name("Morning Activity")
                        .description("Daily morning movement detection")
                        .centerLatitude(pattern.getHomeLocation().getLatitude())
                        .centerLongitude(pattern.getHomeLocation().getLongitude())
                        .radiusMeters(200.0)
                        .category("time")
                        .alertLevel(2)
                        .color("#FFC107") // Amber
                        .build();
                
                timeRecommendations.add(GeofenceRecommendation.builder()
                        .type(RecommendationType.TIME_BASED)
                        .circleGeofence(geofence)
                        .confidence(mostCommonFirstActivity.getValue() / 7.0) // Confidence based on frequency
                        .reason("You typically start moving around " + mostCommonFirstActivity.getKey() + ":00")
                        .build());
            }
        }
        
        return timeRecommendations;
    }
    
    // Utility methods
    
    private double calculateHomeConfidence(Location homeLocation) {
        // Calculate confidence based on visit count
        // Higher visit count means higher confidence
        return Math.min(0.95, 0.5 + (homeLocation.getVisitCount() / 200.0));
    }
    
    private double calculateWorkConfidence(Location workLocation) {
        // Calculate confidence based on visit count
        // Higher visit count means higher confidence
        return Math.min(0.90, 0.4 + (workLocation.getVisitCount() / 150.0));
    }
    
    private double calculateLocationConfidence(Location location) {
        // Calculate confidence based on visit count and type
        double baseConfidence = 0.3;
        
        // Adjust based on visit count
        double visitFactor = Math.min(0.5, location.getVisitCount() / 100.0);
        
        // Adjust based on type
        double typeFactor = 0.0;
        switch (location.getLocationType()) {
            case "LEISURE": typeFactor = 0.2; break;
            case "OTHER": typeFactor = 0.1; break;
        }
        
        return baseConfidence + visitFactor + typeFactor;
    }
    
    private List<List<GpsData>> findCommonRoutes(Map<String, List<List<GpsData>>> routesByDay) {
        // This is a simplified approach to finding common routes
        // A more sophisticated implementation would use clustering or path similarity
        
        // For now, find routes with similar start and end points
        Map<String, List<List<GpsData>>> routeGroups = new HashMap<>();
        
        for (List<List<GpsData>> dayRoutes : routesByDay.values()) {
            for (List<GpsData> route : dayRoutes) {
                if (route.size() < 2) continue;
                
                GpsData start = route.get(0);
                GpsData end = route.get(route.size() - 1);
                
                // Create key based on approximate start and end points
                String key = String.format("%.3f_%.3f_to_%.3f_%.3f",
                        Math.round(start.getLatitude() * 1000) / 1000.0,
                        Math.round(start.getLongitude() * 1000) / 1000.0,
                        Math.round(end.getLatitude() * 1000) / 1000.0,
                        Math.round(end.getLongitude() * 1000) / 1000.0);
                
                routeGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(route);
            }
        }
        
        // Sort groups by frequency and return the most common
        return routeGroups.values().stream()
                .sorted((a, b) -> Integer.compare(b.size(), a.size()))
                .limit(3)
                .map(routes -> routes.get(0)) // Take the first route from each group
                .collect(Collectors.toList());
    }
    
    private List<GpsData> simplifyRoute(List<GpsData> route) {
        // Simple Douglas-Peucker algorithm for route simplification
        if (route.size() <= 2) return new ArrayList<>(route);
        
        List<GpsData> result = new ArrayList<>();
        result.add(route.get(0)); // Always include first point
        
        // Add intermediate points spaced out
        for (int i = 1; i < route.size() - 1; i += 5) {
            result.add(route.get(i));
        }
        
        result.add(route.get(route.size() - 1)); // Always include last point
        return result;
    }
    
    // Data classes
    
    public enum RecommendationType {
        HOME, WORK, FREQUENT_LOCATION, COMMON_ROUTE, TIME_BASED
    }
    
    @lombok.Data
    @lombok.Builder
    public static class GeofenceRecommendation {
        private RecommendationType type;
        private CircleGeofenceDto circleGeofence;
        private PolygonGeofenceDto polygonGeofence;
        private double confidence; // 0.0 to 1.0
        private String reason;
    }
} 