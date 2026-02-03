package com.gpstracker.model.fleet.social;

import java.time.LocalDateTime;

public class FleetSocialData {

    public static class FleetUpdate {
        private String deviceId;
        private double latitude;
        private double longitude;
        private String message;
        private LocalDateTime timestamp;

        public FleetUpdate() {}

        public FleetUpdate(String deviceId, double latitude, double longitude, String message, LocalDateTime timestamp) {
            this.deviceId = deviceId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.message = message;
            this.timestamp = timestamp;
        }

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

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class EcoScore {
        private String deviceId;
        private double score;
        private double co2Savings;
        private LocalDateTime timestamp;

        public EcoScore() {}

        public EcoScore(String deviceId, double score, double co2Savings, LocalDateTime timestamp) {
            this.deviceId = deviceId;
            this.score = score;
            this.co2Savings = co2Savings;
            this.timestamp = timestamp;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public double getCo2Savings() {
            return co2Savings;
        }

        public void setCo2Savings(double co2Savings) {
            this.co2Savings = co2Savings;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class Achievement {
        private String deviceId;
        private String name;
        private String description;
        private LocalDateTime unlocked;
        private String icon;

        public Achievement() {}

        public Achievement(String deviceId, String name, String description, LocalDateTime unlocked, String icon) {
            this.deviceId = deviceId;
            this.name = name;
            this.description = description;
            this.unlocked = unlocked;
            this.icon = icon;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public LocalDateTime getUnlocked() {
            return unlocked;
        }

        public void setUnlocked(LocalDateTime unlocked) {
            this.unlocked = unlocked;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }
    }

    public static class LeaderboardEntry {
        private int rank;
        private String deviceId;
        private double score;

        public LeaderboardEntry() {}

        public LeaderboardEntry(int rank, String deviceId, double score) {
            this.rank = rank;
            this.deviceId = deviceId;
            this.score = score;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }
    }
}
