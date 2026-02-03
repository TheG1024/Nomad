package com.gpstracker.model;

import java.util.List;

public class AnomalyResponse {
    private String deviceId;
    private List<Anomaly> anomalies;

    public AnomalyResponse() {}

    public AnomalyResponse(String deviceId, List<Anomaly> anomalies) {
        this.deviceId = deviceId;
        this.anomalies = anomalies;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public List<Anomaly> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(List<Anomaly> anomalies) {
        this.anomalies = anomalies;
    }
}
