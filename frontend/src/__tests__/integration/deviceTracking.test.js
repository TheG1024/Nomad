import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { MockedProvider } from '@apollo/client/testing';
import deviceReducer, { addDevice, selectDevice } from '../../redux/deviceSlice';
import geofenceReducer from '../../redux/geofenceSlice';
import { GET_DEVICE, DEVICE_UPDATED } from '../../graphql/schema';

// Import components (replace with your actual components)
import DeviceTracker from '../../components/DeviceTracker';
import DeviceMap from '../../components/DeviceMap';

// Create a mock store
const createMockStore = () => {
  return configureStore({
    reducer: {
      devices: deviceReducer,
      geofences: geofenceReducer,
    },
  });
};

// Sample device data
const sampleDevice = {
  id: 'device-1',
  name: 'Test Device',
  status: 'online',
  latitude: 37.7749,
  longitude: -122.4194,
  lastUpdated: new Date().toISOString(),
};

// Mock GraphQL responses
const mocks = [
  {
    request: {
      query: GET_DEVICE,
      variables: { deviceId: 'device-1' },
    },
    result: {
      data: {
        device: {
          __typename: 'Device',
          deviceId: 'device-1',
          name: 'Test Device',
          status: 'online',
          latitude: 37.7749,
          longitude: -122.4194,
          speed: 0,
          direction: 0,
          batteryLevel: 85,
          lastUpdated: new Date().toISOString(),
        },
      },
    },
  },
  {
    request: {
      query: DEVICE_UPDATED,
    },
    result: {
      data: {
        deviceUpdated: {
          __typename: 'Device',
          deviceId: 'device-1',
          name: 'Test Device',
          status: 'online',
          latitude: 37.7850, // Updated location
          longitude: -122.4294, // Updated location
          speed: 5,
          direction: 45,
          batteryLevel: 84,
          lastUpdated: new Date().toISOString(),
        },
      },
    },
  },
];

// Mock the Map component
jest.mock('../../components/DeviceMap', () => {
  return jest.fn(() => <div data-testid="device-map">Map Component</div>);
});

describe('Device Tracking Integration Tests', () => {
  let store;
  
  beforeEach(() => {
    store = createMockStore();
    // Initialize store with a device
    store.dispatch(addDevice(sampleDevice));
    store.dispatch(selectDevice(sampleDevice.id));
  });
  
  test('should load and display device information', async () => {
    render(
      <Provider store={store}>
        <MockedProvider mocks={mocks} addTypename={true}>
          <DeviceTracker deviceId="device-1" />
        </MockedProvider>
      </Provider>
    );
    
    // Check if device name is displayed
    await waitFor(() => {
      expect(screen.getByText('Test Device')).toBeInTheDocument();
    });
    
    // Check if status is displayed
    await waitFor(() => {
      expect(screen.getByText(/online/i)).toBeInTheDocument();
    });
    
    // Check if the map is rendered
    expect(screen.getByTestId('device-map')).toBeInTheDocument();
  });
  
  test('should update device location when receiving updates', async () => {
    // Mock the WebSocket service
    const mockWebSocketService = {
      subscribeToDeviceUpdates: jest.fn(callback => {
        // Simulate receiving a device update
        setTimeout(() => {
          callback({
            deviceId: 'device-1',
            latitude: 37.7850,
            longitude: -122.4294,
            status: 'online',
            timestamp: new Date().toISOString(),
          });
        }, 100);
        return 'subscription-id';
      }),
      connect: jest.fn(),
      disconnect: jest.fn(),
    };
    
    // Replace the WebSocket service with our mock
    window.WebSocketService = mockWebSocketService;
    
    render(
      <Provider store={store}>
        <MockedProvider mocks={mocks} addTypename={true}>
          <DeviceTracker deviceId="device-1" />
        </MockedProvider>
      </Provider>
    );
    
    // Verify that subscription was called
    await waitFor(() => {
      expect(mockWebSocketService.subscribeToDeviceUpdates).toHaveBeenCalled();
    });
    
    // Wait for the device update to be processed
    await new Promise(resolve => setTimeout(resolve, 200));
    
    // Check if the device location was updated in the store
    const state = store.getState();
    const device = state.devices.devices.find(d => d.id === 'device-1');
    
    // Location should be updated after receiving the WebSocket update
    expect(device).toBeDefined();
    expect(device.latitude).toBeCloseTo(37.7850, 4);
    expect(device.longitude).toBeCloseTo(-122.4294, 4);
  });
  
  test('should handle offline devices properly', async () => {
    // Create an offline device
    const offlineDevice = {
      ...sampleDevice,
      id: 'device-2',
      status: 'offline',
    };
    
    // Add the offline device to the store
    store.dispatch(addDevice(offlineDevice));
    store.dispatch(selectDevice(offlineDevice.id));
    
    render(
      <Provider store={store}>
        <MockedProvider mocks={mocks} addTypename={true}>
          <DeviceTracker deviceId="device-2" />
        </MockedProvider>
      </Provider>
    );
    
    // Check if offline status is displayed
    await waitFor(() => {
      expect(screen.getByText(/offline/i)).toBeInTheDocument();
    });
    
    // Check for reconnection UI elements
    const reconnectButton = screen.getByText(/reconnect/i);
    expect(reconnectButton).toBeInTheDocument();
    
    // Simulate clicking reconnect
    fireEvent.click(reconnectButton);
    
    // Verify reconnection attempt UI feedback
    await waitFor(() => {
      expect(screen.getByText(/attempting to reconnect/i)).toBeInTheDocument();
    });
  });
}); 