import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  devices: [],
  selectedDeviceId: null,
  loading: false,
  error: null
};

export const deviceSlice = createSlice({
  name: 'devices',
  initialState,
  reducers: {
    // Start loading state
    fetchDevicesStart: (state) => {
      state.loading = true;
      state.error = null;
    },
    // Set devices after successful fetch
    fetchDevicesSuccess: (state, action) => {
      state.devices = action.payload;
      state.loading = false;
    },
    // Handle fetch error
    fetchDevicesFailure: (state, action) => {
      state.loading = false;
      state.error = action.payload;
    },
    // Select a device
    selectDevice: (state, action) => {
      state.selectedDeviceId = action.payload;
    },
    // Update a single device
    updateDevice: (state, action) => {
      const updatedDevice = action.payload;
      const index = state.devices.findIndex(d => d.deviceId === updatedDevice.deviceId);
      
      if (index !== -1) {
        state.devices[index] = { ...state.devices[index], ...updatedDevice };
      } else {
        state.devices.push(updatedDevice);
      }
    },
    // Add a new device
    addDevice: (state, action) => {
      state.devices.push(action.payload);
    },
    // Remove a device
    removeDevice: (state, action) => {
      state.devices = state.devices.filter(device => device.deviceId !== action.payload);
      if (state.selectedDeviceId === action.payload) {
        state.selectedDeviceId = null;
      }
    }
  }
});

// Export actions
export const { 
  fetchDevicesStart, 
  fetchDevicesSuccess, 
  fetchDevicesFailure,
  selectDevice,
  updateDevice,
  addDevice,
  removeDevice
} = deviceSlice.actions;

// Selectors
export const selectAllDevices = state => state.devices.devices;
export const selectDeviceById = (state, deviceId) => 
  state.devices.devices.find(device => device.deviceId === deviceId);
export const selectSelectedDevice = state => {
  const { selectedDeviceId, devices } = state.devices;
  return devices.find(device => device.deviceId === selectedDeviceId) || null;
};
export const selectDevicesLoading = state => state.devices.loading;
export const selectDevicesError = state => state.devices.error;

// Thunks
export const fetchDevices = () => async (dispatch) => {
  try {
    dispatch(fetchDevicesStart());
    const response = await fetch('/api/devices');
    
    if (!response.ok) {
      throw new Error('Failed to fetch devices');
    }
    
    const data = await response.json();
    dispatch(fetchDevicesSuccess(data));
  } catch (error) {
    dispatch(fetchDevicesFailure(error.message));
  }
};

export default deviceSlice.reducer; 