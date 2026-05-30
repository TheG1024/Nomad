package com.gpstracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

 @Override
 public void addResourceHandlers(ResourceHandlerRegistry registry) {
 // Make cyber-tracker.jsx accessible as /js/cyber-tracker.js (Babel transpiles it client-side)
 registry.addResourceHandler("/js/cyber-tracker.js")
 .addResourceLocations("classpath:/templates/cyber-tracker.jsx");
 
 // Make cyber-tracker.jsx accessible from the root
 registry.addResourceHandler("/cyber-tracker.jsx")
 .addResourceLocations("classpath:/templates/cyber-tracker.jsx");
 
 // Add default static resources (css, js, images)
 registry.addResourceHandler("/**")
 .addResourceLocations("classpath:/static/");
 
 registry.addResourceHandler("/js/**")
 .addResourceLocations("classpath:/static/js/");
 
 registry.addResourceHandler("/css/**")
 .addResourceLocations("classpath:/static/css/");
 
 registry.addResourceHandler("/images/**")
 .addResourceLocations("classpath:/static/images/");
 
 // Vendor resources (React, Leaflet, Font Awesome, Babel, etc.)
 registry.addResourceHandler("/vendor/**")
 .addResourceLocations("classpath:/static/vendor/");
 }
} 