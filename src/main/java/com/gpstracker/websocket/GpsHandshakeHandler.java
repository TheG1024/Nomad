package com.gpstracker.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class GpsHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, 
                                    WebSocketHandler wsHandler, 
                                    Map<String, Object> attributes) {
        // Extract device ID from request headers or query parameters
        String deviceId = extractDeviceId(request);
        attributes.put("deviceId", deviceId);
        
        return () -> deviceId;
    }

    private String extractDeviceId(ServerHttpRequest request) {
        // Try to get device ID from query parameters
        String deviceId = request.getURI().getQuery();
        if (deviceId != null && deviceId.startsWith("deviceId=")) {
            return deviceId.substring("deviceId=".length());
        }
        
        // If not found in query, try headers
        String deviceHeader = request.getHeaders().getFirst("X-Device-ID");
        if (deviceHeader != null) {
            return deviceHeader;
        }
        
        throw new IllegalArgumentException("Device ID not provided");
    }
}
