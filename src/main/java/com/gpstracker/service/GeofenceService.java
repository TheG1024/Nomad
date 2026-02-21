package com.gpstracker.service;

import com.gpstracker.model.GeofenceRequest;
import com.gpstracker.model.GeofenceResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeofenceService {
    private final Map<String, GeofenceRequest> geofences = new ConcurrentHashMap<>();

    public GeofenceResponse saveGeofence(GeofenceRequest request) {
        geofences.put(request.getDeviceId(), request);
        return new GeofenceResponse(
                request.getDeviceId(),
                "saved",
                String.format("Geofence saved with %.2f km radius", request.getRadius())
        );
    }

    public GeofenceRequest getGeofence(String deviceId) {
        return geofences.get(deviceId);
    }
}
