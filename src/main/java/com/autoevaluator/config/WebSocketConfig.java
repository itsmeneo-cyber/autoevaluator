package com.autoevaluator.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String FRONTEND_URL;

    static {
        // Try system env (for Render)
        String envUrl = System.getenv("FRONTEND_URL");

        // Fallback to .env (for local dev)
        if (envUrl == null || envUrl.isBlank()) {
            envUrl = Dotenv.configure().ignoreIfMissing().load()
                    .get("FRONTEND_URL", "http://localhost:3000");
        }

        FRONTEND_URL = envUrl;
        System.out.println("[WebSocket CORS] Allowing origin: " + FRONTEND_URL);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(FRONTEND_URL)
                .withSockJS(); // enables fallback options for browsers that don’t support WebSocket
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // server to client
        config.setApplicationDestinationPrefixes("/app"); // client to server
    }
}
