package com.gpstracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Nomad GPS Tracking System
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class NomadGpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NomadGpsApplication.class, args);
    }
} 