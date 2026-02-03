package com.gpstracker.model;

import java.time.LocalDateTime;

public class GpsData {
    private String deviceId;
    private double latitude;
    private double longitude;
    private double speed;
    private double heading;
    private double batteryLevel;
    private double accuracy;
    private int signalStrength;
    private String networkType;
    private String additionalInfo;
    private String deviceStatus;
    private LocalDateTime timestamp;
    private boolean lowBattery;
    private boolean speedAlert;
    private boolean geofenceAlert;
    private boolean malfunctionAlert;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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
}
