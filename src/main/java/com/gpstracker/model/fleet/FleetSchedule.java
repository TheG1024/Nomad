package com.gpstracker.model.fleet;

import java.util.List;
import java.util.Map;

public class FleetSchedule {
    private String fleetId;
    private Map<String, List<ScheduledTask>> vehicleAssignments;

    public FleetSchedule() {}

    public FleetSchedule(String fleetId, Map<String, List<ScheduledTask>> vehicleAssignments) {
        this.fleetId = fleetId;
        this.vehicleAssignments = vehicleAssignments;
    }

    public String getFleetId() {
        return fleetId;
    }

    public void setFleetId(String fleetId) {
        this.fleetId = fleetId;
    }

    public Map<String, List<ScheduledTask>> getVehicleAssignments() {
        return vehicleAssignments;
    }

    public void setVehicleAssignments(Map<String, List<ScheduledTask>> vehicleAssignments) {
        this.vehicleAssignments = vehicleAssignments;
    }
}
