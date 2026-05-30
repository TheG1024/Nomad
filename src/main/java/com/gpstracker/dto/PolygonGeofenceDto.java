package com.gpstracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Data Transfer Object for Polygon Geofence requests
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PolygonGeofenceDto {
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    @NotNull(message = "Vertices are required")
    @Size(min = 3, message = "A polygon must have at least 3 vertices")
    @Valid
    private List<GeoPointDto> vertices;
    
    private String category;
    
    @Min(value = 1, message = "Alert level must be at least 1")
    @Max(value = 3, message = "Alert level must be at most 3")
    @Builder.Default
    private Integer alertLevel = 1; // Default to info level
    
    private String color; // Optional color for UI display
} 