package com.gpstracker.model.command;

/**
 * Enumeration of all available device command types.
 */
public enum CommandType {
    LOCK("Lock Device", "Sends a lock signal to the device, disabling engine start."),
    UNLOCK("Unlock Device", "Sends an unlock signal to the device, re-enabling engine start."),
    REBOOT("Reboot Device", "Restarts the device firmware and reconnects to the network."),
    PING("Request Location Ping", "Forces the device to send an immediate location update."),
    SHUTDOWN("Shutdown Device", "Powers down the device gracefully."),
    SET_SPEED_THRESHOLD("Set Speed Alert", "Configures the maximum speed threshold before an alert is triggered.");

    private final String displayName;
    private final String description;

    CommandType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}