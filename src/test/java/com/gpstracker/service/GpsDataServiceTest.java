package com.gpstracker.service;

import com.gpstracker.model.GpsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpsDataServiceTest {

    @Mock
    private RedisTemplate<String, GpsData> redisTemplate;

    @Mock
    private ValueOperations<String, GpsData> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ListOperations<String, GpsData> listOperations;

    @InjectMocks
    private GpsDataService gpsDataService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(hashOperations.entries(anyString())).thenReturn(Collections.emptyMap());
    }

    @Test
    void saveGpsDataStoresLastKeyAndSetsIdleStatusWhenNoPreviousData() {
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 1, 10, 0);
        GpsData gpsData = GpsData.builder()
                .deviceId("device-1")
                .speed(0.5)
                .timestamp(timestamp)
                .build();

        String lastKey = "gps:data:device-1:last";
        when(valueOperations.get(lastKey)).thenReturn(null);

        gpsDataService.saveGpsData(gpsData);

        assertThat(gpsData.getDeviceStatus()).isEqualTo("IDLE");
        verify(valueOperations).set(eq(lastKey), eq(gpsData));
    }

    @Test
    void saveGpsDataMarksDeviceOfflineWhenLastUpdateIsStale() {
        LocalDateTime previousTimestamp = LocalDateTime.of(2024, 1, 1, 9, 45);
        LocalDateTime currentTimestamp = LocalDateTime.of(2024, 1, 1, 10, 0);
        GpsData lastData = GpsData.builder()
                .deviceId("device-2")
                .timestamp(previousTimestamp)
                .build();
        GpsData currentData = GpsData.builder()
                .deviceId("device-2")
                .speed(12.0)
                .timestamp(currentTimestamp)
                .build();

        String lastKey = "gps:data:device-2:last";
        when(valueOperations.get(lastKey)).thenReturn(lastData);

        gpsDataService.saveGpsData(currentData);

        assertThat(currentData.getDeviceStatus()).isEqualTo("OFFLINE");
    }

    @Test
    void getLatestGpsDataReturnsOptional() {
        GpsData gpsData = GpsData.builder()
                .deviceId("device-3")
                .timestamp(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();
        String lastKey = "gps:data:device-3:last";
        when(valueOperations.get(lastKey)).thenReturn(gpsData);

        Optional<GpsData> result = gpsDataService.getLatestGpsData("device-3");

        assertThat(result).contains(gpsData);
    }
}
