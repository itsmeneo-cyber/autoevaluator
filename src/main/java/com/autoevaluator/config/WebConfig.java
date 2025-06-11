package com.autoevaluator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String origin = System.getenv("FRONTEND_URL");
        if (origin == null || origin.isBlank()) {
            origin = "http://localhost:3000"; // fallback for local
        }

        registry.addMapping("/**")
                .allowedOrigins(origin)
                .allowedMethods("*")
                .allowCredentials(true);
    }
}
