package com.gpstracker.controller;

import com.gpstracker.dto.ApiResponse;
import com.gpstracker.dto.PaginatedResponse;
import com.gpstracker.dto.PaginationRequest;
import com.gpstracker.model.CircleGeofence;
import com.gpstracker.model.Geofence;
import com.gpstracker.service.GeofenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class GeofenceControllerTest {

    @Mock
    private GeofenceService geofenceService;

    @InjectMocks
    private GeofenceController geofenceController;

    private List<Geofence> testGeofences;
    private final String TEST_DEVICE_ID = "test-device-123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test geofences
        testGeofences = new ArrayList<>();
        
        // Add 5 circle geofences for testing
        for (int i = 0; i < 5; i++) {
            CircleGeofence geofence = new CircleGeofence();
            geofence.setId("test-" + i);
            geofence.setName("Test Geofence " + i);
            geofence.setDeviceId(TEST_DEVICE_ID);
            testGeofences.add(geofence);
        }
    }

    @Test
    void testGetGeofencesForDevice() {
        // Mock service response
        when(geofenceService.getGeofencesForDevice(TEST_DEVICE_ID)).thenReturn(testGeofences);
        
        // Call controller method
        ResponseEntity<ApiResponse<List<Geofence>>> response = 
                geofenceController.getGeofencesForDevice(TEST_DEVICE_ID);
        
        // Assert response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(5, response.getBody().getData().size());
    }
    
    @Test
    void testGetGeofencesForDevicePaginated() {
        // Create paginated response
        PaginatedResponse<Geofence> paginatedResponse = PaginatedResponse.<Geofence>builder()
                .content(testGeofences)
                .page(0)
                .size(10)
                .totalElements(5)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
        
        // Mock service response
        when(geofenceService.getGeofencesForDevicePaginated(eq(TEST_DEVICE_ID), any(PaginationRequest.class)))
                .thenReturn(paginatedResponse);
        
        // Call controller method with updated signature
        ResponseEntity<ApiResponse<PaginatedResponse<Geofence>>> response = 
                geofenceController.getGeofencesForDevicePaginated(
                        TEST_DEVICE_ID, 0, 10, null, true,
                        null, null, null, null, null, null, null, null);
        
        // Assert response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        
        PaginatedResponse<Geofence> result = response.getBody().getData();
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertEquals(5, result.getContent().size());
    }
    
    @Test
    void testGetGeofencesForDevicePaginated_WithSorting() {
        // Create paginated response with sorting
        PaginatedResponse<Geofence> paginatedResponse = PaginatedResponse.<Geofence>builder()
                .content(testGeofences)
                .page(0)
                .size(10)
                .totalElements(5)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
        
        // Mock service response
        when(geofenceService.getGeofencesForDevicePaginated(eq(TEST_DEVICE_ID), any(PaginationRequest.class)))
                .thenReturn(paginatedResponse);
        
        // Call controller method with sorting and updated signature
        ResponseEntity<ApiResponse<PaginatedResponse<Geofence>>> response = 
                geofenceController.getGeofencesForDevicePaginated(
                        TEST_DEVICE_ID, 0, 10, "name", false,
                        null, null, null, null, null, null, null, null);
        
        // Assert response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        
        PaginatedResponse<Geofence> result = response.getBody().getData();
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(5, result.getTotalElements());
    }
    
    @Test
    void testGetGeofencesForDevicePaginated_EmptyResult() {
        // Create empty paginated response
        PaginatedResponse<Geofence> emptyResponse = PaginatedResponse.<Geofence>builder()
                .content(new ArrayList<>())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();
        
        // Mock service response
        when(geofenceService.getGeofencesForDevicePaginated(eq("empty-device"), any(PaginationRequest.class)))
                .thenReturn(emptyResponse);
        
        // Call controller method with updated signature
        ResponseEntity<ApiResponse<PaginatedResponse<Geofence>>> response = 
                geofenceController.getGeofencesForDevicePaginated(
                        "empty-device", 0, 10, null, true,
                        null, null, null, null, null, null, null, null);
        
        // Assert response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        
        PaginatedResponse<Geofence> result = response.getBody().getData();
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
        assertTrue(result.getContent().isEmpty());
    }
} 