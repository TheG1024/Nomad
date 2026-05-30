package com.gpstracker.model.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a command sent to a device.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCommand {
    private String commandId;
    private String deviceId;
    private CommandType type;
    private CommandStatus status;
    private String issuedBy;
    private Instant issuedAt;
    private Instant completedAt;
    private String resultMessage;
    private Map<String, Object> parameters; // e.g. { "speedThreshold": 120 }
}