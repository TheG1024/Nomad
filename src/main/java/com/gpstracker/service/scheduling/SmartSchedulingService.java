package com.gpstracker.service.scheduling;

import com.gpstracker.service.weather.WeatherAwareRoutingService;
import com.gpstracker.model.GpsData;
import com.gpstracker.model.fleet.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SmartSchedulingService {

    @Autowired
    private WeatherAwareRoutingService weatherService;

    private final Map<String, MaintenanceSchedule> maintenanceSchedules = new ConcurrentHashMap<>();
    // private final Map<String, DriverSchedule> driverSchedules = new ConcurrentHashMap<>(); // Not used yet
    private final Map<String, VehicleStatus> vehicleStatuses = new ConcurrentHashMap<>();

    /**
     * Generate optimal schedule for fleet operations
     */
    public FleetSchedule generateSchedule(
            String fleetId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<DeliveryTask> tasks) {
        
        // Sort tasks by priority and deadline
        PriorityQueue<DeliveryTask> prioritizedTasks = new PriorityQueue<>(
            Comparator.<DeliveryTask>comparingInt(t -> t.getPriority())
                     .thenComparing(DeliveryTask::getDeadline)
        );
        prioritizedTasks.addAll(tasks);
        
        Map<String, List<ScheduledTask>> vehicleAssignments = new HashMap<>();
        Map<String, LocalDateTime> vehicleAvailability = new HashMap<>();
        
        // Process each task
        while (!prioritizedTasks.isEmpty()) {
            DeliveryTask task = prioritizedTasks.poll();
            
            // Find best vehicle and time slot
            VehicleTimeSlot bestSlot = findOptimalTimeSlot(
                task, startTime, endTime, vehicleAvailability
            );
            
            if (bestSlot != null) {
                // Create scheduled task
                ScheduledTask scheduledTask = createScheduledTask(task, bestSlot);
                
                // Update vehicle assignments
                vehicleAssignments.computeIfAbsent(bestSlot.getVehicleId(), k -> new ArrayList<>())
                                .add(scheduledTask);
                
                // Update vehicle availability
                vehicleAvailability.put(bestSlot.getVehicleId(), bestSlot.getEndTime());
            } else {
                log.warn("Could not schedule task: {}", task.getId());
            }
        }
        
        return new FleetSchedule(fleetId, vehicleAssignments);
    }

    /**
     * Schedule vehicle maintenance
     */
    public void scheduleVehicleMaintenance(String vehicleId, MaintenanceSchedule schedule) {
        maintenanceSchedules.put(vehicleId, schedule);
    }

    /**
     * Update vehicle status
     */
    public void updateVehicleStatus(String vehicleId, GpsData lastLocation, double fuelLevel) {
        vehicleStatuses.put(vehicleId, new VehicleStatus(
            vehicleId,
            lastLocation,
            fuelLevel,
            calculateMaintenanceUrgency(vehicleId)
        ));
    }

    // Helper methods

    private VehicleTimeSlot findOptimalTimeSlot(
            DeliveryTask task,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Map<String, LocalDateTime> vehicleAvailability) {
        
        List<String> availableVehicles = getAvailableVehicles(task.getRequiredCapacity());
        VehicleTimeSlot bestSlot = null;
        double bestScore = Double.MIN_VALUE;
        
        for (String vehicleId : availableVehicles) {
            LocalDateTime vehicleStartTime = vehicleAvailability.getOrDefault(vehicleId, startTime);
            if (vehicleStartTime.isAfter(task.getDeadline())) continue;
            
            // Check maintenance schedule
            MaintenanceSchedule maintenance = maintenanceSchedules.get(vehicleId);
            if (maintenance != null && maintenance.isMaintenanceDue(vehicleStartTime)) continue;
            
            // Calculate weather-optimal departure time
            LocalDateTime optimalDeparture = weatherService.suggestOptimalDepartureTime(
                task.getOrigin().getLat(), task.getOrigin().getLon(),
                task.getDestination().getLat(), task.getDestination().getLon(),
                vehicleStartTime,
                task.getDeadline()
            );
            
            // Check driver availability
            String assignedDriver = findAvailableDriver(vehicleId, optimalDeparture, task.getEstimatedDuration());
            if (assignedDriver == null) continue;
            
            // Calculate score for this slot
            double score = calculateTimeSlotScore(vehicleId, optimalDeparture, task);
            
            if (score > bestScore) {
                bestScore = score;
                bestSlot = new VehicleTimeSlot(
                    vehicleId,
                    assignedDriver,
                    optimalDeparture,
                    optimalDeparture.plus(task.getEstimatedDuration())
                );
            }
        }
        
        return bestSlot;
    }

    private ScheduledTask createScheduledTask(DeliveryTask task, VehicleTimeSlot slot) {
        Route route = calculateOptimalRoute(task, slot);
        
        return new ScheduledTask(
            task.getId(),
            slot.getVehicleId(),
            slot.getStartTime(),
            slot.getEndTime(),
            task.getOrigin(),
            task.getDestination(),
            route
        );
    }

    private Route calculateOptimalRoute(DeliveryTask task, VehicleTimeSlot slot) {
        // Get weather-aware route
        WeatherAwareRoutingService.WeatherAwareRoute weatherRoute = weatherService.calculateRoute(
            task.getOrigin().getLat(), task.getOrigin().getLon(),
            task.getDestination().getLat(), task.getDestination().getLon(),
            slot.getStartTime()
        );
        
        // Convert route points
        List<Location> points = weatherRoute.getRoute().stream()
            .map(p -> new Location(p.getLat(), p.getLon()))
            .collect(Collectors.toList());
        
        return new Route(
            points,
            weatherRoute.getAdvisories(),
            weatherRoute.getRiskScore(),
            task.getEstimatedDuration(),
            calculateFuelEfficiency(weatherRoute)
        );
    }

    private List<String> getAvailableVehicles(double requiredCapacity) {
        // Implementation would check vehicle database
        return new ArrayList<>();
    }

    private String findAvailableDriver(String vehicleId, LocalDateTime startTime, Duration duration) {
        // Implementation would check driver schedules and qualifications
        return null;
    }

    private double calculateTimeSlotScore(String vehicleId, LocalDateTime startTime, DeliveryTask task) {
        double score = 100.0;
        
        // Adjust for weather risk
        WeatherAwareRoutingService.WeatherAwareRoute weatherRoute = weatherService.calculateRoute(
            task.getOrigin().getLat(), task.getOrigin().getLon(),
            task.getDestination().getLat(), task.getDestination().getLon(),
            startTime
        );
        score -= weatherRoute.getRiskScore() * 20;
        
        // Adjust for vehicle efficiency
        VehicleStatus status = vehicleStatuses.get(vehicleId);
        if (status != null) {
            score -= (1.0 - status.getFuelLevel()) * 10;
            score -= status.getMaintenanceUrgency() * 25;
        }
        
        return Math.max(0, score);
    }

    private double calculateFuelEfficiency(WeatherAwareRoutingService.WeatherAwareRoute route) {
        // Base efficiency
        double efficiency = 1.0;
        
        // Adjust for weather conditions
        efficiency *= (1.0 - route.getRiskScore() * 0.2);
        
        return efficiency;
    }

    private double calculateMaintenanceUrgency(String vehicleId) {
        MaintenanceSchedule schedule = maintenanceSchedules.get(vehicleId);
        if (schedule == null) return 0.0;
        
        return schedule.calculateUrgency();
    }

    @lombok.Data
    private static class VehicleTimeSlot {
        private final String vehicleId;
        private final String driverId;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
    }

    @lombok.Data
    private static class VehicleStatus {
        private final String vehicleId;
        private final GpsData lastLocation;
        private final double fuelLevel;
        private final double maintenanceUrgency;
    }
}
