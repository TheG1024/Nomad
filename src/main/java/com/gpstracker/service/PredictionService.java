package com.gpstracker.service;

import com.gpstracker.model.RoutePoint;
import com.gpstracker.model.RoutePrediction;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Service
public class PredictionService {
    private static final double BASE_LAT = 37.7749;
    private static final double BASE_LON = -122.4194;

    public List<RoutePrediction> buildPredictions(String deviceId, Instant startTime) {
        Random random = new Random(Objects.hash(deviceId, startTime));
        List<RoutePrediction> predictions = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            double latOffset = (random.nextDouble() - 0.5) * 0.2;
            double lonOffset = (random.nextDouble() - 0.5) * 0.2;
            double endLatOffset = (random.nextDouble() - 0.5) * 0.4;
            double endLonOffset = (random.nextDouble() - 0.5) * 0.4;
            double probability = Math.min(0.9, 0.45 + (random.nextDouble() * 0.5));

            RoutePoint start = new RoutePoint(BASE_LAT + latOffset, BASE_LON + lonOffset);
            RoutePoint end = new RoutePoint(BASE_LAT + endLatOffset, BASE_LON + endLonOffset);
            predictions.add(new RoutePrediction(start, end, probability));
        }

        return predictions;
    }
}
