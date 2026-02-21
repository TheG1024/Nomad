package com.gpstracker.model.fleet;

import java.time.Duration;
import java.util.List;

public class Route {
    private List<Location> points;
    private List<String> advisories;
    private double riskScore;
    private Duration estimatedDuration;
    private double fuelEfficiency;

    public Route() {}

    public Route(List<Location> points, List<String> advisories, double riskScore,
                 Duration estimatedDuration, double fuelEfficiency) {
        this.points = points;
        this.advisories = advisories;
        this.riskScore = riskScore;
        this.estimatedDuration = estimatedDuration;
        this.fuelEfficiency = fuelEfficiency;
    }

    public List<Location> getPoints() {
        return points;
    }

    public void setPoints(List<Location> points) {
        this.points = points;
    }

    public List<String> getAdvisories() {
        return advisories;
    }

    public void setAdvisories(List<String> advisories) {
        this.advisories = advisories;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public Duration getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(Duration estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public double getFuelEfficiency() {
        return fuelEfficiency;
    }

    public void setFuelEfficiency(double fuelEfficiency) {
        this.fuelEfficiency = fuelEfficiency;
    }
}
