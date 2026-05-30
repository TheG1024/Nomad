package com.gpstracker.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpstracker.dto.ApiResponse;
import com.gpstracker.dto.CircleGeofenceDto;
import com.gpstracker.dto.PolygonGeofenceDto;
import com.gpstracker.dto.GeoPointDto;
import com.gpstracker.model.CircleGeofence;
import com.gpstracker.model.PolygonGeofence;
import com.gpstracker.NomadGpsApplication;
import com.gpstracker.model.Geofence;
import com.gpstracker.service.GeofenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the GeofenceController
 * These tests verify the end-to-end flow of the API, including database
 * interactions
 */
@SpringBootTest(classes = NomadGpsApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GeofenceControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private GeofenceService geofenceService;

        private final String TEST_DEVICE_ID = "test-device-" + UUID.randomUUID();
        private CircleGeofenceDto circleGeofenceDto;
        private PolygonGeofenceDto polygonGeofenceDto;

        @BeforeEach
        void setUp() {
                // Create test circle geofence DTO
                circleGeofenceDto = CircleGeofenceDto.builder()
                                .deviceId(TEST_DEVICE_ID)
                                .name("Downtown Delivery Zone")
                                .description("Primary delivery area for lower Manhattan")
                                .centerLatitude(40.712776)
                                .centerLongitude(-74.005974)
                                .radiusMeters(500.0)
                                .category("logistics")
                                .alertLevel(1)
                                .color("#FF0000")
                                .build();

                // Create test polygon geofence DTO
                List<GeoPointDto> vertices = new ArrayList<>();
                vertices.add(GeoPointDto.builder().latitude(40.712776).longitude(-74.005974).build());
                vertices.add(GeoPointDto.builder().latitude(40.712776).longitude(-73.997927).build());
                vertices.add(GeoPointDto.builder().latitude(40.704877).longitude(-73.997927).build());
                vertices.add(GeoPointDto.builder().latitude(40.704877).longitude(-74.005974).build());

                polygonGeofenceDto = PolygonGeofenceDto.builder()
                                .deviceId(TEST_DEVICE_ID)
                                .name("Upper East Side Logistics Area")
                                .description("Secondary logistics zone for uptown operations")
                                .vertices(vertices)
                                .category("logistics")
                                .alertLevel(1)
                                .color("#00FF00")
                                .build();
        }

        @Test
        @WithMockUser
        void testCreateCircleGeofence() throws Exception {
                String content = objectMapper.writeValueAsString(circleGeofenceDto);

                MvcResult result = mockMvc.perform(post("/api/geofences/circle")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").exists())
                                .andExpect(jsonPath("$.data.id").isNotEmpty())
                                .andExpect(jsonPath("$.data.deviceId").value(TEST_DEVICE_ID))
                                .andExpect(jsonPath("$.data.name").value(circleGeofenceDto.getName()))
                                .andExpect(jsonPath("$.data.active").value(true))
                                .andReturn();

                // Extract the ID for subsequent tests
                ApiResponse<CircleGeofence> response = objectMapper.readValue(
                                result.getResponse().getContentAsString(),
                                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class,
                                                CircleGeofence.class));

                assertNotNull(response.getData().getId());

                // Verify that the geofence exists in the database
                List<Geofence> geofences = geofenceService.getGeofencesForDevice(TEST_DEVICE_ID);
                assertFalse(geofences.isEmpty());
                assertTrue(geofences.stream().anyMatch(g -> g.getName().equals(circleGeofenceDto.getName())));
        }

        @Test
        @WithMockUser
        void testCreatePolygonGeofence() throws Exception {
                String content = objectMapper.writeValueAsString(polygonGeofenceDto);

                mockMvc.perform(post("/api/geofences/polygon")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").exists())
                                .andExpect(jsonPath("$.data.id").isNotEmpty())
                                .andExpect(jsonPath("$.data.deviceId").value(TEST_DEVICE_ID))
                                .andExpect(jsonPath("$.data.name").value(polygonGeofenceDto.getName()))
                                .andExpect(jsonPath("$.data.vertices.length()").value(4))
                                .andExpect(jsonPath("$.data.active").value(true));
        }

        @Test
        @WithMockUser
        void testGetGeofencesForDevice() throws Exception {
                // First create some geofences
                geofenceService.createCircleGeofence(circleGeofenceDto);
                geofenceService.createPolygonGeofence(polygonGeofenceDto);

                // Test retrieval
                mockMvc.perform(get("/api/geofences/device/{deviceId}", TEST_DEVICE_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data[*].deviceId", everyItem(is(TEST_DEVICE_ID))));
        }

        @Test
        @WithMockUser
        void testGetGeofencesForDevicePaginated() throws Exception {
                // Create multiple geofences
                for (int i = 0; i < 5; i++) {
                        CircleGeofenceDto dto = CircleGeofenceDto.builder()
                                        .deviceId(TEST_DEVICE_ID)
                                        .name("Logistics Hub " + i)
                                        .description("Detailed zone for hub #" + i)
                                        .centerLatitude(40.712776 + (i * 0.001))
                                        .centerLongitude(-74.005974 + (i * 0.001))
                                        .radiusMeters(500.0)
                                        .category("logistics")
                                        .alertLevel(1)
                                        .color("#FF0000")
                                        .build();
                        geofenceService.createCircleGeofence(dto);
                }

                // Test pagination
                mockMvc.perform(get("/api/geofences/device/{deviceId}/paginated", TEST_DEVICE_ID)
                                .param("page", "0")
                                .param("size", "3")
                                .param("sort", "name")
                                .param("direction", "asc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.content").isArray())
                                .andExpect(jsonPath("$.data.content.length()").value(3))
                                .andExpect(jsonPath("$.data.totalElements").value(5))
                                .andExpect(jsonPath("$.data.totalPages").value(2))
                                .andExpect(jsonPath("$.data.first").value(true))
                                .andExpect(jsonPath("$.data.last").value(false));

                // Test second page
                mockMvc.perform(get("/api/geofences/device/{deviceId}/paginated", TEST_DEVICE_ID)
                                .param("page", "1")
                                .param("size", "3"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.content").isArray())
                                .andExpect(jsonPath("$.data.content.length()").value(2))
                                .andExpect(jsonPath("$.data.totalElements").value(5))
                                .andExpect(jsonPath("$.data.first").value(false))
                                .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @WithMockUser
        void testUpdateGeofence() throws Exception {
                // First create a geofence
                CircleGeofence createdGeofence = geofenceService.createCircleGeofence(circleGeofenceDto);

                // Update the geofence
                circleGeofenceDto.setName("Downtown Priority Hub");
                circleGeofenceDto.setDescription("Updated priority area for fast shipping");
                circleGeofenceDto.setRadiusMeters(1000.0);

                String updateContent = objectMapper.writeValueAsString(circleGeofenceDto);

                mockMvc.perform(put("/api/geofences/{id}", createdGeofence.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateContent))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value(createdGeofence.getId()))
                                .andExpect(jsonPath("$.data.name").value("Downtown Priority Hub"))
                                .andExpect(jsonPath("$.data.description")
                                                .value("Updated priority area for fast shipping"))
                                .andExpect(jsonPath("$.data.radiusMeters").value(1000.0));

                // Verify the update in the database
                Geofence updated = geofenceService.getGeofenceById(createdGeofence.getId());
                assertEquals("Downtown Priority Hub", updated.getName());
                assertEquals("Updated priority area for fast shipping", updated.getDescription());
        }

        @Test
        @WithMockUser
        void testDeleteGeofence() throws Exception {
                // First create a geofence
                CircleGeofence createdGeofence = geofenceService.createCircleGeofence(circleGeofenceDto);

                // Delete the geofence
                mockMvc.perform(delete("/api/geofences/{id}", createdGeofence.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                // Verify it's gone from the database or marked as inactive
                List<Geofence> geofences = geofenceService.getGeofencesForDevice(TEST_DEVICE_ID);
                assertTrue(geofences.stream().noneMatch(g -> g.getId().equals(createdGeofence.getId())));
        }

        @Test
        @WithMockUser
        void testGetGeofenceById() throws Exception {
                // First create a geofence
                CircleGeofence createdGeofence = geofenceService.createCircleGeofence(circleGeofenceDto);

                // Get the geofence by ID
                mockMvc.perform(get("/api/geofences/{id}", createdGeofence.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value(createdGeofence.getId()))
                                .andExpect(jsonPath("$.data.name").value(circleGeofenceDto.getName()));
        }

        @Test
        @WithMockUser
        void testGetGeofenceById_NotFound() throws Exception {
                // Test with a non-existent ID
                Long nonExistentId = 999L;

                mockMvc.perform(get("/api/geofences/{id}", nonExistentId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @WithMockUser
        void testSearchGeofences() throws Exception {
                // Create geofences with searchable names
                CircleGeofenceDto searchableDto1 = circleGeofenceDto.toBuilder()
                                .name("Manhattan Residential Gate")
                                .category("residential")
                                .build();
                CircleGeofenceDto searchableDto2 = circleGeofenceDto.toBuilder()
                                .name("Financial District Office")
                                .category("commercial")
                                .build();
                CircleGeofenceDto nonSearchableDto = circleGeofenceDto.toBuilder()
                                .name("Default Storage Facility")
                                .category("industrial")
                                .build();

                geofenceService.createCircleGeofence(searchableDto1);
                geofenceService.createCircleGeofence(searchableDto2);
                geofenceService.createCircleGeofence(nonSearchableDto);

                // Search for geofences
                mockMvc.perform(get("/api/geofences/device/{deviceId}/search", TEST_DEVICE_ID)
                                .param("query", "District"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(1))
                                .andExpect(jsonPath("$.data[*].name", everyItem(containsString("District"))));

                // Search by category
                mockMvc.perform(get("/api/geofences/device/{deviceId}/search", TEST_DEVICE_ID)
                                .param("category", "residential"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(1))
                                .andExpect(jsonPath("$.data[0].category").value("residential"));
        }
}