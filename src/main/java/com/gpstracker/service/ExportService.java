package com.gpstracker.service;

import com.gpstracker.model.GpsData;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExportService {

    @Autowired
    private GpsDataService gpsDataService;

    private static final String[] CSV_HEADER = {
        "Device ID", "Latitude", "Longitude", "Speed", "Heading", "Timestamp", "Additional Info"
    };

    public String exportToCsv(String deviceId, LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        List<GpsData> gpsDataList = gpsDataService.getGpsDataForDevice(deviceId, startTime, endTime);
        
        String fileName = String.format("gps_data_%s_%s.csv", 
            deviceId, 
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );

        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName))) {
            writer.writeNext(CSV_HEADER);

            for (GpsData data : gpsDataList) {
                String[] line = {
                    data.getDeviceId(),
                    String.valueOf(data.getLatitude()),
                    String.valueOf(data.getLongitude()),
                    String.valueOf(data.getSpeed()),
                    String.valueOf(data.getHeading()),
                    data.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    data.getAdditionalInfo()
                };
                writer.writeNext(line);
            }
        }

        log.info("Exported {} GPS data points to {}", gpsDataList.size(), fileName);
        return fileName;
    }
}
