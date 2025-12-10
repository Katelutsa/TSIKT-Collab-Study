package com.example.server.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketEventController webSocketEventController;

    public WebSocketConfig(WebSocketEventController webSocketEventController) {
        this.webSocketEventController = webSocketEventController;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketEventController, "/ws/updates")
                .setAllowedOriginPatterns("*"); // можна й setAllowedOrigins("*")
    }
}


