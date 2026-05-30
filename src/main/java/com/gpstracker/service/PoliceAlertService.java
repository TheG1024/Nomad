package com.gpstracker.service;

import com.gpstracker.model.PoliceAlert;
import com.gpstracker.dto.PoliceAlertDTO;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing police alerts and government restrictions.
 */
public interface PoliceAlertService {
    PoliceAlert createAlert(PoliceAlertDTO dto);
    PoliceAlert updateAlert(String id, PoliceAlertDTO dto);
    void deleteAlert(String id);
    PoliceAlert getAlert(String id);
    List<PoliceAlert> getAllAlerts();
    List<PoliceAlert> getActiveAlerts();
    List<PoliceAlert> getAlertsInRadius(double latitude, double longitude, double radiusMeters);
    PoliceAlert toggleAlert(String id);
    void reportAlert(String id);
}