package com.greenforce.config;

import com.greenforce.service.FileStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageService storage;

    public WebConfig(FileStorageService storage) {
        this.storage = storage;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // open http://localhost:8080/ and land on the existing home page
        registry.addRedirectViewController("/", "/home.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // serves uploaded issue photos at /uploads/<file>
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(storage.getRoot().toUri().toString());
    }
}
