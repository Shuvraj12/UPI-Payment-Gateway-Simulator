package com.upisimulator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Maps {@code /uploads/**} to {@code app.upload.dir} on disk. Deliberately
 * public (see SecurityConfig) rather than access-controlled - profile
 * pictures need to be viewable in other users' UIs later (e.g. "Send to
 * Priya" showing Priya's photo in Phase 7), the same way most consumer apps
 * serve avatar images without requiring the viewer's own auth token.
 * Filenames are random UUIDs, not sequential, so this isn't an enumerable
 * listing of every user's photo.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

}
