package com.gpstracker.service;

import com.gpstracker.dto.CommandResponse;
import com.gpstracker.dto.DeviceCommandRequest;
import com.gpstracker.model.command.CommandStatus;
import com.gpstracker.model.command.CommandType;
import com.gpstracker.model.command.DeviceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service for issuing device commands and tracking their execution.
 * In embedded mode, commands are simulated with realistic delays.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceCommandService {

    private final SimpMessagingTemplate messagingTemplate;

    /** In-memory command history per device. */
    private final Map<String, List<DeviceCommand>> commandHistory = new ConcurrentHashMap<>();

    /**
     * Issue a command to a device.
     * The command is executed asynchronously and results are broadcast via WebSocket.
     */
    public DeviceCommand issueCommand(DeviceCommandRequest request) {
        String commandId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        DeviceCommand command = DeviceCommand.builder()
                .commandId(commandId)
                .deviceId(request.getDeviceId())
                .type(request.getCommandType())
                .status(CommandStatus.PENDING)
                .issuedBy(request.getIssuedBy() != null ? request.getIssuedBy() : "operator")
                .issuedAt(Instant.now())
                .parameters(request.getParameters())
                .build();

        // Store in history (keep last 50 per device)
        commandHistory.computeIfAbsent(request.getDeviceId(), k -> new CopyOnWriteArrayList<>())
                .add(0, command);
        // Trim to 50 entries
        List<DeviceCommand> history = commandHistory.get(request.getDeviceId());
        if (history.size() > 50) {
            history.subList(50, history.size()).clear();
        }

        log.info("Issued command {} of type {} to device {}",
                commandId, request.getCommandType(), request.getDeviceId());

        // Broadcast initial PENDING state
        broadcastResponse(command);

        // Execute command asynchronously (simulated)
        executeCommandAsync(command);

        return command;
    }

    @Async
    protected void executeCommandAsync(DeviceCommand command) {
        try {
            // Simulate realistic execution delay (500ms – 2s depending on command type)
            long delayMs = getSimulatedDelay(command.getType());
            Thread.sleep(delayMs);

            // Determine result based on command type
            String resultMessage = simulateExecution(command);

            command.setStatus(CommandStatus.SUCCESS);
            command.setResultMessage(resultMessage);
            command.setCompletedAt(Instant.now());

            log.info("Command {} completed successfully for device {}: {}",
                    command.getCommandId(), command.getDeviceId(), resultMessage);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            command.setStatus(CommandStatus.FAILED);
            command.setResultMessage("Command interrupted");
            command.setCompletedAt(Instant.now());
        } catch (Exception e) {
            command.setStatus(CommandStatus.FAILED);
            command.setResultMessage("Execution error: " + e.getMessage());
            command.setCompletedAt(Instant.now());
            log.error("Command {} failed for device {}: {}",
                    command.getCommandId(), command.getDeviceId(), e.getMessage());
        }

        broadcastResponse(command);
    }

    private long getSimulatedDelay(CommandType type) {
        return switch (type) {
            case PING -> 500L;
            case LOCK, UNLOCK -> 800L;
            case SET_SPEED_THRESHOLD -> 600L;
            case REBOOT -> 1500L;
            case SHUTDOWN -> 1000L;
        };
    }

    private String simulateExecution(DeviceCommand command) {
        return switch (command.getType()) {
            case LOCK -> "Engine disabled. Device " + command.getDeviceId() + " is now locked.";
            case UNLOCK -> "Engine enabled. Device " + command.getDeviceId() + " is now unlocked.";
            case REBOOT -> "Device " + command.getDeviceId() + " rebooted successfully. Reconnecting to network...";
            case PING -> {
                double lat = 40.7128 + (Math.random() - 0.5) * 0.01;
                double lng = -74.006 + (Math.random() - 0.5) * 0.01;
                yield String.format("Location ping received from %s at (%.6f, %.6f)",
                        command.getDeviceId(), lat, lng);
            }
            case SET_SPEED_THRESHOLD -> {
                Object threshold = command.getParameters() != null
                        ? command.getParameters().get("speedThreshold") : 120;
                yield String.format("Speed alert threshold set to %s km/h for device %s",
                        threshold, command.getDeviceId());
            }
            case SHUTDOWN -> "Device " + command.getDeviceId() + " shut down gracefully.";
        };
    }

    private void broadcastResponse(DeviceCommand command) {
        CommandResponse response = CommandResponse.builder()
                .commandId(command.getCommandId())
                .deviceId(command.getDeviceId())
                .commandType(command.getType())
                .status(command.getStatus())
                .message(command.getResultMessage())
                .issuedAt(command.getIssuedAt())
                .completedAt(command.getCompletedAt())
                .build();

        // Broadcast to device-specific topic
        messagingTemplate.convertAndSend(
                "/topic/commands/" + command.getDeviceId(), response);
    }

    /**
     * Get command history for a specific device.
     */
    public List<DeviceCommand> getCommandHistory(String deviceId, int limit) {
        List<DeviceCommand> history = commandHistory.getOrDefault(deviceId, List.of());
        return history.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Get all devices that have command history.
     */
    public List<String> getDevicesWithCommands() {
        return List.copyOf(commandHistory.keySet());
    }
}