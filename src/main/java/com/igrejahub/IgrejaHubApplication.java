package com.igrejahub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.igrejahub")
@EnableCaching
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class IgrejaHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(IgrejaHubApplication.class, args);
    }
}
