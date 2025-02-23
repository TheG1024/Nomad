package com.gpstracker.model.fleet;

import lombok.Data;
import java.time.Duration;
import java.util.List;

@Data
public class Route {
    private final List<Location> points;
    private final List<String> advisories;
    private final double riskScore;
    private final Duration estimatedDuration;
    private final double fuelEfficiency;
}
