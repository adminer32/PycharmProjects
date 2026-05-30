package com.saaes.system.configuration;

import com.saaes.system.handler.AIWebSocketHandlerImpl;  // 导入实现类
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AIWebSocketHandlerImpl aiWebSocketHandler;

    @Autowired
    public WebSocketConfig(AIWebSocketHandlerImpl aiWebSocketHandler) {
        this.aiWebSocketHandler = aiWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 处理器
        registry.addHandler(aiWebSocketHandler, "/ws/ai").setAllowedOrigins("*");
    }
}
