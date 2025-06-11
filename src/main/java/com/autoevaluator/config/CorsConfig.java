package com.autoevaluator.config;


public class CorsConfig {
    public static final String FRONTEND_ORIGIN;

    static {
        String envOrigin = System.getenv("FRONTEND_URL");
        FRONTEND_ORIGIN = (envOrigin != null && !envOrigin.isBlank())
                ? envOrigin
                : "http://localhost:3000"; // Fallback for local dev
    }
}
