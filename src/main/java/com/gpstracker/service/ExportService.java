package com.gpstracker.service;

import com.gpstracker.model.GpsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExportService {

    private static final String EXPORT_DIR = "exports";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String CSV_HEADER = "Device ID,Timestamp,Latitude,Longitude,Speed,Heading,Battery Level,Status,Accuracy,Network Type,Signal Strength\n";

    @Autowired
    private GpsDataService gpsDataService;

    public String exportToCsv(String deviceId, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        // Create exports directory if it doesn't exist
        Path exportPath = Paths.get(EXPORT_DIR);
        if (!Files.exists(exportPath)) {
            Files.createDirectories(exportPath);
        }

        // Generate filename with timestamp
        String filename = String.format("%s/%s_%s.csv", 
            EXPORT_DIR,
            deviceId,
            LocalDateTime.now().format(FILE_DATE_FORMAT)
        );

        // Get data and write to CSV
        List<GpsData> data = gpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);
        writeDataToCsv(filename, data);

        return filename;
    }

    @Scheduled(cron = "0 0 0 * * 0") // Run at midnight every Sunday
    public void weeklyExport() {
        log.info("Starting weekly GPS data export");
        
        try {
            // Create weekly export directory
            String weeklyDir = String.format("%s/weekly_%s", 
                EXPORT_DIR,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            );
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

    private void exportDataForTimeRange(String directory, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        // In a real application, you would maintain a list of active devices
        // For this example, we'll export a sample device
        String[] sampleDevices = {"device123", "device456"};
        
        for (String deviceId : sampleDevices) {
            String filename = String.format("%s/%s_%s.csv",
                directory,
                deviceId,
                startTime.format(FILE_DATE_FORMAT)
            );

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
                    gpsData.getSignalStrength()
                ));
            }
        }
    }
}
