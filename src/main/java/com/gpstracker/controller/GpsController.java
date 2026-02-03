package com.gpstracker.controller;

import com.gpstracker.model.GeofenceRequest;
import com.gpstracker.model.GeofenceResponse;
import com.gpstracker.service.GeofenceService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gps")
public class GpsController {
    private final GeofenceService geofenceService;

    public GpsController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @PostMapping(value = "/geofence", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<GeofenceResponse> setGeofence(
            @RequestParam String deviceId,
            @RequestParam double centerLat,
            @RequestParam double centerLon,
            @RequestParam double radius
    ) {
        GeofenceRequest request = new GeofenceRequest();
        request.setDeviceId(deviceId);
        request.setCenterLat(centerLat);
        request.setCenterLon(centerLon);
        request.setRadius(radius);

        return ResponseEntity.ok(geofenceService.saveGeofence(request));
    }

    @GetMapping("/geofence")
    public ResponseEntity<GeofenceRequest> getGeofence(@RequestParam String deviceId) {
        GeofenceRequest request = geofenceService.getGeofence(deviceId);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(request);
    }
}
