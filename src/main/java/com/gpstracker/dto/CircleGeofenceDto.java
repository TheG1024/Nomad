package com.gpstracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;

/**
 * Data Transfer Object for Circle Geofence creation and updates
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CircleGeofenceDto {

    private String deviceId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Latitude is required")
    private Double centerLatitude;

    @NotNull(message = "Longitude is required")
    private Double centerLongitude;

    @Positive(message = "Radius must be positive")
    private Double radiusMeters;

    private Integer alertLevel;
    private String category;
    private String color;

    private Map<String, Object> metadata;
}