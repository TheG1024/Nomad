package com.gpstracker.service;

import com.gpstracker.model.RoutePrediction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PredictionServiceTest {
    private final PredictionService predictionService = new PredictionService();

    @Test
    void buildsDeterministicPredictions() {
        Instant startTime = Instant.parse("2024-03-01T10:15:30Z");
        List<RoutePrediction> firstRun = predictionService.buildPredictions("device-1", startTime);
        List<RoutePrediction> secondRun = predictionService.buildPredictions("device-1", startTime);

        assertThat(firstRun).hasSize(3);
        assertThat(secondRun).hasSize(3);
        assertThat(firstRun)
                .usingRecursiveComparison()
                .isEqualTo(secondRun);
        assertThat(firstRun)
                .allSatisfy(prediction -> assertThat(prediction.getProbability()).isBetween(0.0, 1.0));
    }
}
