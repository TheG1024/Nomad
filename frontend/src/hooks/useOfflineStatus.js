import { useState, useEffect, useCallback } from 'react';
import offlineService from '../services/offlineService';

/**
 * Custom hook that provides offline status and functionality
 * 
 * Returns:
 * - isOnline: boolean indicating current online status
 * - queueSize: number of pending operations to sync
 * - syncNow: function to manually trigger synchronization
 * - clearQueue: function to clear all queued operations
 * - lastEvent: last network status event that occurred
 * 
 * Usage:
 * ```
 * const { isOnline, queueSize, syncNow, clearQueue } = useOfflineStatus();
 * ```
 * 
 * @returns {Object} Offline status and control functions
 */
const useOfflineStatus = () => {
  // Track online status, queue size, and last event
  const [networkStatus, setNetworkStatus] = useState(offlineService.getNetworkStatus());
  const [lastEvent, setLastEvent] = useState(null);
  
  // Update state when network status changes
  useEffect(() => {
    const handleNetworkChange = (event) => {
      setNetworkStatus(offlineService.getNetworkStatus());
      setLastEvent(event);
    };
    
    // Register listener
    offlineService.addListener(handleNetworkChange);
    
    // Clean up on unmount
    return () => {
      offlineService.removeListener(handleNetworkChange);
    };
  }, []);
  
  // Function to manually trigger sync
  const syncNow = useCallback(() => {
    if (networkStatus.isOnline && networkStatus.queueSize > 0) {
      offlineService._processQueue();
    } else if (!networkStatus.isOnline) {
      console.warn('Cannot sync while offline. Please connect to the internet and try again.');
    }
  }, [networkStatus.isOnline, networkStatus.queueSize]);
  
  // Function to clear the queue
  const clearQueue = useCallback(() => {
    if (networkStatus.queueSize > 0) {
      const confirmation = window.confirm(
        'Are you sure you want to clear all pending operations? This will discard your unsaved changes.'
      );
      
      if (confirmation) {
        offlineService.clearQueue();
      }
    }
  }, [networkStatus.queueSize]);
  
  // Check for connection quality
  const checkConnectionQuality = useCallback(() => {
    offlineService._checkConnectionQuality();
  }, []);
  
  return {
    isOnline: networkStatus.isOnline,
    queueSize: networkStatus.queueSize,
    syncNow,
    clearQueue,
    lastEvent,
    checkConnectionQuality
  };
};

export default useOfflineStatus; 