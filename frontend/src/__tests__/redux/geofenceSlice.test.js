import geofenceReducer, {
  addGeofence,
  updateGeofence,
  removeGeofence,
  selectGeofence,
  selectAllGeofences,
  selectGeofenceById,
  selectSelectedGeofence
} from '../../redux/geofenceSlice';

describe('Geofence Redux Slice', () => {
  const initialState = {
    geofences: [],
    selectedGeofenceId: null,
    loading: false,
    error: null
  };

  const sampleGeofence1 = {
    id: 'geo1',
    name: 'Home',
    description: 'Home area',
    radius: 100,
    centerLatitude: 37.7749,
    centerLongitude: -122.4194,
    color: '#FF0000'
  };

  const sampleGeofence2 = {
    id: 'geo2',
    name: 'Work',
    description: 'Work area',
    radius: 200,
    centerLatitude: 37.7833,
    centerLongitude: -122.4167,
    color: '#00FF00'
  };

  test('should return the initial state when passed an empty action', () => {
    expect(geofenceReducer(undefined, { type: '' })).toEqual(initialState);
  });

  test('should handle adding a geofence', () => {
    const actual = geofenceReducer(initialState, addGeofence(sampleGeofence1));
    
    expect(actual.geofences).toHaveLength(1);
    expect(actual.geofences[0]).toEqual(sampleGeofence1);
  });

  test('should handle adding multiple geofences', () => {
    let state = geofenceReducer(initialState, addGeofence(sampleGeofence1));
    state = geofenceReducer(state, addGeofence(sampleGeofence2));
    
    expect(state.geofences).toHaveLength(2);
    expect(state.geofences[0]).toEqual(sampleGeofence1);
    expect(state.geofences[1]).toEqual(sampleGeofence2);
  });

  test('should handle updating a geofence', () => {
    // First add a geofence
    let state = geofenceReducer(initialState, addGeofence(sampleGeofence1));
    
    // Then update it
    const updatedGeofence = {
      ...sampleGeofence1,
      name: 'Updated Home',
      radius: 150
    };
    
    state = geofenceReducer(state, updateGeofence(updatedGeofence));
    
    expect(state.geofences).toHaveLength(1);
    expect(state.geofences[0].name).toEqual('Updated Home');
    expect(state.geofences[0].radius).toEqual(150);
  });

  test('should handle removing a geofence', () => {
    // First add two geofences
    let state = geofenceReducer(initialState, addGeofence(sampleGeofence1));
    state = geofenceReducer(state, addGeofence(sampleGeofence2));
    
    // Then remove one
    state = geofenceReducer(state, removeGeofence(sampleGeofence1.id));
    
    expect(state.geofences).toHaveLength(1);
    expect(state.geofences[0]).toEqual(sampleGeofence2);
  });

  test('should handle selecting a geofence', () => {
    // First add two geofences
    let state = geofenceReducer(initialState, addGeofence(sampleGeofence1));
    state = geofenceReducer(state, addGeofence(sampleGeofence2));
    
    // Then select one
    state = geofenceReducer(state, selectGeofence(sampleGeofence2.id));
    
    expect(state.selectedGeofenceId).toEqual(sampleGeofence2.id);
  });

  test('selectors should return the correct data', () => {
    // First create a state with two geofences and one selected
    let state = geofenceReducer(initialState, addGeofence(sampleGeofence1));
    state = geofenceReducer(state, addGeofence(sampleGeofence2));
    state = geofenceReducer(state, selectGeofence(sampleGeofence1.id));
    
    const rootState = { geofences: state };
    
    // Test selectors
    expect(selectAllGeofences(rootState)).toEqual([sampleGeofence1, sampleGeofence2]);
    expect(selectGeofenceById(rootState, sampleGeofence1.id)).toEqual(sampleGeofence1);
    expect(selectGeofenceById(rootState, 'nonexistent')).toBeUndefined();
    expect(selectSelectedGeofence(rootState)).toEqual(sampleGeofence1);
  });
}); 