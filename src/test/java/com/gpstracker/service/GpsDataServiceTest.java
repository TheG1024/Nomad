package com.gpstracker.service;

import com.gpstracker.model.GpsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
    private SetOperations<String, Object> setOperations;

    @Mock
    private RedisTemplate<String, Object> objectRedisTemplate;

    @Mock
    private SetOperations<String, Object> objectSetOperations;

    @Mock
    private ListOperations<String, GpsData> listOperations;

    @InjectMocks
    private GpsDataService gpsDataService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(objectRedisTemplate.opsForSet()).thenReturn(objectSetOperations);
        when(hashOperations.entries(anyString())).thenReturn(Collections.emptyMap());
        lenient().when(hashOperations.get(anyString(), any())).thenReturn(null);
        lenient().when(hashOperations.increment(anyString(), eq("totalDistance"), anyDouble())).thenReturn(0.0);
        lenient().when(hashOperations.increment(anyString(), eq("dataPoints"), anyLong())).thenReturn(1L);
        lenient().when(hashOperations.increment(anyString(), eq("alerts"), anyLong())).thenReturn(0L);
    }

    @Test
    void saveGpsDataStoresLastKnownData() {
        LocalDateTime timestamp = LocalDateTime.of(2023, 1, 1, 10, 0);
        GpsData gpsData = GpsData.builder()
            .deviceId("device-1")
            .latitude(10.0)
            .longitude(10.0)
            .speed(20.0)
            .heading(0.0)
            .batteryLevel(1.0)
            .accuracy(1.0)
            .signalStrength(5)
            .timestamp(timestamp)
            .build();

        gpsDataService.saveGpsData(gpsData);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), eq(gpsData));
        verify(objectSetOperations).add(eq("gps:devices"), eq("device-1"));

        List<String> keys = keyCaptor.getAllValues();
        assertThat(keys).anyMatch(key -> key.startsWith("gps:data:device-1:"));
        assertThat(keys).anyMatch(key -> key.equals("gps:data:device-1:last"));
    }

    @Test
    void getGpsDataForDeviceReturnsEmptyWhenNoKeys() {
        when(redisTemplate.keys(anyString())).thenReturn(null);

        List<GpsData> result = gpsDataService.getGpsDataForDevice(
            "device-1",
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void getGpsDataForDeviceIncludesBoundaryTimestamps() {
        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2023, 1, 1, 11, 0);
        GpsData startData = GpsData.builder().deviceId("device-1").timestamp(start).build();
        GpsData endData = GpsData.builder().deviceId("device-1").timestamp(end).build();

        when(redisTemplate.keys(anyString())).thenReturn(Set.of("k1", "k2"));
        when(valueOperations.get("k1")).thenReturn(startData);
        when(valueOperations.get("k2")).thenReturn(endData);

        List<GpsData> result = gpsDataService.getGpsDataForDevice("device-1", start, end);

        assertThat(result).containsExactly(startData, endData);
    }

    @Test
    void getLatestGpsDataReturnsOptional() {
        GpsData gpsData = GpsData.builder().deviceId("device-1").build();
        when(valueOperations.get("gps:data:device-1:last")).thenReturn(gpsData);

        Optional<GpsData> result = gpsDataService.getLatestGpsData("device-1");

        assertThat(result).contains(gpsData);
    }
}
