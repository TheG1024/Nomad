package com.gpstracker.model.fleet;

import java.time.LocalDateTime;

public class ScheduledTask {
    private String taskId;
    private String vehicleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Location origin;
    private Location destination;
    private Route route;

    public ScheduledTask() {}

    public ScheduledTask(String taskId, String vehicleId, LocalDateTime startTime, LocalDateTime endTime,
                         Location origin, Location destination, Route route) {
        this.taskId = taskId;
        this.vehicleId = vehicleId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.origin = origin;
        this.destination = destination;
        this.route = route;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

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

    public Location getOrigin() {
        return origin;
    }

    public void setOrigin(Location origin) {
        this.origin = origin;
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }
}
