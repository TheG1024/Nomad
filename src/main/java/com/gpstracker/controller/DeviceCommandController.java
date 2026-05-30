package com.gpstracker.controller;

import com.gpstracker.dto.ApiResponse;
import com.gpstracker.dto.CommandResponse;
import com.gpstracker.dto.DeviceCommandRequest;
import com.gpstracker.model.command.DeviceCommand;
import com.gpstracker.service.DeviceCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for issuing commands to devices.
 * Commands are executed asynchronously; results are delivered via WebSocket.
 */
@RestController
@RequestMapping("/api/commands")
@RequiredArgsConstructor
@Slf4j
public class DeviceCommandController {

    private final DeviceCommandService commandService;

    /**
     * Issue a new command to a device.
     * Subscribe to /topic/commands/{deviceId} to receive the async result.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CommandResponse>> sendCommand(
            @Valid @RequestBody DeviceCommandRequest request) {

        log.info("Received command {} for device {}",
                request.getCommandType(), request.getDeviceId());

        DeviceCommand command = commandService.issueCommand(request);

        CommandResponse response = CommandResponse.builder()
                .commandId(command.getCommandId())
                .deviceId(command.getDeviceId())
                .commandType(command.getType())
                .status(command.getStatus())
                .message("Command received and queued for execution. Result will be sent via WebSocket.")
                .issuedAt(command.getIssuedAt())
                .build();

        return ResponseEntity.accepted()
                .body(ApiResponse.success(response));
    }

    /**
     * Get command history for a device.
     */
    @GetMapping("/history/{deviceId}")
    public ResponseEntity<ApiResponse<List<?>>> getHistory(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "20") int limit) {

        List<DeviceCommand> history = commandService.getCommandHistory(deviceId, limit);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Get the list of devices that have command history.
     */
    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<String>>> getCommandDevices() {
        return ResponseEntity.ok(ApiResponse.success(commandService.getDevicesWithCommands()));
    }

    /**
     * Quick command aliases for common operations.
     */
    @PostMapping("/lock/{deviceId}")
    public ResponseEntity<ApiResponse<CommandResponse>> lockDevice(@PathVariable String deviceId) {
        return sendCommand(new DeviceCommandRequest(deviceId,
                com.gpstracker.model.command.CommandType.LOCK, null, "operator"));
    }

    @PostMapping("/unlock/{deviceId}")
    public ResponseEntity<ApiResponse<CommandResponse>> unlockDevice(@PathVariable String deviceId) {
        return sendCommand(new DeviceCommandRequest(deviceId,
                com.gpstracker.model.command.CommandType.UNLOCK, null, "operator"));
    }

    @PostMapping("/ping/{deviceId}")
    public ResponseEntity<ApiResponse<CommandResponse>> pingDevice(@PathVariable String deviceId) {
        return sendCommand(new DeviceCommandRequest(deviceId,
                com.gpstracker.model.command.CommandType.PING, null, "operator"));
    }

    @PostMapping("/reboot/{deviceId}")
    public ResponseEntity<ApiResponse<CommandResponse>> rebootDevice(@PathVariable String deviceId) {
        return sendCommand(new DeviceCommandRequest(deviceId,
                com.gpstracker.model.command.CommandType.REBOOT, null, "operator"));
    }

    @PostMapping("/shutdown/{deviceId}")
    public ResponseEntity<ApiResponse<CommandResponse>> shutdownDevice(@PathVariable String deviceId) {
        return sendCommand(new DeviceCommandRequest(deviceId,
                com.gpstracker.model.command.CommandType.SHUTDOWN, null, "operator"));
    }

    @PostMapping("/speed-threshold/{deviceId}")
    public ResponseEntity<ApiResponse<CommandResponse>> setSpeedThreshold(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {
        int threshold = (int) body.getOrDefault("speedThreshold", 120);
        return sendCommand(new DeviceCommandRequest(deviceId,
                com.gpstracker.model.command.CommandType.SET_SPEED_THRESHOLD,
                Map.of("speedThreshold", threshold), "operator"));
    }
}