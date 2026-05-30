import { createSlice } from '@reduxjs/toolkit';

/**
 * Initial state for the geofence slice
 */
const initialState = {
  geofences: [],
  selectedGeofenceId: null,
  loading: false,
  error: null
};

/**
 * Redux slice for managing geofence state
 */
const geofenceSlice = createSlice({
  name: 'geofences',
  initialState,
  reducers: {
    /**
     * Add a new geofence to the state
     */
    addGeofence: (state, action) => {
      state.geofences.push(action.payload);
    },
    
    /**
     * Update an existing geofence
     */
    updateGeofence: (state, action) => {
      const index = state.geofences.findIndex(geofence => geofence.id === action.payload.id);
      if (index !== -1) {
        state.geofences[index] = action.payload;
      }
    },
    
    /**
     * Remove a geofence by ID
     */
    removeGeofence: (state, action) => {
      state.geofences = state.geofences.filter(geofence => geofence.id !== action.payload);
      if (state.selectedGeofenceId === action.payload) {
        state.selectedGeofenceId = null;
      }
    },
    
    /**
     * Set a geofence as selected
     */
    selectGeofence: (state, action) => {
      state.selectedGeofenceId = action.payload;
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
     * Reset geofence state
     */
    resetGeofences: (state) => {
      state.geofences = [];
      state.selectedGeofenceId = null;
      state.loading = false;
      state.error = null;
    }
  }
});

// Export actions
export const {
  addGeofence,
  updateGeofence,
  removeGeofence,
  selectGeofence,
  setLoading,
  setError,
  resetGeofences
} = geofenceSlice.actions;

// Selectors
export const selectAllGeofences = state => state.geofences.geofences;
export const selectGeofenceById = (state, id) => state.geofences.geofences.find(geofence => geofence.id === id);
export const selectSelectedGeofence = state => {
  const { selectedGeofenceId, geofences } = state.geofences;
  return geofences.find(geofence => geofence.id === selectedGeofenceId);
};
export const selectGeofenceLoading = state => state.geofences.loading;
export const selectGeofenceError = state => state.geofences.error;

// Export reducer
export default geofenceSlice.reducer; 