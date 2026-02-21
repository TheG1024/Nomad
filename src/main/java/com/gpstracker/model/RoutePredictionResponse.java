package com.gpstracker.model;

import java.util.List;

public class RoutePredictionResponse {
    private String deviceId;
    private String generatedAt;
    private List<RoutePrediction> predictions;

    public RoutePredictionResponse() {}

    public RoutePredictionResponse(String deviceId, String generatedAt, List<RoutePrediction> predictions) {
        this.deviceId = deviceId;
        this.generatedAt = generatedAt;
        this.predictions = predictions;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<RoutePrediction> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<RoutePrediction> predictions) {
        this.predictions = predictions;
    }
}
