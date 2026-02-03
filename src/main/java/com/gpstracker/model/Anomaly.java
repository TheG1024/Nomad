package com.gpstracker.model;

public class Anomaly {
    private String type;
    private String description;
    private double severity;

    public Anomaly() {}

    public Anomaly(String type, String description, double severity) {
        this.type = type;
        this.description = description;
        this.severity = severity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getSeverity() {
        return severity;
    }

    public void setSeverity(double severity) {
        this.severity = severity;
    }
}
