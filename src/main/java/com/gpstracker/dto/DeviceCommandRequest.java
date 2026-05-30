package com.gpstracker.dto;

import com.gpstracker.model.command.CommandType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request payload for sending a command to a device.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCommandRequest {

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotNull(message = "Command type is required")
    private CommandType commandType;

    /** Optional parameters (e.g. speed threshold for SET_SPEED_THRESHOLD). */
    private Map<String, Object> parameters;

    /** Optional issuer identifier. Defaults to "system". */
    private String issuedBy;
}