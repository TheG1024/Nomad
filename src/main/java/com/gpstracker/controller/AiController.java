package com.gpstracker.controller;

import com.gpstracker.model.GpsData;
import com.gpstracker.service.ai.*;
import com.gpstracker.service.ai.BatteryOptimizationService.DeviceOptimizationProfile;
import com.gpstracker.service.ai.BatteryOptimizationService.DeviceOptimizationRecommendation;
import com.gpstracker.service.ai.BatteryOptimizationService.LocationOptimizationRecommendation;
import com.gpstracker.service.ai.GeofenceRecommendationService.GeofenceRecommendation;
import com.gpstracker.service.ai.IntelligentAlertingService.Alert;
import com.gpstracker.service.ai.IntelligentAlertingService.AlertRecommendation;
import com.gpstracker.service.ai.IntelligentAlertingService.AlertingConfiguration;
import com.gpstracker.service.ai.PatternLearningService.DevicePattern;
import com.gpstracker.service.ai.PredictionService.Anomaly;
import com.gpstracker.service.ai.PredictionService.PredictedRoute;
import com.gpstracker.service.ai.TripClassificationService.Trip;
import com.gpstracker.service.ai.TripClassificationService.TripClassification;
import com.gpstracker.service.ai.TripClassificationService.TripStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller that exposes AI features through API endpoints
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final PredictionService predictionService;
    private final PatternLearningService patternLearningService;
    private final GeofenceRecommendationService geofenceRecommendationService;
    private final TripClassificationService tripClassificationService;
    private final BatteryOptimizationService batteryOptimizationService;
    private final IntelligentAlertingService alertingService;

    //==============================
    // Pattern Learning Endpoints
    //==============================
    
    /**
     * Get movement patterns for a device
     *
     * @param deviceId Device ID to get patterns for
     * @return Device movement patterns
     */
    @GetMapping("/patterns/{deviceId}")
    public ResponseEntity<DevicePattern> getDevicePatterns(@PathVariable String deviceId) {
        DevicePattern pattern = patternLearningService.getPatternForDevice(deviceId);
        if (pattern == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pattern);
    }

    /**
     * Force update of movement patterns for a device
     *
     * @param deviceId Device ID to update patterns for
     * @return Success message
     */
    @PostMapping("/patterns/{deviceId}/update")
    public ResponseEntity<Map<String, String>> updateDevicePatterns(@PathVariable String deviceId) {
        patternLearningService.updatePatternForDevice(deviceId);
        return ResponseEntity.ok(Map.of("message", "Pattern update initiated for device " + deviceId));
    }

    //==============================
    // Prediction Endpoints
    //==============================
    
    /**
     * Get route predictions for a device
     *
     * @param deviceId Device ID to get predictions for
     * @return List of predicted routes
     */
    @GetMapping("/predictions/routes/{deviceId}")
    public ResponseEntity<List<PredictedRoute>> getPredictedRoutes(@PathVariable String deviceId) {
        List<PredictedRoute> routes = predictionService.predictRoute(deviceId);
        return ResponseEntity.ok(routes);
    }

    /**
     * Get anomaly detections for recent device data
     *
     * @param deviceId Device ID to check for anomalies
     * @return List of detected anomalies
     */
    @GetMapping("/predictions/anomalies/{deviceId}")
    public ResponseEntity<List<Anomaly>> getAnomalies(@PathVariable String deviceId) {
        List<Anomaly> anomalies = predictionService.detectAnomalies(deviceId);
        return ResponseEntity.ok(anomalies);
    }

    /**
     * Check a specific GPS data point for anomalies
     *
     * @param deviceId Device ID to check
     * @param data GPS data point to check
     * @return List of detected anomalies for this data point
     */
    @PostMapping("/predictions/anomalies/{deviceId}/check")
    public ResponseEntity<List<Anomaly>> checkDataPointForAnomalies(
            @PathVariable String deviceId,
            @RequestBody GpsData data) {
        List<Anomaly> anomalies = predictionService.detectAnomalies(deviceId, data);
        return ResponseEntity.ok(anomalies);
    }

    //==============================
    // Geofence Recommendation Endpoints
    //==============================
    
    /**
     * Get geofence recommendations for a device
     *
     * @param deviceId Device ID to get recommendations for
     * @param limit Maximum number of recommendations to return
     * @return List of geofence recommendations
     */
    @GetMapping("/recommendations/geofences/{deviceId}")
    public ResponseEntity<List<GeofenceRecommendation>> getGeofenceRecommendations(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "5") int limit) {
        List<GeofenceRecommendation> recommendations = 
                geofenceRecommendationService.getTopRecommendationsForDevice(deviceId, limit);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Clear cached geofence recommendations for a device
     *
     * @param deviceId Device ID to clear recommendations for
     * @return Success message
     */
    @DeleteMapping("/recommendations/geofences/{deviceId}/cache")
    public ResponseEntity<Map<String, String>> clearGeofenceRecommendationsCache(
            @PathVariable String deviceId) {
        geofenceRecommendationService.clearRecommendationsCache(deviceId);
        return ResponseEntity.ok(Map.of("message", "Geofence recommendations cache cleared for device " + deviceId));
    }

    //==============================
    // Trip Classification Endpoints
    //==============================
    
    /**
     * Get a specific trip
     *
     * @param deviceId Device ID
     * @param tripId Trip ID
     * @return Trip details
     */
    @GetMapping("/trips/{deviceId}/{tripId}")
    public ResponseEntity<Trip> getTrip(
            @PathVariable String deviceId,
            @PathVariable String tripId) {
        Trip trip = tripClassificationService.getTrip(deviceId, tripId);
        if (trip == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(trip);
    }

    /**
     * Classify trips for a device in a time range
     *
     * @param deviceId Device ID
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of classified trips
     */
    @GetMapping("/trips/{deviceId}")
    public ResponseEntity<List<Trip>> classifyTrips(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<Trip> trips = tripClassificationService.classifyTrips(deviceId, startTime, endTime);
        return ResponseEntity.ok(trips);
    }

    /**
     * Get trips by classification type
     *
     * @param deviceId Device ID
     * @param classification Trip classification to filter by
     * @param limit Maximum number of trips to return
     * @return List of trips matching the classification
     */
    @GetMapping("/trips/{deviceId}/classification/{classification}")
    public ResponseEntity<List<Trip>> getTripsByClassification(
            @PathVariable String deviceId,
            @PathVariable TripClassification classification,
            @RequestParam(defaultValue = "10") int limit) {
        List<Trip> trips = tripClassificationService.getTripsByClassification(deviceId, classification, limit);
        return ResponseEntity.ok(trips);
    }

    /**
     * Get trip statistics for a device
     *
     * @param deviceId Device ID
     * @return Trip statistics
     */
    @GetMapping("/trips/{deviceId}/statistics")
    public ResponseEntity<TripStatistics> getTripStatistics(@PathVariable String deviceId) {
        TripStatistics stats = tripClassificationService.getTripStatistics(deviceId);
        return ResponseEntity.ok(stats);
    }

    //==============================
    // Battery Optimization Endpoints
    //==============================
    
    /**
     * Get the optimal polling interval for a device
     *
     * @param deviceId Device ID
     * @param latitude Current latitude
     * @param longitude Current longitude
     * @param batteryLevel Current battery level
     * @param isMoving Whether the device is currently moving
     * @return Optimal polling interval in seconds
     */
    @GetMapping("/battery/polling-interval/{deviceId}")
    public ResponseEntity<Map<String, Integer>> getOptimalPollingInterval(
            @PathVariable String deviceId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double batteryLevel,
            @RequestParam boolean isMoving) {
        int interval = batteryOptimizationService.getOptimalPollingInterval(
                deviceId, latitude, longitude, batteryLevel, isMoving);
        return ResponseEntity.ok(Map.of("optimalIntervalSeconds", interval));
    }

    /**
     * Get battery optimization recommendations for a device
     *
     * @param deviceId Device ID
     * @return Optimization recommendations
     */
    @GetMapping("/battery/recommendations/{deviceId}")
    public ResponseEntity<DeviceOptimizationRecommendation> getBatteryOptimizationRecommendations(
            @PathVariable String deviceId) {
        DeviceOptimizationRecommendation recommendations = 
                batteryOptimizationService.getOptimizationRecommendations(deviceId);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Apply battery optimization recommendations
     *
     * @param deviceId Device ID
     * @param applyMoving Whether to apply moving interval recommendations
     * @param applyStationary Whether to apply stationary interval recommendations
     * @param applySleep Whether to apply sleep mode recommendations
     * @param applyLocationBased Whether to apply location-based recommendations
     * @return Success message
     */
    @PostMapping("/battery/recommendations/{deviceId}/apply")
    public ResponseEntity<Map<String, String>> applyBatteryOptimizationRecommendations(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "true") boolean applyMoving,
            @RequestParam(defaultValue = "true") boolean applyStationary,
            @RequestParam(defaultValue = "true") boolean applySleep,
            @RequestParam(defaultValue = "true") boolean applyLocationBased) {
        boolean success = batteryOptimizationService.applyOptimizationRecommendations(
                deviceId, applyMoving, applyStationary, applySleep, applyLocationBased);
        
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Battery optimization recommendations applied for device " + deviceId));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to apply battery optimization recommendations"));
        }
    }

    /**
     * Get current battery optimization profile for a device
     *
     * @param deviceId Device ID
     * @return Device optimization profile
     */
    @GetMapping("/battery/profile/{deviceId}")
    public ResponseEntity<DeviceOptimizationProfile> getBatteryOptimizationProfile(
            @PathVariable String deviceId) {
        DeviceOptimizationProfile profile = batteryOptimizationService.getOrCreateDeviceProfile(deviceId);
        return ResponseEntity.ok(profile);
    }

    //==============================
    // Intelligent Alerting Endpoints
    //==============================
    
    /**
     * Process a GPS data point and generate alerts
     *
     * @param data GPS data point to process
     * @return List of generated alerts
     */
    @PostMapping("/alerts/process")
    public ResponseEntity<List<Alert>> processDataForAlerts(@RequestBody GpsData data) {
        List<Alert> alerts = alertingService.processGpsDataPoint(data);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get current alert configuration for a device
     *
     * @param deviceId Device ID
     * @return Alert configuration
     */
    @GetMapping("/alerts/configuration/{deviceId}")
    public ResponseEntity<AlertingConfiguration> getAlertConfiguration(@PathVariable String deviceId) {
        AlertingConfiguration config = alertingService.getAlertConfiguration(deviceId);
        return ResponseEntity.ok(config);
    }

    /**
     * Configure alerts for a device
     *
     * @param deviceId Device ID
     * @param configuration Alert configuration
     * @return Updated configuration
     */
    @PutMapping("/alerts/configuration/{deviceId}")
    public ResponseEntity<AlertingConfiguration> configureAlerts(
            @PathVariable String deviceId,
            @RequestBody AlertingConfiguration configuration) {
        configuration.setDeviceId(deviceId);
        alertingService.configureAlerts(deviceId, configuration);
        return ResponseEntity.ok(configuration);
    }

    /**
     * Clear alert history for a device
     *
     * @param deviceId Device ID
     * @return Success message
     */
    @DeleteMapping("/alerts/history/{deviceId}")
    public ResponseEntity<Map<String, String>> clearAlertHistory(@PathVariable String deviceId) {
        alertingService.clearAlertHistory(deviceId);
        return ResponseEntity.ok(Map.of("message", "Alert history cleared for device " + deviceId));
    }

    /**
     * Get alert recommendations for a device
     *
     * @param deviceId Device ID
     * @return Alert recommendations
     */
    @GetMapping("/alerts/recommendations/{deviceId}")
    public ResponseEntity<AlertRecommendation> getAlertRecommendations(@PathVariable String deviceId) {
        AlertRecommendation recommendations = alertingService.getAlertRecommendations(deviceId);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Apply alert recommendations
     *
     * @param deviceId Device ID
     * @param recommendation Alert recommendations to apply
     * @return Updated configuration
     */
    @PostMapping("/alerts/recommendations/{deviceId}/apply")
    public ResponseEntity<AlertingConfiguration> applyAlertRecommendations(
            @PathVariable String deviceId,
            @RequestBody AlertRecommendation recommendation) {
        AlertingConfiguration updatedConfig = alertingService.applyRecommendations(deviceId, recommendation);
        return ResponseEntity.ok(updatedConfig);
    }
}
