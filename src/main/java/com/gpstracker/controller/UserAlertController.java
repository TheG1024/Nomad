package com.gpstracker.controller;

import com.gpstracker.model.alert.UserReportedAlert;
import com.gpstracker.service.alert.UserReportedAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-alerts")
@Slf4j
@CrossOrigin(origins = "*")
public class UserAlertController {
    
    @Autowired
    private UserReportedAlertService alertService;
    
    /**
     * Report a new Waze-style alert
     */
    @PostMapping("/report")
    public ResponseEntity<?> reportAlert(@RequestBody UserReportedAlert alert) {
        try {
            UserReportedAlert created = alertService.reportAlert(alert);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Alert reported successfully",
                "alert", created
            ));
        } catch (Exception e) {
            log.error("Error reporting alert", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to report alert: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Upvote an alert (confirm it exists)
     */
    @PostMapping("/{alertId}/upvote")
    public ResponseEntity<?> upvote(@PathVariable String alertId) {
        try {
            UserReportedAlert alert = alertService.upvote(alertId);
            if (alert != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Alert confirmed",
                    "alert", alert
                ));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error upvoting alert", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to upvote: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Downvote an alert (mark as incorrect)
     */
    @PostMapping("/{alertId}/downvote")
    public ResponseEntity<?> downvote(@PathVariable String alertId) {
        try {
            UserReportedAlert alert = alertService.downvote(alertId);
            if (alert != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", alert.getStatus() == UserReportedAlert.AlertStatus.REJECTED ? 
                        "Alert removed due to low confidence" : "Alert downvoted",
                    "alert", alert
                ));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error downvoting alert", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to downvote: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get alerts in a map viewport
     */
    @GetMapping("/area")
    public ResponseEntity<?> getAlertsInArea(
        @RequestParam double north,
        @RequestParam double south,
        @RequestParam double east,
        @RequestParam double west
    ) {
        try {
            List<UserReportedAlert> alerts = alertService.getAlertsInArea(north, south, east, west);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", alerts.size(),
                "alerts", alerts
            ));
        } catch (Exception e) {
            log.error("Error fetching alerts in area", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fetch alerts: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get nearby alerts for a location
     */
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyAlerts(
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestParam(defaultValue = "5.0") double radiusKm
    ) {
        try {
            List<UserReportedAlert> alerts = alertService.getNearbyAlerts(latitude, longitude, radiusKm);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", alerts.size(),
                "radiusKm", radiusKm,
                "alerts", alerts
            ));
        } catch (Exception e) {
            log.error("Error fetching nearby alerts", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to fetch nearby alerts: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Confirm an alert
     */
    @PostMapping("/{alertId}/confirm")
    public ResponseEntity<?> confirmAlert(@PathVariable String alertId) {
        try {
            UserReportedAlert alert = alertService.confirmAlert(alertId);
            if (alert != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Alert confirmed",
                    "alert", alert
                ));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error confirming alert", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to confirm: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Remove an alert
     */
    @DeleteMapping("/{alertId}")
    public ResponseEntity<?> removeAlert(@PathVariable String alertId) {
        try {
            alertService.removeAlert(alertId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Alert removed"
            ));
        } catch (Exception e) {
            log.error("Error removing alert", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to remove: " + e.getMessage()
            ));
        }
    }
}