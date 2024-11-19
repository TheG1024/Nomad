package com.gpstracker.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Component
public class GpsHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        // Generate a unique session ID
        String sessionId = UUID.randomUUID().toString();
        attributes.put("sessionId", sessionId);
        
        return () -> sessionId;
    }
}
