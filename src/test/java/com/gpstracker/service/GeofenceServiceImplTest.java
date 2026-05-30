package com.gpstracker.service;

import com.gpstracker.dto.PaginatedResponse;
import com.gpstracker.dto.PaginationRequest;
import com.gpstracker.model.CircleGeofence;
import com.gpstracker.model.Geofence;
import com.gpstracker.model.PolygonGeofence;
import com.gpstracker.service.impl.GeofenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GeofenceServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private GeofenceServiceImpl geofenceService;

    private List<Geofence> testGeofences;
    private final String TEST_DEVICE_ID = "test-device-123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test geofences
        testGeofences = new ArrayList<>();
        
        // Add 5 circle geofences
        for (int i = 0; i < 5; i++) {
            CircleGeofence circleGeofence = new CircleGeofence();
            circleGeofence.setId("circle-" + i);
            circleGeofence.setDeviceId(TEST_DEVICE_ID);
            circleGeofence.setName("Circle Geofence " + i);
            circleGeofence.setDescription("Test Circle Geofence " + i);
            circleGeofence.setCenterLatitude(40.0 + i * 0.1);
            circleGeofence.setCenterLongitude(-74.0 + i * 0.1);
            circleGeofence.setRadiusMeters(1000.0 + i * 100);
            circleGeofence.setCategory("test");
            circleGeofence.setAlertLevel(i % 3 + 1);
            circleGeofence.setActive(true);
            circleGeofence.setCreatedAt(LocalDateTime.now().minusDays(10 - i));
            circleGeofence.setUpdatedAt(LocalDateTime.now().minusDays(5 - i));
            
            testGeofences.add(circleGeofence);
        }
        
        // Add 5 polygon geofences
        for (int i = 0; i < 5; i++) {
            PolygonGeofence polygonGeofence = new PolygonGeofence();
            polygonGeofence.setId("polygon-" + i);
            polygonGeofence.setDeviceId(TEST_DEVICE_ID);
            polygonGeofence.setName("Polygon Geofence " + i);
            polygonGeofence.setDescription("Test Polygon Geofence " + i);
            polygonGeofence.setVertices(new ArrayList<>());
            polygonGeofence.setCategory("test");
            polygonGeofence.setAlertLevel(i % 3 + 1);
            polygonGeofence.setActive(true);
            polygonGeofence.setCreatedAt(LocalDateTime.now().minusDays(10 - i));
            polygonGeofence.setUpdatedAt(LocalDateTime.now().minusDays(5 - i));
            
            testGeofences.add(polygonGeofence);
        }
        
        // Set up Redis mocks
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Mock geofence IDs for the device
        Set<Object> geofenceIds = new HashSet<>();
        for (Geofence geofence : testGeofences) {
            geofenceIds.add(geofence.getId());
        }
        
        when(setOperations.members(anyString())).thenReturn(geofenceIds);
        
        // Mock retrieving individual geofences
        for (Geofence geofence : testGeofences) {
            when(valueOperations.get("geofence:" + geofence.getId())).thenReturn(geofence);
        }
        
        // Spy on the getGeofencesForDevice method
        GeofenceServiceImpl spyService = Mockito.spy(geofenceService);
        when(spyService.getGeofencesForDevice(TEST_DEVICE_ID)).thenReturn(testGeofences);
        geofenceService = spyService;
    }

    @Test
    void testGetGeofencesForDevicePaginated_FirstPage() {
        // Create pagination request for first page
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(0)
                .size(5)
                .build();
        
        // Mock the getGeofencesForDevice method
        Mockito.doReturn(testGeofences).when(geofenceService).getGeofencesForDevice(TEST_DEVICE_ID);
        
        // Call the method under test
        PaginatedResponse<Geofence> result = geofenceService.getGeofencesForDevicePaginated(TEST_DEVICE_ID, paginationRequest);
        
        // Verify the result
        assertNotNull(result);
        assertEquals(0, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(10, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertTrue(result.isFirst());
        assertFalse(result.isLast());
        assertEquals(5, result.getContent().size());
    }
    
    @Test
    void testGetGeofencesForDevicePaginated_SecondPage() {
        // Create pagination request for second page
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(1)
                .size(5)
                .build();
        
        // Mock the getGeofencesForDevice method
        Mockito.doReturn(testGeofences).when(geofenceService).getGeofencesForDevice(TEST_DEVICE_ID);
        
        // Call the method under test
        PaginatedResponse<Geofence> result = geofenceService.getGeofencesForDevicePaginated(TEST_DEVICE_ID, paginationRequest);
        
        // Verify the result
        assertNotNull(result);
        assertEquals(1, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(10, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertFalse(result.isFirst());
        assertTrue(result.isLast());
        assertEquals(5, result.getContent().size());
    }
    
    @Test
    void testGetGeofencesForDevicePaginated_WithSorting() {
        // Create pagination request with sorting by name
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(0)
                .size(10)
                .sortBy("name")
                .ascending(true)
                .build();
        
        // Mock the getGeofencesForDevice method
        Mockito.doReturn(testGeofences).when(geofenceService).getGeofencesForDevice(TEST_DEVICE_ID);
        
        // Call the method under test
        PaginatedResponse<Geofence> result = geofenceService.getGeofencesForDevicePaginated(TEST_DEVICE_ID, paginationRequest);
        
        // Verify the result
        assertNotNull(result);
        assertEquals(10, result.getContent().size());
        
        // First element should start with "C" (Circle) because of alphabetical sorting
        assertTrue(result.getContent().get(0).getName().startsWith("C"));
    }
    
    @Test
    void testGetGeofencesForDevicePaginated_EmptyResult() {
        // Mock an empty list
        Mockito.doReturn(new ArrayList<Geofence>()).when(geofenceService).getGeofencesForDevice("empty-device");
        
        // Create pagination request
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(0)
                .size(5)
                .build();
        
        // Call the method under test
        PaginatedResponse<Geofence> result = geofenceService.getGeofencesForDevicePaginated("empty-device", paginationRequest);
        
        // Verify the result
        assertNotNull(result);
        assertEquals(0, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertTrue(result.getContent().isEmpty());
    }
    
    @Test
    void testGetGeofencesForDevicePaginated_OutOfBoundsPage() {
        // Create pagination request for a page beyond the available data
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(5)  // Way beyond our test data
                .size(5)
                .build();
        
        // Mock the getGeofencesForDevice method
        Mockito.doReturn(testGeofences).when(geofenceService).getGeofencesForDevice(TEST_DEVICE_ID);
        
        // Call the method under test
        PaginatedResponse<Geofence> result = geofenceService.getGeofencesForDevicePaginated(TEST_DEVICE_ID, paginationRequest);
        
        // Verify the result
        assertNotNull(result);
        assertEquals(5, result.getPage());
        assertEquals(5, result.getSize());
        assertEquals(10, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertFalse(result.isFirst());
        assertTrue(result.isLast());
        assertTrue(result.getContent().isEmpty());
    }
} 