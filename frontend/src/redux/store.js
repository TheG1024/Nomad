import { configureStore } from '@reduxjs/toolkit';
import deviceReducer from './deviceSlice';
import geofenceReducer from './geofenceSlice';

/**
 * Redux store configuration
 */
export const store = configureStore({
  reducer: {
    devices: deviceReducer,
    geofences: geofenceReducer,
  },
  // Enable Redux DevTools extension in development
  devTools: process.env.NODE_ENV !== 'production',
});

export default store; 