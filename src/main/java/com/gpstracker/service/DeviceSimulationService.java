package com.gpstracker.service;

import com.gpstracker.model.DeviceAlert;
import com.gpstracker.model.GpsData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class DeviceSimulationService {
    private static final double BASE_LAT = 37.7749;
    private static final double BASE_LON = -122.4194;

    public GpsData buildSampleData(String deviceId) {
        Random random = new Random(deviceId.hashCode());
        GpsData data = new GpsData();
        data.setDeviceId(deviceId);
        data.setLatitude(BASE_LAT + (random.nextDouble() - 0.5) * 0.05);
        data.setLongitude(BASE_LON + (random.nextDouble() - 0.5) * 0.05);
        data.setSpeed(25 + random.nextDouble() * 40);
        data.setHeading(random.nextDouble() * 360);
        data.setBatteryLevel(0.4 + random.nextDouble() * 0.6);
        data.setAccuracy(5 + random.nextDouble() * 20);
        data.setSignalStrength(3 + random.nextInt(3));
        data.setNetworkType("LTE");
        data.setAdditionalInfo("Simulated payload");
        data.setDeviceStatus("online");
        data.setTimestamp(LocalDateTime.now());
        return data;
    }

    public DeviceAlert buildSampleAlert(String deviceId) {
        return new DeviceAlert(false, false, false, false, "Device " + deviceId + " connected");
    }
}
