package com.gpstracker.controller;

import com.gpstracker.service.scheduling.SmartSchedulingService;
import com.gpstracker.service.social.FleetSocialService;
import com.gpstracker.model.GpsData;
import com.gpstracker.model.fleet.*;
import com.gpstracker.model.fleet.social.FleetSocialData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/fleet")
@CrossOrigin(origins = "*")
public class FleetManagementController {

    @Autowired
    private SmartSchedulingService schedulingService;

    @Autowired
    private FleetSocialService socialService;

    @PostMapping("/schedule")
    public ResponseEntity<FleetSchedule> generateSchedule(
            @RequestParam String fleetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestBody List<DeliveryTask> tasks) {
        
        try {
            FleetSchedule schedule = schedulingService.generateSchedule(
                fleetId, startTime, endTime, tasks
            );
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            log.error("Error generating schedule: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/maintenance/schedule")
    public ResponseEntity<Void> scheduleVehicleMaintenance(
            @RequestParam String vehicleId,
            @RequestBody MaintenanceSchedule schedule) {
        
        try {
            schedulingService.scheduleVehicleMaintenance(vehicleId, schedule);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error scheduling maintenance: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/status/update")
    public ResponseEntity<Void> updateVehicleStatus(
            @RequestParam String vehicleId,
            @RequestParam double fuelLevel,
            @RequestBody GpsData location) {
        
        try {
            schedulingService.updateVehicleStatus(vehicleId, location, fuelLevel);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating vehicle status: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/social/share")
    public ResponseEntity<Void> shareFleetUpdate(
            @RequestParam String fleetId,
            @RequestParam String message,
            @RequestBody GpsData location) {
        
        try {
            socialService.shareStatus(fleetId, location, message);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error sharing fleet update: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/social/updates")
    public ResponseEntity<List<FleetSocialData.FleetUpdate>> getFleetUpdates(
            @RequestParam String fleetId) {
        
        try {
            List<FleetSocialData.FleetUpdate> updates = socialService.getFleetUpdates(fleetId);
            return ResponseEntity.ok(updates);
        } catch (Exception e) {
            log.error("Error getting fleet updates: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/eco/score")
    public ResponseEntity<FleetSocialData.EcoScore> getEcoScore(
            @RequestParam String vehicleId,
            @RequestBody GpsData currentData) {
        
        try {
            FleetSocialData.EcoScore score = socialService.calculateEcoScore(vehicleId, currentData);
            return ResponseEntity.ok(score);
        } catch (Exception e) {
            log.error("Error calculating eco score: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<FleetSocialData.LeaderboardEntry>> getLeaderboard(
            @RequestParam String fleetId) {
        
        try {
            List<FleetSocialData.LeaderboardEntry> leaderboard = socialService.getLeaderboard(fleetId);
            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            log.error("Error getting leaderboard: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<FleetSocialData.Achievement>> getAchievements(
            @RequestParam String deviceId) {
        
        try {
            List<FleetSocialData.Achievement> achievements = socialService.getAchievements(deviceId);
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            log.error("Error getting achievements: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("Unhandled exception in fleet management controller: ", e);
        return ResponseEntity.internalServerError()
                .body("An unexpected error occurred processing the request");
    }
}
