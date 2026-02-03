package com.gpstracker.model;

public class DeviceAlert {
    private boolean lowBattery;
    private boolean speedAlert;
    private boolean geofenceAlert;
    private boolean malfunctionAlert;
    private String message;

    public DeviceAlert() {}

    public DeviceAlert(boolean lowBattery, boolean speedAlert, boolean geofenceAlert, boolean malfunctionAlert,
                       String message) {
        this.lowBattery = lowBattery;
        this.speedAlert = speedAlert;
        this.geofenceAlert = geofenceAlert;
        this.malfunctionAlert = malfunctionAlert;
        this.message = message;
    }

    public boolean isLowBattery() {
        return lowBattery;
    }

    public void setLowBattery(boolean lowBattery) {
        this.lowBattery = lowBattery;
    }

    public boolean isSpeedAlert() {
        return speedAlert;
    }

    public void setSpeedAlert(boolean speedAlert) {
        this.speedAlert = speedAlert;
    }

    public boolean isGeofenceAlert() {
        return geofenceAlert;
    }

    public void setGeofenceAlert(boolean geofenceAlert) {
        this.geofenceAlert = geofenceAlert;
    }

    public boolean isMalfunctionAlert() {
        return malfunctionAlert;
    }

    public void setMalfunctionAlert(boolean malfunctionAlert) {
        this.malfunctionAlert = malfunctionAlert;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
