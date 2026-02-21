package com.gpstracker.model.fleet;

import java.time.Duration;
import java.time.LocalDateTime;

public class DeliveryTask {
    private String id;
    private Location origin;
    private Location destination;
    private LocalDateTime deadline;
    private Duration estimatedDuration;
    private int priority;
    private double requiredCapacity;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Duration getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(Duration estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public double getRequiredCapacity() {
        return requiredCapacity;
    }

    public void setRequiredCapacity(double requiredCapacity) {
        this.requiredCapacity = requiredCapacity;
    }
}
