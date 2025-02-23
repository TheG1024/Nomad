package com.gpstracker.model.fleet;

import lombok.Data;
import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class DeliveryTask {
    private final String id;
    private final Location origin;
    private final Location destination;
    private final LocalDateTime deadline;
    private final Duration estimatedDuration;
    private final int priority;
    private final double requiredCapacity;
}
