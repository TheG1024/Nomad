package com.gpstracker.config;

import com.gpstracker.websocket.GpsHandshakeHandler;
import com.gpstracker.websocket.GpsWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RawWebSocketConfig implements WebSocketConfigurer {

    private final GpsWebSocketHandler gpsWebSocketHandler;
    private final GpsHandshakeHandler handshakeHandler;

    public RawWebSocketConfig(GpsWebSocketHandler gpsWebSocketHandler, GpsHandshakeHandler handshakeHandler) {
        this.gpsWebSocketHandler = gpsWebSocketHandler;
        this.handshakeHandler = handshakeHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gpsWebSocketHandler, "/gps")
            .setAllowedOrigins("*")
            .setHandshakeHandler(handshakeHandler)
            .withSockJS()
            .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
    }
}
