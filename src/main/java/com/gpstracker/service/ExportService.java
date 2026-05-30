package com.gpstracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gpstracker.model.GpsData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting GPS data
 * Only active when the embedded profile is not active
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "!embedded")
public class ExportService {

    private static final String EXPORT_DIR = "exports";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String CSV_HEADER = "Device ID,Timestamp,Latitude,Longitude,Speed,Heading,Battery Level,Status,Accuracy,Network Type,Signal Strength\n";

    private final GpsDataService gpsDataService;
    private final ObjectMapper objectMapper;

    public ExportService(GpsDataService gpsDataService) {
        this.gpsDataService = gpsDataService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    /**
     * Export GPS data for a device in a specified time range
     */
    public void exportGpsData(String deviceId, LocalDateTime startTime, LocalDateTime endTime, String format) {
        log.info("Exporting GPS data for device {} from {} to {} in {} format",
                deviceId, startTime, endTime, format);

        List<GpsData> gpsData = gpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);

        switch (format.toLowerCase()) {
            case "csv":
                exportToCsv(gpsData, generateFileName(deviceId, startTime, endTime, "csv"));
                break;
            case "json":
                exportToJson(gpsData, generateFileName(deviceId, startTime, endTime, "json"));
                break;
            default:
                log.error("Unsupported export format: {}", format);
                throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }

    /**
     * Export GPS data to CSV format
     */
    private void exportToCsv(List<GpsData> gpsData, String fileName) {
        log.info("Exporting {} GPS data points to CSV file: {}", gpsData.size(), fileName);
        try {
            ensureDirectoryExists();
            writeDataToCsv(EXPORT_DIR + File.separator + fileName, gpsData);
        } catch (IOException e) {
            log.error("Failed to export to CSV: {}", e.getMessage());
            throw new RuntimeException("CSV export failed", e);
        }
    }

    /**
     * Export GPS data to JSON format
     */
    private void exportToJson(List<GpsData> gpsData, String fileName) {
        log.info("Exporting {} GPS data points to JSON file: {}", gpsData.size(), fileName);
        try {
            ensureDirectoryExists();
            objectMapper.writeValue(new File(EXPORT_DIR + File.separator + fileName), gpsData);
        } catch (IOException e) {
            log.error("Failed to export to JSON: {}", e.getMessage());
            throw new RuntimeException("JSON export failed", e);
        }
    }

    private void ensureDirectoryExists() throws IOException {
        Path path = Paths.get(EXPORT_DIR);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Generate a filename for the export
     */
    private String generateFileName(String deviceId, LocalDateTime startTime, LocalDateTime endTime, String extension) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return String.format("gps_data_%s_%s_%s.%s",
                deviceId,
                startTime.format(formatter),
                endTime.format(formatter),
                extension);
    }

    @Scheduled(cron = "0 0 0 * * 0") // Run at midnight every Sunday
    public void weeklyExport() {
        log.info("Starting weekly GPS data export");

        try {
            // Create weekly export directory
            String weeklyDir = String.format("%s/weekly_%s",
                    EXPORT_DIR,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            Files.createDirectories(Paths.get(weeklyDir));

            // Calculate time range for the past week
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusWeeks(1);

            // Export data for each known device
            exportDataForTimeRange(weeklyDir, startTime, endTime);

            log.info("Weekly export completed successfully");
        } catch (Exception e) {
            log.error("Error during weekly export: ", e);
        }
    }

    private void exportDataForTimeRange(String directory, LocalDateTime startTime, LocalDateTime endTime)
            throws IOException {
        // In a real application, you would maintain a list of active devices
        // For this example, we'll export a sample device
        String[] sampleDevices = { "device123", "device456" };

        for (String deviceId : sampleDevices) {
            String filename = String.format("%s/%s_%s.csv",
                    directory,
                    deviceId,
                    startTime.format(FILE_DATE_FORMAT));

            List<GpsData> data = gpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);
            writeDataToCsv(filename, data);
            log.info("Exported data for device: {}", deviceId);
        }
    }

    private void writeDataToCsv(String filename, List<GpsData> data) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write CSV header
            writer.write(CSV_HEADER);

            // Write data rows
            for (GpsData gpsData : data) {
                writer.write(String.format("%s,%s,%f,%f,%f,%f,%f,%s,%f,%s,%d\n",
                        gpsData.getDeviceId(),
                        gpsData.getTimestamp(),
                        gpsData.getLatitude(),
                        gpsData.getLongitude(),
                        gpsData.getSpeed(),
                        gpsData.getHeading(),
                        gpsData.getBatteryLevel(),
                        gpsData.getDeviceStatus(),
                        gpsData.getAccuracy(),
                        gpsData.getNetworkType(),
                        gpsData.getSignalStrength()));
            }
        }
    }

}
