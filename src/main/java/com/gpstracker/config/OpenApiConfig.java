package com.gpstracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;

/**
 * OpenAPI (Swagger) Configuration
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springdoc-openapi-ui/");
    }
    
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }
    
    @Bean
    public GroupedOpenApi geofenceApi() {
        return GroupedOpenApi.builder()
                .group("geofence")
                .pathsToMatch("/api/geofence/**")
                .build();
    }
    
    @Bean
    public OpenAPI appInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nomad GPS Tracker API")
                        .description("Modern GPS tracking platform with AI and social features")
                        .version("1.0.0"));
    }
} 