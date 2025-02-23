package com.gpstracker.model.fleet;

import lombok.Data;
import java.time.*;
import java.util.Map;

@Data
public class MaintenanceSchedule {
    private final String vehicleId;
    private final LocalDateTime lastMaintenance;
    private final Duration maintenanceInterval;
    private final Map<MaintenanceType, LocalDateTime> nextScheduled;
    
    public boolean isMaintenanceDue(LocalDateTime time) {
        return time.isAfter(lastMaintenance.plus(maintenanceInterval));
    }
    
    public double calculateUrgency() {
        Duration timeSinceLastMaintenance = Duration.between(lastMaintenance, LocalDateTime.now());
        return Math.min(1.0, timeSinceLastMaintenance.toHours() / maintenanceInterval.toHours());
    }
}
