import React, { useState, useEffect } from 'react';
import offlineService from '../services/offlineService';

/**
 * Higher Order Component that adds offline status awareness and capabilities
 * to the wrapped component.
 *
 * Adds the following props to the wrapped component:
 * - isOnline: boolean indicating current online status
 * - queueSize: number of operations queued for sync
 * - syncNow: function to manually trigger synchronization
 * - clearQueue: function to clear all queued operations
 * 
 * Usage:
 * ```
 * const MyComponentWithOffline = withOfflineSupport(MyComponent);
 * ```
 * 
 * @param {React.ComponentType} WrappedComponent - The component to enhance
 * @returns {React.FC} Enhanced component with offline capabilities
 */
const withOfflineSupport = (WrappedComponent) => {
  const WithOfflineSupport = (props) => {
    // Track online status and queue size
    const [networkStatus, setNetworkStatus] = useState(offlineService.getNetworkStatus());
    
    useEffect(() => {
      // Handler for network status changes
      const handleNetworkChange = () => {
        setNetworkStatus(offlineService.getNetworkStatus());
      };
      
      // Register listener
      offlineService.addListener(handleNetworkChange);
      
      // Initial check
      handleNetworkChange();
      
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
        if (window.confirm('Are you sure you want to clear all pending operations? This will discard your unsaved changes.')) {
          offlineService.clearQueue();
        }
      }
    };
    
    // Additional props to pass down
    const offlineProps = {
      isOnline: networkStatus.isOnline,
      queueSize: networkStatus.queueSize,
      syncNow,
      clearQueue
    };
    
    // Render wrapped component with additional props
    return <WrappedComponent {...props} {...offlineProps} />;
  };
  
  // Set display name for debugging
  const wrappedComponentName = WrappedComponent.displayName 
    || WrappedComponent.name 
    || 'Component';
    
  WithOfflineSupport.displayName = `withOfflineSupport(${wrappedComponentName})`;
  
  return WithOfflineSupport;
};

export default withOfflineSupport; 