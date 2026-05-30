package com.gpstracker.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object for device location updates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceUpdateDTO {
    private String deviceId;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String status;
    private Integer batteryLevel;
    private Double speed;
    private String direction;
} 