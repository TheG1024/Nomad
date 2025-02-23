package com.gpstracker.model.fleet;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScheduledTask {
    private final String taskId;
    private final String vehicleId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Location origin;
    private final Location destination;
    private final Route route;
}
