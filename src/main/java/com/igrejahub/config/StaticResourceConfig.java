package com.igrejahub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve /api/files/** a partir de /app/uploads/ no container
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:/app/uploads/")
                .setCachePeriod(3600);
    }
}
