package com.gpstracker.controller;

import com.gpstracker.model.AnomalyResponse;
import com.gpstracker.model.GpsData;
import com.gpstracker.model.RoutePredictionResponse;
import com.gpstracker.service.AnomalyService;
import com.gpstracker.service.PredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final PredictionService predictionService;
    private final AnomalyService anomalyService;

    public AiController(PredictionService predictionService, AnomalyService anomalyService) {
        this.predictionService = predictionService;
        this.anomalyService = anomalyService;
    }

    @RequestMapping("/predict/route")
    public ResponseEntity<RoutePredictionResponse> predictRoute(
            @RequestParam String deviceId,
            @RequestParam(required = false) String startTime
    ) {
        Instant startInstant = Instant.now();
        if (startTime != null && !startTime.isBlank()) {
            try {
                startInstant = Instant.parse(startTime);
            } catch (DateTimeParseException ignored) {
                startInstant = Instant.now();
            }
        }

        RoutePredictionResponse response = new RoutePredictionResponse(
                deviceId,
                Instant.now().toString(),
                predictionService.buildPredictions(deviceId, startInstant)
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/detect/anomalies")
    public ResponseEntity<AnomalyResponse> detectAnomalies(
            @RequestParam String deviceId,
            @RequestBody GpsData gpsData
    ) {
        if (gpsData.getDeviceId() == null || gpsData.getDeviceId().isBlank()) {
            gpsData.setDeviceId(deviceId);
        }

        AnomalyResponse response = new AnomalyResponse(
                deviceId,
                anomalyService.detectAnomalies(gpsData)
        );

        return ResponseEntity.ok(response);
    }
}
