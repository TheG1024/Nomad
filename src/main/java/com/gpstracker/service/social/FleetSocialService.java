package com.gpstracker.service.social;

import com.gpstracker.model.GpsData;
import com.gpstracker.model.fleet.social.FleetSocialData.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FleetSocialService {

    private static final String FLEET_KEY_PREFIX = "fleet:";
    private static final String ACHIEVEMENT_KEY_PREFIX = "achievement:";
    private static final String LEADERBOARD_KEY_PREFIX = "leaderboard:";
    private static final String ECO_SCORE_KEY_PREFIX = "eco:";
    private static final int DATA_RETENTION_DAYS = 7;

    @Autowired
    private RedisTemplate<String, Object> redisTemplateObject;

    public void shareStatus(String fleetId, GpsData data, String message) {
        FleetUpdate update = new FleetUpdate(
            data.getDeviceId(),
            data.getLatitude(),
            data.getLongitude(),
            message,
            LocalDateTime.now()
        );
        
        String key = FLEET_KEY_PREFIX + fleetId;
        redisTemplateObject.opsForList().leftPush(key, update);
        redisTemplateObject.opsForList().trim(key, 0, 99); // Keep last 100 updates
        redisTemplateObject.expire(key, DATA_RETENTION_DAYS, TimeUnit.DAYS);
    }

    public List<FleetUpdate> getFleetUpdates(String fleetId) {
        String key = FLEET_KEY_PREFIX + fleetId;
        return Optional.ofNullable(redisTemplateObject.opsForList().range(key, 0, -1))
                      .orElse(Collections.emptyList())
                      .stream()
                      .map(obj -> (FleetUpdate) obj)
                      .collect(Collectors.toList());
    }

    public EcoScore calculateEcoScore(String deviceId, GpsData data) {
        double score = 100.0;
        
        // Calculate eco score based on various factors
        if (data.getSpeed() > 120.0) { // Speed penalty
            score -= 10.0;
        }
        
        if (data.getSpeed() < 1.0 && data.getDeviceStatus().equals("IDLE")) { // Idle penalty
            score -= 5.0;
        }
        
        // Store and return eco score
        EcoScore ecoScore = new EcoScore(
            deviceId,
            Math.max(0.0, score),
            calculateCO2Savings(score),
            LocalDateTime.now()
        );
        
        String key = ECO_SCORE_KEY_PREFIX + deviceId;
        redisTemplateObject.opsForValue().set(key, ecoScore);
        redisTemplateObject.expire(key, DATA_RETENTION_DAYS, TimeUnit.DAYS);
        
        return ecoScore;
    }

    public void updateLeaderboard(String fleetId, String deviceId, double score) {
        String key = LEADERBOARD_KEY_PREFIX + fleetId;
        redisTemplateObject.opsForZSet().add(key, deviceId, score);
        redisTemplateObject.expire(key, DATA_RETENTION_DAYS, TimeUnit.DAYS);
    }

    public List<LeaderboardEntry> getLeaderboard(String fleetId) {
        String key = LEADERBOARD_KEY_PREFIX + fleetId;
        Set<Object> topDrivers = redisTemplateObject.opsForZSet().reverseRange(key, 0, 9);
        
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (topDrivers != null) {
            int rank = 1;
            for (Object deviceId : topDrivers) {
                Double score = redisTemplateObject.opsForZSet().score(key, deviceId);
                entries.add(new LeaderboardEntry(
                    rank++,
                    (String) deviceId,
                    score != null ? score : 0.0
                ));
            }
        }
        return entries;
    }

    public List<Achievement> getAchievements(String deviceId) {
        String key = ACHIEVEMENT_KEY_PREFIX + deviceId;
        return Optional.ofNullable(redisTemplateObject.opsForSet().members(key))
                      .orElse(Collections.emptySet())
                      .stream()
                      .map(obj -> (Achievement) obj)
                      .collect(Collectors.toList());
    }

    private double calculateCO2Savings(double ecoScore) {
        // Base CO2 emission for a typical vehicle is about 120 g/km
        double baseCO2 = 120.0;
        // Calculate savings based on eco score (0-100)
        return baseCO2 * (ecoScore / 100.0);
    }
}
