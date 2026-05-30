package com.gpstracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for PoliceAlert.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoliceAlertDTO {
    private String name;
    private Double latitude;
    private Double longitude;
    private Double radius;
    private String alertType;
    private String severity;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean active;
    private String source;
}