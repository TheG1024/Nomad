package com.gpstracker.config;

import com.gpstracker.websocket.GpsHandshakeHandler;
import com.gpstracker.websocket.GpsWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketHandlerConfig implements WebSocketConfigurer {

    private final GpsWebSocketHandler gpsWebSocketHandler;
    private final GpsHandshakeHandler gpsHandshakeHandler;

    public WebSocketHandlerConfig(GpsWebSocketHandler gpsWebSocketHandler, GpsHandshakeHandler gpsHandshakeHandler) {
        this.gpsWebSocketHandler = gpsWebSocketHandler;
        this.gpsHandshakeHandler = gpsHandshakeHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gpsWebSocketHandler, "/gps")
                .setAllowedOrigins("*")
                .setHandshakeHandler(gpsHandshakeHandler)
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
    }
}
