package com.music.scoring_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ScoreWebSocketHandler scoreWebSocketHandler;

    public WebSocketConfig(ScoreWebSocketHandler scoreWebSocketHandler) {
        this.scoreWebSocketHandler = scoreWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 大屏幕网页将通过 ws://localhost:8080/ws/screen 连接到这里
        // setAllowedOrigins("*") 允许跨域，方便本地测试
        registry.addHandler(scoreWebSocketHandler, "/ws/screen").setAllowedOrigins("*");
    }
}