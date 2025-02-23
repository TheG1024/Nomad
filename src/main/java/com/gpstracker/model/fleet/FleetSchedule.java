package com.gpstracker.model.fleet;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class FleetSchedule {
    private final String fleetId;
    private final Map<String, List<ScheduledTask>> vehicleAssignments;
}
