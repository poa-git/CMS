package com.system.complaints.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enables WebSocket broadcasts to all clients
        config.enableSimpleBroker("/topic");

        // Prefix for client → server messages
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow CORS from any frontend
                .withSockJS();                 // SockJS fallback

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*"); // Native WebSocket
    }
}
