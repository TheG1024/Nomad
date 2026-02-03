package com.gpstracker.model;

public class RoutePrediction {
    private RoutePoint start;
    private RoutePoint end;
    private double probability;

    public RoutePrediction() {}

    public RoutePrediction(RoutePoint start, RoutePoint end, double probability) {
        this.start = start;
        this.end = end;
        this.probability = probability;
    }

    public RoutePoint getStart() {
        return start;
    }

    public void setStart(RoutePoint start) {
        this.start = start;
    }

    public RoutePoint getEnd() {
        return end;
    }

    public void setEnd(RoutePoint end) {
        this.end = end;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }
}
