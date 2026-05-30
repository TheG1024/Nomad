package com.gpstracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model class representing a police alert/government restriction zone.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoliceAlert {
    private String id;
    private String name;
    private Double latitude;
    private Double longitude;
    private Double radius; // in meters
    private String alertType; // "ROADBLOCK", "RADAR", "SURVEILLANCE", "CONTROVERSY"
    private String severity; // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean active;
    private String source; // "USER_REPORTED", "OFFICIAL", "AI_PREDICTION"
    private Integer reportCount;
}