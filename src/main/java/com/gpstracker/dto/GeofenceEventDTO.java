package com.gpstracker.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for geofence events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceEventDTO {
    private String deviceId;
    private String geofenceId;
    private String eventType; // "ENTER" or "EXIT"
    private long timestamp;
    private String deviceName;
    private String geofenceName;
} 