package com.gpstracker.service;

import com.gpstracker.model.GpsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GpsDataService {

    private static final String GPS_DATA_KEY_PREFIX = "gps:data:";
    private static final int DATA_RETENTION_DAYS = 7;

    @Autowired
    private RedisTemplate<String, GpsData> redisTemplate;

    public void saveGpsData(GpsData gpsData) {
        String key = GPS_DATA_KEY_PREFIX + gpsData.getDeviceId() + ":" + 
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        redisTemplate.opsForValue().set(key, gpsData);
        redisTemplate.expire(key, DATA_RETENTION_DAYS, TimeUnit.DAYS);
    }

    public List<GpsData> getGpsDataForDevice(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        String keyPattern = GPS_DATA_KEY_PREFIX + deviceId + ":*";
        Set<String> keys = redisTemplate.keys(keyPattern);
        
        return keys.stream()
                  .map(key -> redisTemplate.opsForValue().get(key))
                  .filter(data -> {
                      LocalDateTime timestamp = data.getTimestamp();
                      return timestamp.isAfter(startTime) && timestamp.isBefore(endTime);
                  })
                  .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * 0") // Run at midnight every Sunday
    public void weeklyExport() {
        log.info("Starting weekly GPS data export");
        // Implementation for weekly export will be added
    }
}
