package com.gpstracker.model.fleet;

import lombok.Data;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DriverSchedule {
    private final String driverId;
    private final List<Shift> shifts;
    private final Duration maxDailyDuration;
    
    public double calculateFatigueScore(LocalDateTime time) {
        // Calculate fatigue based on recent shifts
        double fatigue = 0.0;
        Duration totalDuration = Duration.ZERO;
        
        for (Shift shift : shifts) {
            if (shift.getEndTime().isAfter(time.minusHours(24))) {
                totalDuration = totalDuration.plus(shift.getDuration());
            }
        }
        
        fatigue = Math.min(1.0, totalDuration.toHours() / maxDailyDuration.toHours());
        return fatigue;
    }

    @Data
    public static class Shift {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final Duration duration;
    }
}
