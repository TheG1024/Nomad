package com.gpstracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * Data Transfer Object for Time Restriction requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeRestrictionDto {
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
} 