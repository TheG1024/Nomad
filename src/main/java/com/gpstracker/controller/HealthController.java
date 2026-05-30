package com.gpstracker.controller;

import com.gpstracker.config.RedisConfig;
import com.gpstracker.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for health and status checks
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final RedisConfig redisConfig;

    /**
     * Health check endpoint
     * 
     * @return Health status of the application
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthStatus>> healthCheck() {
        HealthStatus status = new HealthStatus();
        status.setStatus("UP");
        status.setVersion("1.0.0");
        
        Map<String, String> services = new HashMap<>();
        
        // Check Redis
        try {
            boolean fallbackActive = redisConfig.isFallbackActive();
            services.put("redis", fallbackActive ? "DOWN (using in-memory fallback)" : "UP");
        } catch (Exception e) {
            log.warn("Error checking Redis status: {}", e.getMessage());
            services.put("redis", "DOWN (error: " + e.getMessage() + ")");
        }
        
        // Add other service statuses as needed
        services.put("application", "UP");
        
        status.setServices(services);
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }
    
    @Data
    static class HealthStatus {
        private String status;
        private String version;
        private Map<String, String> services;
    }
} 