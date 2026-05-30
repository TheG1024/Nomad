import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  geofences: [],
  selectedGeofenceId: null,
  loading: false,
  error: null
};

export const geofenceSlice = createSlice({
  name: 'geofences',
  initialState,
  reducers: {
    // Start loading state
    fetchGeofencesStart: (state) => {
      state.loading = true;
      state.error = null;
    },
    // Set geofences after successful fetch
    fetchGeofencesSuccess: (state, action) => {
      state.geofences = action.payload;
      state.loading = false;
    },
    // Handle fetch error
    fetchGeofencesFailure: (state, action) => {
      state.loading = false;
      state.error = action.payload;
    },
    // Select a geofence
    selectGeofence: (state, action) => {
      state.selectedGeofenceId = action.payload;
    },
    // Add a new geofence
    addGeofence: (state, action) => {
      state.geofences.push(action.payload);
    },
    // Update a geofence
    updateGeofence: (state, action) => {
      const updatedGeofence = action.payload;
      const index = state.geofences.findIndex(g => g.geofenceId === updatedGeofence.geofenceId);
      
      if (index !== -1) {
        state.geofences[index] = { ...state.geofences[index], ...updatedGeofence };
      }
    },
    // Remove a geofence
    removeGeofence: (state, action) => {
      state.geofences = state.geofences.filter(geofence => geofence.geofenceId !== action.payload);
      if (state.selectedGeofenceId === action.payload) {
        state.selectedGeofenceId = null;
      }
    }
  }
});

// Export actions
export const {
  fetchGeofencesStart,
  fetchGeofencesSuccess,
  fetchGeofencesFailure,
  selectGeofence,
  addGeofence,
  updateGeofence,
  removeGeofence
} = geofenceSlice.actions;

// Selectors
export const selectAllGeofences = state => state.geofences.geofences;
export const selectGeofenceById = (state, geofenceId) => 
  state.geofences.geofences.find(geofence => geofence.geofenceId === geofenceId);
export const selectSelectedGeofence = state => {
  const { selectedGeofenceId, geofences } = state.geofences;
  return geofences.find(geofence => geofence.geofenceId === selectedGeofenceId) || null;
};
export const selectGeofencesLoading = state => state.geofences.loading;
export const selectGeofencesError = state => state.geofences.error;

// Thunks
export const fetchGeofences = () => async (dispatch) => {
  try {
    dispatch(fetchGeofencesStart());
    const response = await fetch('/api/geofences');
    
    if (!response.ok) {
      throw new Error('Failed to fetch geofences');
    }
    
    const data = await response.json();
    dispatch(fetchGeofencesSuccess(data));
  } catch (error) {
    dispatch(fetchGeofencesFailure(error.message));
  }
};

export default geofenceSlice.reducer; 