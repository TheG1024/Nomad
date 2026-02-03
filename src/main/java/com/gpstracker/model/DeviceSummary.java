package com.gpstracker.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DeviceSummary {
    private String deviceId;
    private GpsData latestData;
    private Map<String, Object> dailyStatistics;
    private int recentAlertCount;
}
