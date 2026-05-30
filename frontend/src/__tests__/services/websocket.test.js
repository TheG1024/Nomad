import WebSocketService from '../../services/websocket';

// Mock the SockJS and StompJS dependencies
jest.mock('@stomp/stompjs', () => {
  return {
    Client: jest.fn().mockImplementation(() => ({
      activate: jest.fn(),
      deactivate: jest.fn(),
      connected: true,
      subscribe: jest.fn().mockReturnValue({ id: 'subscription-id' }),
      publish: jest.fn(),
      onConnect: null,
      onStompError: null,
      onWebSocketClose: null,
    })),
  };
});

jest.mock('sockjs-client', () => {
  return jest.fn().mockImplementation(() => ({
    close: jest.fn(),
  }));
});

describe('WebSocketService', () => {
  let webSocketService;
  let mockCallback;
  let mockErrorCallback;

  beforeEach(() => {
    jest.clearAllMocks();
    mockCallback = jest.fn();
    mockErrorCallback = jest.fn();
    webSocketService = new WebSocketService();
  });

  test('connect initializes the STOMP client', () => {
    webSocketService.connect();
    expect(webSocketService.stompClient.activate).toHaveBeenCalled();
  });

  test('disconnect deactivates the STOMP client', () => {
    webSocketService.connect();
    webSocketService.disconnect();
    expect(webSocketService.stompClient.deactivate).toHaveBeenCalled();
  });

  test('subscribes to device updates', () => {
    webSocketService.connect();
    webSocketService.subscribeToDeviceUpdates(mockCallback);
    
    expect(webSocketService.stompClient.subscribe).toHaveBeenCalledWith(
      '/topic/devices',
      expect.any(Function)
    );
  });

  test('subscribes to geofence events', () => {
    webSocketService.connect();
    webSocketService.subscribeToGeofenceEvents(mockCallback);
    
    expect(webSocketService.stompClient.subscribe).toHaveBeenCalledWith(
      '/topic/geofence',
      expect.any(Function)
    );
  });

  test('sends device update message', () => {
    const deviceUpdate = {
      deviceId: '123',
      latitude: 37.7749,
      longitude: -122.4194,
      timestamp: new Date().toISOString(),
    };

    webSocketService.connect();
    webSocketService.sendDeviceUpdate(deviceUpdate);
    
    expect(webSocketService.stompClient.publish).toHaveBeenCalledWith({
      destination: '/app/device',
      body: JSON.stringify(deviceUpdate),
    });
  });

  test('sends geofence event message', () => {
    const geofenceEvent = {
      geofenceId: '456',
      deviceId: '123',
      eventType: 'ENTER',
      timestamp: new Date().toISOString(),
    };

    webSocketService.connect();
    webSocketService.sendGeofenceEvent(geofenceEvent);
    
    expect(webSocketService.stompClient.publish).toHaveBeenCalledWith({
      destination: '/app/geofence',
      body: JSON.stringify(geofenceEvent),
    });
  });
}); 