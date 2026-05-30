package com.gpstracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Data Transfer Object for pagination requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest {
    
    @Min(value = 0, message = "Page number must not be negative")
    @Builder.Default
    private Integer page = 0;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not be greater than 100")
    @Builder.Default
    private Integer size = 20;
    
    private String sortBy;
    
    @Builder.Default
    private Boolean ascending = true;
    
    // Filtering options
    private String nameFilter;
    private String categoryFilter;
    private Integer alertLevelFilter;
    private Boolean activeFilter;
    
    // Location-based filtering
    private Double minLatitude;
    private Double maxLatitude;
    private Double minLongitude;
    private Double maxLongitude;
    
    public static PaginationRequest defaultPagination() {
        return PaginationRequest.builder().build();
    }
} 