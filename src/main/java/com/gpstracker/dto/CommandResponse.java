package com.gpstracker.dto;

import com.gpstracker.model.command.CommandStatus;
import com.gpstracker.model.command.CommandType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response sent back after a command is issued and processed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandResponse {
    private String commandId;
    private String deviceId;
    private CommandType commandType;
    private CommandStatus status;
    private String message;
    private Instant issuedAt;
    private Instant completedAt;
}