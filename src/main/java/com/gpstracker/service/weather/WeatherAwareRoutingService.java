package com.gpstracker.service.weather;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WeatherAwareRoutingService {

    @Value("${openweathermap.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    private final Map<String, WeatherCache> weatherCache = new ConcurrentHashMap<>();
    private static final int CACHE_DURATION_MINUTES = 30;

    /**
     * Calculate weather-adjusted route
     */
    public WeatherAwareRoute calculateRoute(
            double startLat, double startLon,
            double endLat, double endLon,
            LocalDateTime departureTime) {
        
        // Get weather conditions along the route
        List<WeatherPoint> weatherPoints = getWeatherAlongRoute(
            startLat, startLon,
            endLat, endLon,
            departureTime
        );

        // Calculate base route
        List<RoutePoint> baseRoute = calculateBaseRoute(
            startLat, startLon,
            endLat, endLon
        );

        // Adjust route based on weather conditions
        return adjustRouteForWeather(baseRoute, weatherPoints);
    }

    /**
     * Get weather forecast for a location
     */
    public WeatherForecast getWeatherForecast(double lat, double lon) {
        String cacheKey = String.format("%.4f_%.4f", lat, lon);
        WeatherCache cached = weatherCache.get(cacheKey);

        if (isValidCache(cached)) {
            return cached.forecast;
        }

        String url = String.format(
            "http://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&appid=%s&units=metric",
            lat, lon, apiKey
        );

        WeatherForecast forecast = restTemplate.getForObject(url, WeatherForecast.class);
        if (forecast != null) {
            weatherCache.put(cacheKey, new WeatherCache(forecast, LocalDateTime.now()));
        }

        return forecast;
    }

    /**
     * Calculate weather risk score for a route
     */
    public double calculateWeatherRiskScore(List<WeatherPoint> weatherPoints) {
        double riskScore = 0.0;
        
        for (WeatherPoint point : weatherPoints) {
            switch (point.condition) {
                case RAIN:
                    riskScore += point.severity * 0.3;
                    break;
                case SNOW:
                    riskScore += point.severity * 0.8;
                    break;
                case FOG:
                    riskScore += point.severity * 0.5;
                    break;
                case STORM:
                    riskScore += point.severity * 1.0;
                    break;
                default:
                    break;
            }
        }
        
        return Math.min(1.0, riskScore);
    }

    /**
     * Suggest optimal departure time based on weather
     */
    public LocalDateTime suggestOptimalDepartureTime(
            double startLat, double startLon,
            double endLat, double endLon,
            LocalDateTime startWindow,
            LocalDateTime endWindow) {
        
        LocalDateTime bestTime = startWindow;
        double lowestRisk = Double.MAX_VALUE;
        
        LocalDateTime current = startWindow;
        while (current.isBefore(endWindow)) {
            List<WeatherPoint> weatherPoints = getWeatherAlongRoute(
                startLat, startLon,
                endLat, endLon,
                current
            );
            
            double risk = calculateWeatherRiskScore(weatherPoints);
            if (risk < lowestRisk) {
                lowestRisk = risk;
                bestTime = current;
            }
            
            current = current.plusMinutes(30); // Check every 30 minutes
        }
        
        return bestTime;
    }

    // Private helper methods

    private boolean isValidCache(WeatherCache cache) {
        return cache != null &&
               cache.timestamp.plusMinutes(CACHE_DURATION_MINUTES).isAfter(LocalDateTime.now());
    }

    private List<WeatherPoint> getWeatherAlongRoute(
            double startLat, double startLon,
            double endLat, double endLon,
            LocalDateTime time) {
        
        List<WeatherPoint> points = new ArrayList<>();
        int steps = 10; // Check weather at 10 points along route
        
        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            double lat = startLat + (endLat - startLat) * progress;
            double lon = startLon + (endLon - startLon) * progress;
            
            WeatherForecast forecast = getWeatherForecast(lat, lon);
            if (forecast != null) {
                points.add(new WeatherPoint(
                    lat, lon,
                    getWeatherCondition(forecast),
                    getWeatherSeverity(forecast)
                ));
            }
        }
        
        return points;
    }

    private List<RoutePoint> calculateBaseRoute(
            double startLat, double startLon,
            double endLat, double endLon) {
        // In a real implementation, this would use a routing engine like GraphHopper
        // For now, we'll create a simple straight-line route
        List<RoutePoint> route = new ArrayList<>();
        int steps = 20;
        
        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            route.add(new RoutePoint(
                startLat + (endLat - startLat) * progress,
                startLon + (endLon - startLon) * progress
            ));
        }
        
        return route;
    }

    private WeatherAwareRoute adjustRouteForWeather(
            List<RoutePoint> baseRoute,
            List<WeatherPoint> weatherPoints) {
        
        List<RoutePoint> adjustedRoute = new ArrayList<>(baseRoute);
        double riskScore = calculateWeatherRiskScore(weatherPoints);
        
        // If weather risk is high, try to find alternative route points
        if (riskScore > 0.5) {
            for (int i = 1; i < adjustedRoute.size() - 1; i++) {
                RoutePoint current = adjustedRoute.get(i);
                WeatherPoint nearestWeather = findNearestWeatherPoint(current, weatherPoints);
                
                if (nearestWeather != null && isHighRiskWeather(nearestWeather)) {
                    // Attempt to move route point away from bad weather
                    adjustRoutePoint(adjustedRoute, i, nearestWeather);
                }
            }
        }
        
        return new WeatherAwareRoute(
            adjustedRoute,
            weatherPoints,
            riskScore,
            generateWeatherAdvisories(weatherPoints)
        );
    }

    private WeatherPoint findNearestWeatherPoint(RoutePoint point, List<WeatherPoint> weatherPoints) {
        return weatherPoints.stream()
            .min(Comparator.comparingDouble(w -> 
                calculateDistance(point.lat, point.lon, w.lat, w.lon)))
            .orElse(null);
    }

    private boolean isHighRiskWeather(WeatherPoint point) {
        return point.severity > 0.7 || point.condition == WeatherCondition.STORM;
    }

    private void adjustRoutePoint(List<RoutePoint> route, int index, WeatherPoint weatherPoint) {
        RoutePoint current = route.get(index);
        // Move point perpendicular to the bad weather
        double angle = Math.atan2(
            weatherPoint.lon - current.lon,
            weatherPoint.lat - current.lat
        );
        double distance = 0.01; // About 1km
        
        route.set(index, new RoutePoint(
            current.lat + distance * Math.sin(angle + Math.PI/2),
            current.lon + distance * Math.cos(angle + Math.PI/2)
        ));
    }

    private List<String> generateWeatherAdvisories(List<WeatherPoint> weatherPoints) {
        List<String> advisories = new ArrayList<>();
        Map<WeatherCondition, Integer> conditionCounts = new HashMap<>();
        
        weatherPoints.forEach(point -> 
            conditionCounts.merge(point.condition, 1, Integer::sum)
        );
        
        conditionCounts.forEach((condition, count) -> {
            if (count > weatherPoints.size() / 3) { // Condition present in 1/3 of points
                advisories.add(getAdvisoryForCondition(condition));
            }
        });
        
        return advisories;
    }

    private String getAdvisoryForCondition(WeatherCondition condition) {
        switch (condition) {
            case RAIN:
                return "Expect rain along route. Maintain safe following distance.";
            case SNOW:
                return "Snow conditions expected. Consider winter equipment.";
            case FOG:
                return "Foggy conditions. Use fog lights and reduce speed.";
            case STORM:
                return "Severe weather warning. Consider postponing travel.";
            default:
                return "Check weather conditions before departure.";
        }
    }

    private WeatherCondition getWeatherCondition(WeatherForecast forecast) {
        // Implementation would parse the OpenWeatherMap response
        return WeatherCondition.CLEAR;
    }

    private double getWeatherSeverity(WeatherForecast forecast) {
        // Implementation would calculate severity from OpenWeatherMap data
        return 0.0;
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

    // Data classes
    @lombok.Data
    public static class WeatherAwareRoute {
        private final List<RoutePoint> route;
        private final List<WeatherPoint> weatherPoints;
        private final double riskScore;
        private final List<String> advisories;
    }

    @lombok.Data
    public static class RoutePoint {
        private final double lat;
        private final double lon;
    }

    @lombok.Data
    public static class WeatherPoint {
        private final double lat;
        private final double lon;
        private final WeatherCondition condition;
        private final double severity;
    }

    @lombok.Data
    public static class WeatherCache {
        private final WeatherForecast forecast;
        private final LocalDateTime timestamp;
    }

    @lombok.Data
    public static class WeatherForecast {
        // This would match the OpenWeatherMap API response structure
        // Implementation details would depend on the actual API response
    }

    public enum WeatherCondition {
        CLEAR, RAIN, SNOW, FOG, STORM
    }
}
