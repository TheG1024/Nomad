// Import jest-dom matchers
import '@testing-library/jest-dom';

// Mock IndexedDB for service worker tests
const indexedDB = {
  open: jest.fn().mockReturnValue({
    onupgradeneeded: jest.fn(),
    onsuccess: jest.fn(),
    onerror: jest.fn(),
  }),
};

global.indexedDB = indexedDB;

// Mock WebSocket Service
jest.mock('./services/WebSocketService', () => {
  return {
    connect: jest.fn(),
    disconnect: jest.fn(),
    sendDeviceUpdate: jest.fn(),
    sendGeofenceEvent: jest.fn(),
  };
});

// Mock the fetch API
global.fetch = jest.fn().mockImplementation(() => 
  Promise.resolve({
    ok: true,
    json: () => Promise.resolve([])
  })
);

// Clean up after each test
afterEach(() => {
  jest.clearAllMocks();
}); 