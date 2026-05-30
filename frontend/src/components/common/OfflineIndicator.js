import React, { useState, useEffect } from 'react';
import offlineService from '../../services/offlineService';
import './OfflineIndicator.css';

/**
 * OfflineIndicator component
 * Displays a status bar indicating online/offline state and pending operations
 */
const OfflineIndicator = () => {
  const [networkStatus, setNetworkStatus] = useState(offlineService.getNetworkStatus());
  const [visible, setVisible] = useState(!networkStatus.isOnline || networkStatus.queueSize > 0);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    // Handler for network status changes
    const handleNetworkChange = (event) => {
      const currentStatus = offlineService.getNetworkStatus();
      setNetworkStatus(currentStatus);
      
      // Show indicator when offline or when there are pending operations
      setVisible(!currentStatus.isOnline || currentStatus.queueSize > 0);
      
      // Auto-expand on important events
      if (event.type === 'OFFLINE' || 
          (event.type === 'OPERATION_QUEUED' && !expanded)) {
        setExpanded(true);
      }
      
      // Auto-collapse when all operations are processed and we're online
      if (currentStatus.isOnline && 
          currentStatus.queueSize === 0 && 
          (event.type === 'OPERATION_PROCESSED' || event.type === 'QUEUE_CLEARED')) {
        // Use timeout to show success state briefly before hiding
        setTimeout(() => {
          setExpanded(false);
          // Hide after animation completes
          setTimeout(() => {
            if (offlineService.getNetworkStatus().queueSize === 0) {
              setVisible(false);
            }
          }, 500);
        }, 1500);
      }
    };

    // Register listener
    offlineService.addListener(handleNetworkChange);
    
    // Clean up listener on unmount
    return () => {
      offlineService.removeListener(handleNetworkChange);
    };
  }, [expanded]);

  // Nothing to show
  if (!visible) {
    return null;
  }

  // Determine indicator classes based on status
  const getStatusClasses = () => {
    const { isOnline, queueSize } = networkStatus;
    
    if (!isOnline) {
      return 'offline-indicator--offline';
    }
    
    if (queueSize > 0) {
      return 'offline-indicator--syncing';
    }
    
    return 'offline-indicator--online';
  };

  // Determine status text based on network status
  const getStatusText = () => {
    const { isOnline, queueSize } = networkStatus;
    
    if (!isOnline) {
      return 'You are offline';
    }
    
    if (queueSize > 0) {
      return `Syncing ${queueSize} ${queueSize === 1 ? 'change' : 'changes'}...`;
    }
    
    return 'All changes synced';
  };

  // Handle clear queue button click
  const handleClearQueue = () => {
    if (window.confirm('Are you sure you want to clear all pending operations? This will discard your unsaved changes.')) {
      offlineService.clearQueue();
    }
  };

  // Handle expand/collapse toggle
  const toggleExpanded = () => {
    setExpanded(!expanded);
  };

  return (
    <div className={`offline-indicator ${getStatusClasses()} ${expanded ? 'expanded' : 'collapsed'}`}>
      <div className="offline-indicator__main" onClick={toggleExpanded}>
        <div className="offline-indicator__status-icon"></div>
        <div className="offline-indicator__text">
          {getStatusText()}
        </div>
        <button 
          className="offline-indicator__toggle-btn"
          aria-label={expanded ? 'Collapse details' : 'Expand details'}
        >
          {expanded ? '▲' : '▼'}
        </button>
      </div>

      {expanded && (
        <div className="offline-indicator__details">
          <div className="offline-indicator__info">
            <p>Status: <strong>{networkStatus.isOnline ? 'Online' : 'Offline'}</strong></p>
            <p>Pending operations: <strong>{networkStatus.queueSize}</strong></p>
          </div>
          
          {networkStatus.queueSize > 0 && (
            <div className="offline-indicator__actions">
              <button 
                className="offline-indicator__sync-btn"
                onClick={() => offlineService._processQueue()}
                disabled={!networkStatus.isOnline}
              >
                Sync now
              </button>
              <button 
                className="offline-indicator__clear-btn"
                onClick={handleClearQueue}
              >
                Clear queue
              </button>
            </div>
          )}
          
          <div className="offline-indicator__help">
            {!networkStatus.isOnline ? (
              <p>Your changes will be saved locally and synchronized when you go back online.</p>
            ) : networkStatus.queueSize > 0 ? (
              <p>Your changes are being synchronized with the server.</p>
            ) : (
              <p>All your changes have been successfully synchronized.</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default OfflineIndicator; 