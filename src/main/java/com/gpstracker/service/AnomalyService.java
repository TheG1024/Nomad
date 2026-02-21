package com.gpstracker.service;

import com.gpstracker.model.Anomaly;
import com.gpstracker.model.GpsData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyService {
    public List<Anomaly> detectAnomalies(GpsData data) {
        List<Anomaly> anomalies = new ArrayList<>();

        if (data.getSpeed() > 120) {
            anomalies.add(new Anomaly(
                    "High Speed",
                    "Speed exceeded 120 km/h",
                    0.85
            ));
        }

        if (data.getBatteryLevel() > 0 && data.getBatteryLevel() < 0.2) {
            anomalies.add(new Anomaly(
                    "Low Battery",
                    "Battery level below 20%",
                    0.65
            ));
        }

        if (data.getDeviceStatus() != null && data.getDeviceStatus().equalsIgnoreCase("offline")) {
            anomalies.add(new Anomaly(
                    "Device Offline",
                    "Device reported offline status",
                    0.75
            ));
        }

        return anomalies;
    }
}
