package com.gpstracker.model.fleet.social;

import lombok.Data;
import java.time.LocalDateTime;

public class FleetSocialData {

    @Data
    public static class FleetUpdate {
        private final String deviceId;
        private final double latitude;
        private final double longitude;
        private final String message;
        private final LocalDateTime timestamp;
    }

    @Data
    public static class EcoScore {
        private final String deviceId;
        private final double score;
        private final double co2Savings;
        private final LocalDateTime timestamp;
    }

    @Data
    public static class Achievement {
        private final String deviceId;
        private final String name;
        private final String description;
        private final LocalDateTime unlocked;
        private final String icon;
    }

    @Data
    public static class LeaderboardEntry {
        private final int rank;
        private final String deviceId;
        private final double score;
    }
}
