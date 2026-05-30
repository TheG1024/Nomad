package com.gpstracker.controller;

import com.gpstracker.model.GpsData;
import com.gpstracker.service.ExportService;
import com.gpstracker.service.GpsDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/gps")
@CrossOrigin(origins = "*")
public class GpsDataController {

    @Autowired(required = false)
    private ExportService exportService;

    @Autowired
    private GpsDataService gpsDataService;

    @GetMapping("/export")
    public ResponseEntity<Resource> exportGpsData(
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "csv") String format) {
        
        // Check if export service is available (not available in embedded mode)
        if (exportService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(null);
        }
        
        try {
            // Generate filename for the export
            String fileExtension = format.toLowerCase();
            String fileName = String.format("gps_data_%s_%s_%s.%s", 
                deviceId, 
                startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
                endTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
                fileExtension);
            
            // Export the data
            exportService.exportGpsData(deviceId, startTime, endTime, format);
            
            // Create a resource from the file
            Path path = Paths.get(fileName);
            Resource resource = new UrlResource(path.toUri());

            // Set appropriate content type based on format
            MediaType mediaType = format.equalsIgnoreCase("csv") ? 
                MediaType.parseMediaType("text/csv") : 
                MediaType.APPLICATION_JSON;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("Error exporting GPS data: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/data")
    public ResponseEntity<List<GpsData>> getGpsData(
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            List<GpsData> data = gpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error retrieving GPS data: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<GpsData>> getAlerts(
            @RequestParam String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            List<GpsData> alerts = gpsDataService.getAlerts(deviceId, startTime, endTime);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error retrieving alerts: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

 @GetMapping("/statistics")
 public ResponseEntity<Map<String, Object>> getStatistics(
 @RequestParam String deviceId,
 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
 
 try {
 LocalDateTime dateTime = date.atStartOfDay();
 Map<String, Object> stats = gpsDataService.getDeviceStatistics(deviceId, dateTime);
 return ResponseEntity.ok(stats);
 } catch (Exception e) {
 log.error("Error retrieving statistics: ", e);
 return ResponseEntity.internalServerError().build();
 }
 }

    @PostMapping("/geofence")
    public ResponseEntity<Void> setGeofence(
            @RequestParam String deviceId,
            @RequestParam double centerLat,
            @RequestParam double centerLon,
            @RequestParam double radius) {
        
        try {
            gpsDataService.setGeofence(deviceId, centerLat, centerLon, radius);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error setting geofence: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/data")
    public ResponseEntity<Void> saveGpsData(@RequestBody GpsData gpsData) {
        try {
            gpsDataService.saveGpsData(gpsData);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error saving GPS data: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("Unhandled exception: ", e);
        return ResponseEntity.internalServerError()
                .body("An unexpected error occurred. Please try again later.");
    }
}
