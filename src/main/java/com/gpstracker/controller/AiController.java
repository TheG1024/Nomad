package com.gpstracker.controller;

import com.gpstracker.model.GpsData;
import com.gpstracker.service.ai.PredictionService;
import com.gpstracker.service.ai.PredictionService.PredictedRoute;
import com.gpstracker.service.ai.PredictionService.Anomaly;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    @Autowired
    private PredictionService predictionService;

    @GetMapping("/predict/route")
    public ResponseEntity<RouteResponse> predictRoute(
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime) {
        
        try {
            List<PredictedRoute> predictions = predictionService.predictRoute(deviceId, startTime);
            
            // Transform predictions into a format suitable for the frontend
            List<RouteData> routes = predictions.stream()
                .map(route -> new RouteData(
                    new Location(route.getStartLat(), route.getStartLon()),
                    new Location(route.getEndLat(), route.getEndLon()),
                    route.getProbability()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(new RouteResponse(deviceId, routes, startTime));
        } catch (Exception e) {
            log.error("Error predicting route: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/detect/anomalies")
    public ResponseEntity<AnomalyResponse> detectAnomalies(
            @RequestParam String deviceId,
            @RequestBody GpsData currentData) {
        
        try {
            List<Anomaly> anomalies = predictionService.detectAnomalies(deviceId, currentData);
            
            // Transform anomalies into a format suitable for the frontend
            List<AnomalyData> anomalyData = anomalies.stream()
                .map(anomaly -> new AnomalyData(
                    anomaly.getType().toString(),
                    anomaly.getDescription(),
                    anomaly.getSeverity()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(new AnomalyResponse(
                deviceId,
                currentData.getTimestamp(),
                anomalyData,
                new Location(currentData.getLatitude(), currentData.getLongitude())
            ));
        } catch (Exception e) {
            log.error("Error detecting anomalies: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("Unhandled exception in AI controller: ", e);
        return ResponseEntity.internalServerError()
                .body("An unexpected error occurred processing the AI request");
    }

    // Response classes
    @lombok.Data
    static class RouteResponse {
        private final String deviceId;
        private final List<RouteData> predictions;
        private final LocalDateTime timestamp;
    }

    @lombok.Data
    static class RouteData {
        private final Location start;
        private final Location end;
        private final double probability;
    }

    @lombok.Data
    static class AnomalyResponse {
        private final String deviceId;
        private final LocalDateTime timestamp;
        private final List<AnomalyData> anomalies;
        private final Location location;
    }

    @lombok.Data
    static class AnomalyData {
        private final String type;
        private final String description;
        private final double severity;
    }

    @lombok.Data
    static class Location {
        private final double lat;
        private final double lon;
    }
}
