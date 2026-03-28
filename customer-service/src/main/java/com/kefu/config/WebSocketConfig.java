package com.kefu.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private VisitorWebSocketHandler visitorWebSocketHandler;

    @Autowired
    private AgentWebSocketHandler agentWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(visitorWebSocketHandler, "/ws/visitor")
                .addInterceptors(new WebSocketHandshakeInterceptor())
                .setAllowedOrigins("*");
        registry.addHandler(agentWebSocketHandler, "/ws/agent")
                .addInterceptors(new WebSocketHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
