import React, { createContext, useContext, useState, useEffect } from 'react';
import offlineService from '../services/offlineService';

// Create context
const OfflineContext = createContext({
  isOnline: true,
  queueSize: 0,
  syncNow: () => {},
  clearQueue: () => {},
  lastEvent: null,
});

/**
 * Provider component for offline status context
 * Makes offline status and operations available throughout the app
 * 
 * @param {Object} props - Component props
 * @param {React.ReactNode} props.children - Child components
 */
export const OfflineProvider = ({ children }) => {
  // Initialize state from offline service
  const [networkStatus, setNetworkStatus] = useState(offlineService.getNetworkStatus());
  const [lastEvent, setLastEvent] = useState(null);
  
  // Listen for offline events
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
  const syncNow = () => {
    if (networkStatus.isOnline && networkStatus.queueSize > 0) {
      offlineService._processQueue();
    }
  };
  
  // Function to clear the queue
  const clearQueue = () => {
    if (networkStatus.queueSize > 0) {
      const confirmation = window.confirm(
        'Are you sure you want to clear all pending operations? This will discard your unsaved changes.'
      );
      
      if (confirmation) {
        offlineService.clearQueue();
      }
    }
  };
  
  // Context value
  const value = {
    isOnline: networkStatus.isOnline,
    queueSize: networkStatus.queueSize,
    syncNow,
    clearQueue,
    lastEvent,
  };
  
  return (
    <OfflineContext.Provider value={value}>
      {children}
    </OfflineContext.Provider>
  );
};

/**
 * Custom hook to use the offline context
 * 
 * Usage:
 * ```
 * const { isOnline, queueSize, syncNow } = useOfflineContext();
 * ```
 * 
 * @returns {Object} The offline context value
 * @throws {Error} If used outside of OfflineProvider
 */
export const useOfflineContext = () => {
  const context = useContext(OfflineContext);
  
  if (context === undefined) {
    throw new Error('useOfflineContext must be used within an OfflineProvider');
  }
  
  return context;
};

export default OfflineContext; 