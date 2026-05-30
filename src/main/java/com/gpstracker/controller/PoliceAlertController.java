package com.gpstracker.controller;

import com.gpstracker.dto.PoliceAlertDTO;
import com.gpstracker.model.PoliceAlert;
import com.gpstracker.service.PoliceAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for police alert management.
 */
@Slf4j
@RestController
@RequestMapping("/api/police-alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PoliceAlertController {

    private final PoliceAlertService policeAlertService;

    /**
     * Get all police alerts.
     */
    @GetMapping
    public ResponseEntity<List<PoliceAlert>> getAllAlerts() {
        List<PoliceAlert> alerts = policeAlertService.getAllAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get active police alerts only.
     */
    @GetMapping("/active")
    public ResponseEntity<List<PoliceAlert>> getActiveAlerts() {
        List<PoliceAlert> alerts = policeAlertService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts within a specific radius of a location.
     */
    @GetMapping("/near")
    public ResponseEntity<List<PoliceAlert>> getAlertsNear(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5000") double radiusMeters) {
        List<PoliceAlert> alerts = policeAlertService.getAlertsInRadius(latitude, longitude, radiusMeters);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get a specific police alert by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PoliceAlert> getAlert(@PathVariable String id) {
        PoliceAlert alert = policeAlertService.getAlert(id);
        if (alert == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(alert);
    }

    /**
     * Create a new police alert.
     */
    @PostMapping
    public ResponseEntity<PoliceAlert> createAlert(@RequestBody PoliceAlertDTO dto) {
        PoliceAlert alert = policeAlertService.createAlert(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(alert);
    }

    /**
     * Update an existing police alert.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PoliceAlert> updateAlert(@PathVariable String id, @RequestBody PoliceAlertDTO dto) {
        try {
            PoliceAlert alert = policeAlertService.updateAlert(id, dto);
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a police alert.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable String id) {
        policeAlertService.deleteAlert(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Toggle the active status of a police alert.
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<PoliceAlert> toggleAlert(@PathVariable String id) {
        PoliceAlert alert = policeAlertService.toggleAlert(id);
        return ResponseEntity.ok(alert);
    }

    /**
     * Report a police alert (increment report count).
     */
    @PostMapping("/{id}/report")
    public ResponseEntity<PoliceAlert> reportAlert(@PathVariable String id) {
        policeAlertService.reportAlert(id);
        PoliceAlert alert = policeAlertService.getAlert(id);
        return ResponseEntity.ok(alert);
    }
}