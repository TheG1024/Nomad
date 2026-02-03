package com.gpstracker.model.fleet;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class DriverSchedule {
    private String driverId;
    private List<Shift> shifts;
    private Duration maxDailyDuration;

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

    public static class Shift {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Duration duration;

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public List<Shift> getShifts() {
        return shifts;
    }

    public void setShifts(List<Shift> shifts) {
        this.shifts = shifts;
    }

    public Duration getMaxDailyDuration() {
        return maxDailyDuration;
    }

    public void setMaxDailyDuration(Duration maxDailyDuration) {
        this.maxDailyDuration = maxDailyDuration;
    }
}
