package com.gpstracker.service.impl;

import com.gpstracker.model.PoliceAlert;
import com.gpstracker.service.PoliceAlertService;
import com.gpstracker.dto.PoliceAlertDTO;
import com.gpstracker.mapper.PoliceAlertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of PoliceAlertService using in-memory storage (since Redis is not available).
 */
@Slf4j
@Service
public class PoliceAlertServiceImpl implements PoliceAlertService {

    private final PoliceAlertMapper policeAlertMapper;
    
    // In-memory storage for police alerts
    private final Map<String, PoliceAlert> alertStorage = new ConcurrentHashMap<>();
    
    public PoliceAlertServiceImpl(PoliceAlertMapper policeAlertMapper) {
        this.policeAlertMapper = policeAlertMapper;
        initializeSampleAlerts();
    }

    private void initializeSampleAlerts() {
        List<PoliceAlert> sampleAlerts = List.of(
            PoliceAlert.builder()
                .id(UUID.randomUUID().toString())
                .name(" Roadblock - Main Street")
                .latitude(40.7128)
                .longitude(-74.0060)
                .radius(500.0)
                .alertType("ROADBLOCK")
                .severity("HIGH")
                .description("Police checkpoint established. Expect delays of 15-20 minutes.")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusHours(4))
                .active(true)
                .source("USER_REPORTED")
                .reportCount(12)
                .build(),
            
            PoliceAlert.builder()
                .id(UUID.randomUUID().toString())
                .name(" Speed Camera - Highway 95")
                .latitude(34.0522)
                .longitude(-118.2437)
                .radius(200.0)
                .alertType("RADAR")
                .severity("MEDIUM")
                .description("Mobile radar unit detected. Speed limit enforced at 45 mph.")
                .startTime(LocalDateTime.now())
                .endTime(null) // Ongoing
                .active(true)
                .source("OFFICIAL")
                .reportCount(45)
                .build(),
            
            PoliceAlert.builder()
                .id(UUID.randomUUID().toString())
                .name(" Surveillance Zone - Downtown")
                .latitude(51.5074)
                .longitude(-0.1278)
                .radius(1000.0)
                .alertType("SURVEILLANCE")
                .severity("LOW")
                .description("Enhanced CCTV coverage due to major event. License plate recognition active.")
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().plusHours(6))
                .active(true)
                .source("OFFICIAL")
                .reportCount(8)
                .build(),
            
            PoliceAlert.builder()
                .id(UUID.randomUUID().toString())
                .name(" Traffic Stop - Oakland Ave")
                .latitude(40.7589)
                .longitude(-73.9851)
                .radius(100.0)
                .alertType("CONTROVERSY")
                .severity("HIGH")
                .description("Arrest in progress. Area temporarily restricted. Use alternative route.")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusMinutes(30))
                .active(true)
                .source("USER_REPORTED")
                .reportCount(23)
                .build()
        );
        
        sampleAlerts.forEach(alert -> alertStorage.put(alert.getId(), alert));
        log.info("Initialized {} sample police alerts", sampleAlerts.size());
    }

    @Override
    @Transactional
    public PoliceAlert createAlert(PoliceAlertDTO dto) {
        String id = UUID.randomUUID().toString();
        PoliceAlert alert = policeAlertMapper.toEntity(dto);
        alert.setId(id);
        alert.setActive(true);
        alert.setStartTime(LocalDateTime.now());
        alert.setReportCount(0);
        alertStorage.put(id, alert);
        log.info("Created new police alert: {}", id);
        return alert;
    }

    @Override
    @Transactional
    public PoliceAlert updateAlert(String id, PoliceAlertDTO dto) {
        PoliceAlert existing = alertStorage.get(id);
        if (existing == null) {
            throw new RuntimeException("Police alert not found: " + id);
        }
        
        PoliceAlert updated = policeAlertMapper.toEntity(dto);
        updated.setId(id);
        updated.setStartTime(existing.getStartTime());
        updated.setActive(existing.isActive());
        updated.setReportCount(existing.getReportCount());
        alertStorage.put(id, updated);
        log.info("Updated police alert: {}", id);
        return updated;
    }

    @Override
    @Transactional
    public void deleteAlert(String id) {
        alertStorage.remove(id);
        log.info("Deleted police alert: {}", id);
    }

    @Override
    public PoliceAlert getAlert(String id) {
        return alertStorage.get(id);
    }

    @Override
    public List<PoliceAlert> getAllAlerts() {
        return new ArrayList<>(alertStorage.values());
    }

    @Override
    public List<PoliceAlert> getActiveAlerts() {
        return alertStorage.values().stream()
            .filter(PoliceAlert::isActive)
            .filter(alert -> {
                if (alert.getEndTime() == null) return true;
                return alert.getEndTime().isAfter(LocalDateTime.now());
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<PoliceAlert> getAlertsInRadius(double latitude, double longitude, double radiusMeters) {
        return alertStorage.values().stream()
            .filter(PoliceAlert::isActive)
            .filter(alert -> {
                double distance = calculateDistance(
                    latitude, longitude,
                    alert.getLatitude(), alert.getLongitude()
                );
                return distance <= alert.getRadius();
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PoliceAlert toggleAlert(String id) {
        PoliceAlert alert = alertStorage.get(id);
        if (alert == null) {
            throw new RuntimeException("Police alert not found: " + id);
        }
        alert.setActive(!alert.isActive());
        alertStorage.put(id, alert);
        log.info("Toggled police alert {} to active={}", id, alert.isActive());
        return alert;
    }

    @Override
    @Transactional
    public void reportAlert(String id) {
        PoliceAlert alert = alertStorage.get(id);
        if (alert == null) {
            throw new RuntimeException("Police alert not found: " + id);
        }
        alert.setReportCount(alert.getReportCount() + 1);
        alertStorage.put(id, alert);
        log.info("Reported police alert {}: total reports = {}", id, alert.getReportCount());
    }

    /**
     * Calculate distance between two coordinates using Haversine formula.
     * Returns distance in meters.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in meters
        
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}