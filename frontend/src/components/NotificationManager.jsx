import React, { useState, useEffect, useCallback } from 'react';

const NotificationManager = () => {
    const [notifications, setNotifications] = useState([]);
    const maxNotifications = 5;
    
    const getIconForType = useCallback((type) => {
        switch (type) {
            case 'ERROR': return 'fa-exclamation-circle';
            case 'WARNING': return 'fa-exclamation-triangle';
            case 'SUCCESS': return 'fa-check-circle';
            case 'GEOFENCE_ENTER': return 'fa-arrow-right-to-bracket';
            case 'GEOFENCE_EXIT': return 'fa-arrow-right-from-bracket';
            case 'DEVICE_UPDATE': return 'fa-location-dot';
            default: return 'fa-info-circle';
        }
    }, []);
    
    const getTitleForType = useCallback((type) => {
        switch (type) {
            case 'ERROR': return 'Error';
            case 'WARNING': return 'Warning';
            case 'SUCCESS': return 'Success';
            case 'GEOFENCE_ENTER': return 'Geofence Entry';
            case 'GEOFENCE_EXIT': return 'Geofence Exit';
            case 'DEVICE_UPDATE': return 'Device Update';
            default: return 'Information';
        }
    }, []);
    
    const addNotification = useCallback((type, message) => {
        const id = 'notification-' + Date.now();
        
        setNotifications(prevNotifications => {
            // Add new notification
            const updatedNotifications = [
                ...prevNotifications, 
                { id, type, message, timestamp: Date.now() }
            ];
            
            // Enforce max notifications
            if (updatedNotifications.length > maxNotifications) {
                return updatedNotifications.slice(updatedNotifications.length - maxNotifications);
            }
            
            return updatedNotifications;
        });
        
        // Auto-remove after 6 seconds
        setTimeout(() => {
            removeNotification(id);
        }, 6000);
        
        return id;
    }, []);
    
    const removeNotification = useCallback((id) => {
        setNotifications(prevNotifications => 
            prevNotifications.filter(notification => notification.id !== id)
        );
    }, []);
    
    // Expose methods globally
    useEffect(() => {
        window.notificationManager = {
            addNotification,
            removeNotification
        };
        
        return () => {
            delete window.notificationManager;
        };
    }, [addNotification, removeNotification]);
    
    return (
        <div id="notifications-area">
            {notifications.map((notification) => (
                <div 
                    key={notification.id} 
                    className={`cyber-notification ${notification.type.toLowerCase()}`}
                    style={{
                        opacity: 1,
                        transform: 'translateX(0)'
                    }}
                >
                    <div className="notification-icon">
                        <i className={`fa ${getIconForType(notification.type)}`}></i>
                    </div>
                    <div className="notification-content">
                        <div className="notification-title">{getTitleForType(notification.type)}</div>
                        <div className="notification-message">{notification.message}</div>
                    </div>
                    <button 
                        className="notification-close" 
                        onClick={() => removeNotification(notification.id)}
                    >
                        <i className="fa fa-times"></i>
                    </button>
                </div>
            ))}
        </div>
    );
};

export default NotificationManager; 