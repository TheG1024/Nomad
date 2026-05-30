import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.connected = false;
        this.stompClient = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 2000;
        this.subscriptions = {
            deviceUpdates: null,
            geofenceEvents: null,
            notifications: null
        };
        this.callbacks = {
            onDeviceUpdate: null,
            onGeofenceEvent: null,
            onNotification: null,
            onConnect: null,
            onDisconnect: null
        };
    }

    connect(callbacks = {}) {
        // Store callbacks
        this.callbacks = { ...this.callbacks, ...callbacks };

        const socket = new SockJS('/ws');
        this.stompClient = new Client({
            webSocketFactory: () => socket,
            debug: function (str) {
                // Suppress debug logs in production
                if (process.env.NODE_ENV !== 'production') {
                    console.log(str);
                }
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000
        });

        this.stompClient.onConnect = (frame) => {
            console.log('Connected to WebSocket');
            this.connected = true;
            this.reconnectAttempts = 0;

            // Subscribe to channels
            this.subscribeToChannels();

            // Invoke callback
            if (this.callbacks.onConnect) {
                this.callbacks.onConnect(frame);
            }
        };

        this.stompClient.onStompError = (error) => {
            console.error('WebSocket connection error:', error);
            this.connected = false;
            this.attemptReconnect();
            
            // Invoke callback
            if (this.callbacks.onDisconnect) {
                this.callbacks.onDisconnect(error);
            }
        };

        // Activate the client
        this.stompClient.activate();
    }

    disconnect() {
        if (this.stompClient) {
            // Unsubscribe from all topics
            Object.values(this.subscriptions).forEach(subscription => {
                if (subscription) {
                    this.stompClient.unsubscribe(subscription);
                }
            });
            
            this.stompClient.deactivate();
            this.connected = false;
            console.log('Disconnected from WebSocket');
            
            // Invoke callback
            if (this.callbacks.onDisconnect) {
                this.callbacks.onDisconnect();
            }
        }
    }

    attemptReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('Max reconnection attempts reached');
            
            // Invoke notification callback for error
            if (this.callbacks.onNotification) {
                this.callbacks.onNotification({
                    type: 'ERROR',
                    message: 'Failed to reconnect to server'
                });
            }
            return;
        }
        
        this.reconnectAttempts++;
        
        // Invoke notification callback for warning
        if (this.callbacks.onNotification) {
            this.callbacks.onNotification({
                type: 'WARNING',
                message: `Connection lost. Reconnecting (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`
            });
        }
        
        setTimeout(() => {
            console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            this.connect();
        }, this.reconnectDelay);
    }

    subscribeToChannels() {
        if (!this.connected || !this.stompClient) return;
        
        // Device updates
        this.subscriptions.deviceUpdates = this.stompClient.subscribe('/topic/device/updates', (message) => {
            const update = JSON.parse(message.body);
            console.log('Received device update:', update);
            
            // Invoke callback
            if (this.callbacks.onDeviceUpdate) {
                this.callbacks.onDeviceUpdate(update);
            }
        });
        
        // Geofence events
        this.subscriptions.geofenceEvents = this.stompClient.subscribe('/topic/geofence/events', (message) => {
            const event = JSON.parse(message.body);
            console.log('Received geofence event:', event);
            
            // Invoke callback
            if (this.callbacks.onGeofenceEvent) {
                this.callbacks.onGeofenceEvent(event);
            }
        });
        
        // General notifications
        this.subscriptions.notifications = this.stompClient.subscribe('/topic/notifications', (message) => {
            const notification = JSON.parse(message.body);
            console.log('Received notification:', notification);
            
            // Invoke callback
            if (this.callbacks.onNotification) {
                this.callbacks.onNotification(notification);
            }
        });
    }

    sendDeviceUpdate(deviceId, latitude, longitude, status = 'online') {
        if (!this.connected || !this.stompClient) {
            console.error('Cannot send update: Not connected');
            return;
        }
        
        const update = {
            deviceId,
            latitude,
            longitude,
            timestamp: Date.now(),
            status
        };
        
        this.stompClient.publish({
            destination: '/app/device/update',
            body: JSON.stringify(update)
        });
    }

    sendGeofenceEvent(deviceId, geofenceId, eventType) {
        if (!this.connected || !this.stompClient) {
            console.error('Cannot send event: Not connected');
            return;
        }
        
        const event = {
            deviceId,
            geofenceId,
            eventType,
            timestamp: Date.now()
        };
        
        this.stompClient.publish({
            destination: '/app/geofence/event',
            body: JSON.stringify(event)
        });
    }
}

// Export as singleton
export default new WebSocketService(); 