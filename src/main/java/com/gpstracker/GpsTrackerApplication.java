package com.gpstracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GpsTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GpsTrackerApplication.class, args);
    }
}
