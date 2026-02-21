package com.gpstracker.model.fleet;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public class MaintenanceSchedule {
    private String vehicleId;
    private LocalDateTime lastMaintenance;
    private Duration maintenanceInterval;
    private Map<MaintenanceType, LocalDateTime> nextScheduled;

    public boolean isMaintenanceDue(LocalDateTime time) {
        return time.isAfter(lastMaintenance.plus(maintenanceInterval));
    }
    
    public double calculateUrgency() {
        Duration timeSinceLastMaintenance = Duration.between(lastMaintenance, LocalDateTime.now());
        return Math.min(1.0, timeSinceLastMaintenance.toHours() / maintenanceInterval.toHours());
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDateTime getLastMaintenance() {
        return lastMaintenance;
    }

    public void setLastMaintenance(LocalDateTime lastMaintenance) {
        this.lastMaintenance = lastMaintenance;
    }

    public Duration getMaintenanceInterval() {
        return maintenanceInterval;
    }

    public void setMaintenanceInterval(Duration maintenanceInterval) {
        this.maintenanceInterval = maintenanceInterval;
    }

    public Map<MaintenanceType, LocalDateTime> getNextScheduled() {
        return nextScheduled;
    }

    public void setNextScheduled(Map<MaintenanceType, LocalDateTime> nextScheduled) {
        this.nextScheduled = nextScheduled;
    }
}
