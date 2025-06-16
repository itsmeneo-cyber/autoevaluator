package com.autoevaluator.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String FRONTEND_URL;

    static {
        // Try environment variable (Render/production)
        String envUrl = System.getenv("FRONTEND_URL");

        // Fallback to .env (local)
        if (envUrl == null || envUrl.isBlank()) {
            envUrl = Dotenv.configure().ignoreIfMissing().load()
                    .get("FRONTEND_URL", "http://localhost:3000");
        }

        FRONTEND_URL = envUrl;
        System.out.println("[CORS] Allowing origin: " + FRONTEND_URL);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(FRONTEND_URL)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
