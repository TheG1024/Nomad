package com.gpstracker.controller;

import com.gpstracker.model.DeviceAlert;
import com.gpstracker.model.GpsData;
import com.gpstracker.service.DeviceSimulationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class SubscriptionController {
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceSimulationService simulationService;

    public SubscriptionController(SimpMessagingTemplate messagingTemplate, DeviceSimulationService simulationService) {
        this.messagingTemplate = messagingTemplate;
        this.simulationService = simulationService;
    }

    @MessageMapping("/subscribe")
    public void subscribe(@Payload String deviceId) {
        GpsData data = simulationService.buildSampleData(deviceId);
        DeviceAlert alert = simulationService.buildSampleAlert(deviceId);

        messagingTemplate.convertAndSend("/topic/device/" + deviceId, data);
        messagingTemplate.convertAndSend("/topic/alerts/" + deviceId, alert);
    }
}
