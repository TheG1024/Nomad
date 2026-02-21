package com.gpstracker.service;

import com.gpstracker.model.Anomaly;
import com.gpstracker.model.GpsData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyServiceTest {
    private final AnomalyService anomalyService = new AnomalyService();

    @Test
    void detectsHighSpeedAndLowBattery() {
        GpsData data = new GpsData();
        data.setSpeed(130.0);
        data.setBatteryLevel(0.1);
        data.setDeviceStatus("online");

        List<Anomaly> anomalies = anomalyService.detectAnomalies(data);

        assertThat(anomalies).hasSize(2);
        assertThat(anomalies)
                .extracting(Anomaly::getType)
                .contains("High Speed", "Low Battery");
    }

    @Test
    void flagsOfflineDevice() {
        GpsData data = new GpsData();
        data.setSpeed(10.0);
        data.setBatteryLevel(0.9);
        data.setDeviceStatus("offline");

        List<Anomaly> anomalies = anomalyService.detectAnomalies(data);

        assertThat(anomalies)
                .extracting(Anomaly::getType)
                .containsExactly("Device Offline");
    }
}
