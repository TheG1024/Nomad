package com.gpstracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import com.gpstracker.websocket.GpsWebSocketHandler;
import com.gpstracker.websocket.GpsHandshakeHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private GpsWebSocketHandler gpsWebSocketHandler;

    @Autowired
    private GpsHandshakeHandler gpsHandshakeHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gpsWebSocketHandler, "/gps")
               .setHandshakeHandler(gpsHandshakeHandler)
               .setAllowedOrigins("*");
    }
}
