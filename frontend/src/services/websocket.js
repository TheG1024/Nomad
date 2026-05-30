import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * WebSocketService class that handles WebSocket connections using STOMP protocol.
 * This service handles connecting to the WebSocket server, disconnecting, and
 * subscribing to topics for real-time updates of device locations and geofence events.
 */
class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.subscriptions = {};
    this.connected = false;
    this.reconnectionAttempts = 0;
    this.maxReconnectionAttempts = 5;
    this.reconnectionDelay = 2000; // Starting delay in ms
  }

  /**
   * Connect to the WebSocket server
   * @param {Function} onConnect - Callback to execute when connected
   * @param {Function} onError - Callback to execute when there's an error
   */
  connect(onConnect, onError) {
    try {
      // Create a SockJS instance
      const socket = new SockJS('/ws');

      // Initialize STOMP client with SockJS
      this.stompClient = new Client({
        webSocketFactory: () => socket,
        debug: process.env.NODE_ENV === 'development' ? console.log : () => {},
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      // Handle successful connection
      this.stompClient.onConnect = (frame) => {
        console.log('Connected to WebSocket');
        this.connected = true;
        this.reconnectionAttempts = 0;
        
        // Resubscribe to previously subscribed topics
        Object.entries(this.subscriptions).forEach(([topic, callbacks]) => {
          callbacks.forEach(callback => {
            this.subscribe(topic, callback);
          });
        });
        
        if (onConnect) {
          onConnect(frame);
        }
      };

      // Handle connection errors
      this.stompClient.onStompError = (error) => {
        console.error('STOMP Error:', error);
        if (onError) {
          onError(error);
        }
        this.attemptReconnection();
      };

      // Handle WebSocket connection closure
      this.stompClient.onWebSocketClose = () => {
        console.log('WebSocket connection closed');
        this.connected = false;
        this.attemptReconnection();
      };

      // Activate the STOMP client
      this.stompClient.activate();
    } catch (error) {
      console.error('WebSocket connect error:', error);
      if (onError) {
        onError(error);
      }
    }
  }

  /**
   * Disconnect from the WebSocket server
   */
  disconnect() {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.deactivate();
      this.connected = false;
      console.log('Disconnected from WebSocket');
    }
  }

  /**
   * Attempt to reconnect to the WebSocket server with exponential backoff
   */
  attemptReconnection() {
    if (this.reconnectionAttempts < this.maxReconnectionAttempts) {
      this.reconnectionAttempts++;
      const delay = this.reconnectionDelay * Math.pow(1.5, this.reconnectionAttempts - 1);
      
      console.log(`Attempting to reconnect (${this.reconnectionAttempts}/${this.maxReconnectionAttempts}) in ${delay}ms`);
      
      setTimeout(() => {
        console.log(`Reconnecting to WebSocket attempt ${this.reconnectionAttempts}`);
        this.connect();
      }, delay);
    } else {
      console.error('Maximum reconnection attempts reached. Please refresh the page.');
    }
  }

  /**
   * Subscribe to a topic
   * @param {string} topic - The topic to subscribe to
   * @param {Function} callback - The callback function to execute when a message is received
   * @returns {string} The subscription ID
   */
  subscribe(topic, callback) {
    if (!this.stompClient || !this.stompClient.connected) {
      console.warn('Cannot subscribe, STOMP client not connected');
      
      // Store the subscription to be reestablished on reconnection
      if (!this.subscriptions[topic]) {
        this.subscriptions[topic] = [];
      }
      
      if (!this.subscriptions[topic].includes(callback)) {
        this.subscriptions[topic].push(callback);
      }
      
      return null;
    }

    // Subscribe to the topic
    const subscription = this.stompClient.subscribe(topic, (message) => {
      try {
        const payload = JSON.parse(message.body);
        callback(payload);
      } catch (error) {
        console.error('Error parsing message:', error);
      }
    });

    // Store the callback for potential resubscription
    if (!this.subscriptions[topic]) {
      this.subscriptions[topic] = [];
    }
    
    if (!this.subscriptions[topic].includes(callback)) {
      this.subscriptions[topic].push(callback);
    }

    return subscription.id;
  }

  /**
   * Unsubscribe from a topic
   * @param {string} topic - The topic to unsubscribe from
   * @param {Function} callback - The callback function to remove
   */
  unsubscribe(topic, callback) {
    if (this.subscriptions[topic]) {
      const index = this.subscriptions[topic].indexOf(callback);
      if (index !== -1) {
        this.subscriptions[topic].splice(index, 1);
      }
    }
  }

  /**
   * Subscribe to device location updates
   * @param {Function} callback - The callback function to execute when a device update is received
   * @returns {string} The subscription ID
   */
  subscribeToDeviceUpdates(callback) {
    return this.subscribe('/topic/devices', callback);
  }

  /**
   * Subscribe to geofence events
   * @param {Function} callback - The callback function to execute when a geofence event is received
   * @returns {string} The subscription ID
   */
  subscribeToGeofenceEvents(callback) {
    return this.subscribe('/topic/geofence', callback);
  }

  /**
   * Send a device update message
   * @param {Object} deviceUpdate - The device update message to send
   */
  sendDeviceUpdate(deviceUpdate) {
    if (!this.stompClient || !this.stompClient.connected) {
      console.warn('Cannot send message, STOMP client not connected');
      return;
    }

    this.stompClient.publish({
      destination: '/app/device',
      body: JSON.stringify(deviceUpdate),
    });
  }

  /**
   * Send a geofence event message
   * @param {Object} geofenceEvent - The geofence event message to send
   */
  sendGeofenceEvent(geofenceEvent) {
    if (!this.stompClient || !this.stompClient.connected) {
      console.warn('Cannot send message, STOMP client not connected');
      return;
    }

    this.stompClient.publish({
      destination: '/app/geofence',
      body: JSON.stringify(geofenceEvent),
    });
  }
}

// Export a singleton instance of the WebSocketService
export default WebSocketService; 