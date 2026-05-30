package com.gpstracker.service.ai;

import com.gpstracker.model.GpsData;
import com.gpstracker.service.GpsDataService;
import com.gpstracker.service.ai.PatternLearningService.DevicePattern;
import com.gpstracker.service.ai.PatternLearningService.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatternLearningServiceTest {

    @Mock
    private GpsDataService gpsDataService;

    @InjectMocks
    private PatternLearningService patternLearningService;

    private final String deviceId = "device123";
    private List<GpsData> sampleGpsData;

    @BeforeEach
    void setUp() {
        // Create sample GPS data with patterns
        // Home location points (morning and evening)
        sampleGpsData = createSampleGpsData();
    }

    @Test
    void updatePatternForDevice_shouldAnalyzeGpsDataAndUpdatePattern() {
        // Setup mock response
        when(gpsDataService.getGpsDataForDevice(eq(deviceId), any(), any()))
                .thenReturn(sampleGpsData);

        // Execute
        patternLearningService.updatePatternForDevice(deviceId);

        // Verify that the service retrieved GPS data
        verify(gpsDataService).getGpsDataForDevice(eq(deviceId), any(), any());

        // Verify that a pattern was created and cached
        DevicePattern pattern = patternLearningService.getPatternForDevice(deviceId);
        assertNotNull(pattern);
        assertEquals(deviceId, pattern.getDeviceId());
        
        // Verify that home and work locations were identified
        assertNotNull(pattern.getHomeLocation());
        assertNotNull(pattern.getWorkLocation());
        
        // Home location should be around coordinates in the sample data
        assertTrue(isNear(pattern.getHomeLocation().getLatitude(), 40.7128, 0.001));
        assertTrue(isNear(pattern.getHomeLocation().getLongitude(), -74.006, 0.001));
        
        // Work location should be around coordinates in the sample data
        assertTrue(isNear(pattern.getWorkLocation().getLatitude(), 40.7500, 0.001));
        assertTrue(isNear(pattern.getWorkLocation().getLongitude(), -73.9967, 0.001));
        
        // Verify that frequent locations were identified
        assertFalse(pattern.getFrequentLocations().isEmpty());
        
        // Verify that daily routines were identified
        assertFalse(pattern.getRoutines().isEmpty());
    }
    
    @Test
    void getPatternForDevice_shouldReturnCachedPattern() {
        // Setup mock response
        when(gpsDataService.getGpsDataForDevice(eq(deviceId), any(), any()))
                .thenReturn(sampleGpsData);
        
        // Initial call should create the pattern
        DevicePattern initialPattern = patternLearningService.getPatternForDevice(deviceId);
        
        // Verify that GPS data was retrieved
        verify(gpsDataService).getGpsDataForDevice(eq(deviceId), any(), any());
        
        // Reset mock to verify no further calls
        reset(gpsDataService);
        
        // Second call should return the cached pattern without retrieving data again
        DevicePattern cachedPattern = patternLearningService.getPatternForDevice(deviceId);
        
        // Verify same pattern is returned
        assertSame(initialPattern, cachedPattern);
        
        // Verify no more calls to GPS data service
        verifyNoMoreInteractions(gpsDataService);
    }
    
    @Test
    void updateAllDevicePatterns_shouldProcessKnownDevices() {
        // Setup
        // First populate the cache with a pattern
        when(gpsDataService.getGpsDataForDevice(eq(deviceId), any(), any()))
                .thenReturn(sampleGpsData);
        patternLearningService.getPatternForDevice(deviceId);
        
        // Reset mock to verify scheduled update
        reset(gpsDataService);
        when(gpsDataService.getGpsDataForDevice(eq(deviceId), any(), any()))
                .thenReturn(sampleGpsData);
        
        // Execute scheduled method
        patternLearningService.updateAllDevicePatterns();
        
        // Verify GPS data was retrieved again
        verify(gpsDataService).getGpsDataForDevice(eq(deviceId), any(), any());
    }
    
    @Test
    void clearPatterns_shouldRemoveCachedPatterns() {
        // Setup - create a pattern in the cache
        when(gpsDataService.getGpsDataForDevice(eq(deviceId), any(), any()))
                .thenReturn(sampleGpsData);
        patternLearningService.getPatternForDevice(deviceId);
        
        // Execute
        patternLearningService.clearPatterns();
        
        // Reset mock to verify pattern is retrieved again
        reset(gpsDataService);
        when(gpsDataService.getGpsDataForDevice(eq(deviceId), any(), any()))
                .thenReturn(sampleGpsData);
        
        // Get pattern again - should require a data fetch
        patternLearningService.getPatternForDevice(deviceId);
        
        // Verify GPS data was retrieved again
        verify(gpsDataService).getGpsDataForDevice(eq(deviceId), any(), any());
    }
    
    // Helper methods
    
    private List<GpsData> createSampleGpsData() {
        List<GpsData> data = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Create 30 days of GPS data with patterns
        for (int day = 0; day < 30; day++) {
            LocalDateTime currentDay = now.minus(day, ChronoUnit.DAYS);
            
            // Home in the morning (7-8 AM)
            for (int i = 0; i < 12; i++) {
                data.add(createGpsPoint(deviceId, 
                        40.7128, -74.006, // NYC coordinates for home
                        0.0, // Not moving
                        currentDay.withHour(7).withMinute(i * 5)));
            }
            
            // Commute to work (8-9 AM)
            for (int i = 0; i < 12; i++) {
                double progressFactor = i / 11.0;
                double lat = 40.7128 + (40.7500 - 40.7128) * progressFactor;
                double lon = -74.006 + (-73.9967 + 74.006) * progressFactor;
                
                data.add(createGpsPoint(deviceId, 
                        lat, lon,
                        15.0, // Moving at 15 m/s
                        currentDay.withHour(8).withMinute(i * 5)));
            }
            
            // At work during the day (9 AM - 5 PM)
            for (int hour = 9; hour < 17; hour++) {
                for (int i = 0; i < 3; i++) {
                    data.add(createGpsPoint(deviceId, 
                            40.7500, -73.9967, // Work coordinates
                            0.0, // Not moving
                            currentDay.withHour(hour).withMinute(i * 20)));
                }
            }
            
            // Commute back home (5-6 PM)
            for (int i = 0; i < 12; i++) {
                double progressFactor = i / 11.0;
                double lat = 40.7500 - (40.7500 - 40.7128) * progressFactor;
                double lon = -73.9967 - (-73.9967 + 74.006) * progressFactor;
                
                data.add(createGpsPoint(deviceId, 
                        lat, lon,
                        15.0, // Moving at 15 m/s
                        currentDay.withHour(17).withMinute(i * 5)));
            }
            
            // Home in the evening (6-11 PM)
            for (int hour = 18; hour < 23; hour++) {
                for (int i = 0; i < 3; i++) {
                    data.add(createGpsPoint(deviceId, 
                            40.7128, -74.006, // Home coordinates
                            0.0, // Not moving
                            currentDay.withHour(hour).withMinute(i * 20)));
                }
            }
            
            // Add some gym visits (MWF at 6 PM)
            DayOfWeek dayOfWeek = currentDay.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.MONDAY || dayOfWeek == DayOfWeek.WEDNESDAY || dayOfWeek == DayOfWeek.FRIDAY) {
                for (int i = 0; i < 6; i++) {
                    data.add(createGpsPoint(deviceId, 
                            40.7300, -74.0100, // Gym coordinates
                            0.0, // Not moving
                            currentDay.withHour(18).withMinute(i * 10)));
                }
            }
            
            // Add some weekend activities
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                // Park visits
                for (int i = 0; i < 12; i++) {
                    data.add(createGpsPoint(deviceId, 
                            40.7800, -73.9700, // Park coordinates
                            1.5, // Walking speed
                            currentDay.withHour(14).withMinute(i * 5)));
                }
                
                // Shopping mall visits
                for (int i = 0; i < 12; i++) {
                    data.add(createGpsPoint(deviceId, 
                            40.7400, -74.0050, // Mall coordinates
                            0.5, // Slow walking
                            currentDay.withHour(16).withMinute(i * 5)));
                }
            }
        }
        
        return data;
    }
    
    private GpsData createGpsPoint(String deviceId, double latitude, double longitude, 
                                 double speed, LocalDateTime timestamp) {
        GpsData data = new GpsData();
        data.setDeviceId(deviceId);
        data.setLatitude(latitude);
        data.setLongitude(longitude);
        data.setSpeed(speed);
        data.setTimestamp(timestamp);
        data.setBatteryLevel(80.0); // Default battery level
        return data;
    }
    
    private boolean isNear(double value1, double value2, double tolerance) {
        return Math.abs(value1 - value2) <= tolerance;
    }
} 