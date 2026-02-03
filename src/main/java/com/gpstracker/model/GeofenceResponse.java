package com.gpstracker.model;

public class GeofenceResponse {
    private String deviceId;
    private String status;
    private String message;

    public GeofenceResponse() {}

    public GeofenceResponse(String deviceId, String status, String message) {
        this.deviceId = deviceId;
        this.status = status;
        this.message = message;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
