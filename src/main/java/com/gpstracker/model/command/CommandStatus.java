package com.gpstracker.model.command;

/**
 * Status of a device command through its lifecycle.
 */
public enum CommandStatus {
    PENDING,    // Command received, not yet processed
    SENT,       // Command dispatched to device
    ACKNLEDGED, // Device acknowledged receipt
    EXECUTING,  // Device is executing the command
    SUCCESS,    // Command completed successfully
    FAILED,     // Command failed
    TIMEOUT     // Device did not respond in time
}