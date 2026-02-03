package com.gpstracker.service;

import com.gpstracker.model.GpsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);
        when(listOperations.leftPush(anyString(), any())).thenReturn(1L);
        lenient().when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(0L);
        lenient().when(hashOperations.increment(anyString(), anyString(), anyDouble())).thenReturn(0.0);
        lenient().when(hashOperations.get(anyString(), anyString())).thenReturn(null);
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
    void saveGpsDataStoresLastDataAndSetsStatus() {
        LocalDateTime now = LocalDateTime.now();
        GpsData lastData = GpsData.builder()
                .deviceId("device-1")
                .timestamp(now.minusMinutes(10))
                .speed(0.0)
                .build();
        when(valueOperations.get(eq("gps:data:device-1:last"))).thenReturn(lastData);

        GpsData currentData = GpsData.builder()
                .deviceId("device-1")
                .timestamp(now)
                .speed(15.0)
                .batteryLevel(0.5)
                .accuracy(5.0)
                .signalStrength(3)
                .build();

        gpsDataService.saveGpsData(currentData);

        assertThat(currentData.getDeviceStatus()).isEqualTo("OFFLINE");
        verify(valueOperations).set(eq("gps:data:device-1:last"), eq(currentData));
        verify(valueOperations, atLeastOnce()).set(anyString(), eq(currentData));
    }
}
