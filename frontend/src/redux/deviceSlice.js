import { createSlice } from '@reduxjs/toolkit';

/**
 * Initial state for the device slice
 */
const initialState = {
  devices: [],
  selectedDeviceId: null,
  loading: false,
  error: null
};

/**
 * Redux slice for managing device state
 */
const deviceSlice = createSlice({
  name: 'devices',
  initialState,
  reducers: {
    /**
     * Add a new device to the state
     */
    addDevice: (state, action) => {
      state.devices.push(action.payload);
    },
    
    /**
     * Update an existing device
     */
    updateDevice: (state, action) => {
      const index = state.devices.findIndex(device => device.id === action.payload.id);
      if (index !== -1) {
        state.devices[index] = action.payload;
      }
    },
    
    /**
     * Remove a device by ID
     */
    removeDevice: (state, action) => {
      state.devices = state.devices.filter(device => device.id !== action.payload);
      if (state.selectedDeviceId === action.payload) {
        state.selectedDeviceId = null;
      }
    },
    
    /**
     * Set a device as selected
     */
    selectDevice: (state, action) => {
      state.selectedDeviceId = action.payload;
    },
    
    /**
     * Set device location
     */
    setDeviceLocation: (state, action) => {
      const { id, latitude, longitude, timestamp } = action.payload;
      const index = state.devices.findIndex(device => device.id === id);
      
      if (index !== -1) {
        state.devices[index] = {
          ...state.devices[index],
          latitude,
          longitude,
          lastUpdated: timestamp || new Date().toISOString()
        };
      }
    },
    
    /**
     * Set loading state
     */
    setLoading: (state, action) => {
      state.loading = action.payload;
    },
    
    /**
     * Set error state
     */
    setError: (state, action) => {
      state.error = action.payload;
    },
    
    /**
     * Reset device state
     */
    resetDevices: (state) => {
      state.devices = [];
      state.selectedDeviceId = null;
      state.loading = false;
      state.error = null;
    }
  }
});

// Export actions
export const {
  addDevice,
  updateDevice,
  removeDevice,
  selectDevice,
  setDeviceLocation,
  setLoading,
  setError,
  resetDevices
} = deviceSlice.actions;

// Selectors
export const selectAllDevices = state => state.devices.devices;
export const selectDeviceById = (state, id) => state.devices.devices.find(device => device.id === id);
export const selectSelectedDevice = state => {
  const { selectedDeviceId, devices } = state.devices;
  return devices.find(device => device.id === selectedDeviceId);
};
export const selectDeviceLoading = state => state.devices.loading;
export const selectDeviceError = state => state.devices.error;

// Export reducer
export default deviceSlice.reducer; 