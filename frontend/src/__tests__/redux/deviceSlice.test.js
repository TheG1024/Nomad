import deviceReducer, { 
  updateDevice, 
  addDevice, 
  removeDevice, 
  selectDevice,
  selectAllDevices,
  selectDeviceById,
  selectSelectedDevice
} from '../../redux/slices/deviceSlice';

describe('deviceSlice', () => {
  const initialState = {
    devices: [],
    selectedDeviceId: null,
    loading: false,
    error: null
  };

  it('should return the initial state', () => {
    expect(deviceReducer(undefined, { type: undefined })).toEqual(initialState);
  });

  it('should handle adding a device', () => {
    const device = {
      deviceId: '123',
      name: 'Test Device',
      status: 'online',
      latitude: 123.456,
      longitude: 78.910
    };

    const newState = deviceReducer(initialState, addDevice(device));
    expect(newState.devices).toHaveLength(1);
    expect(newState.devices[0]).toEqual(device);
  });

  it('should handle updating a device', () => {
    const existingDevice = {
      deviceId: '123',
      name: 'Test Device',
      status: 'online',
      latitude: 123.456,
      longitude: 78.910
    };

    const updatedDevice = {
      deviceId: '123',
      status: 'offline'
    };

    const stateWithDevice = deviceReducer(initialState, addDevice(existingDevice));
    const newState = deviceReducer(stateWithDevice, updateDevice(updatedDevice));
    
    expect(newState.devices).toHaveLength(1);
    expect(newState.devices[0]).toEqual({
      ...existingDevice,
      ...updatedDevice
    });
  });

  it('should handle removing a device', () => {
    const device = {
      deviceId: '123',
      name: 'Test Device'
    };

    const stateWithDevice = deviceReducer(initialState, addDevice(device));
    const newState = deviceReducer(stateWithDevice, removeDevice('123'));
    
    expect(newState.devices).toHaveLength(0);
  });

  it('should handle selecting a device', () => {
    const newState = deviceReducer(initialState, selectDevice('123'));
    expect(newState.selectedDeviceId).toBe('123');
  });

  // Test selectors
  describe('selectors', () => {
    const device1 = { deviceId: '123', name: 'Device 1' };
    const device2 = { deviceId: '456', name: 'Device 2' };
    
    const state = {
      devices: {
        devices: [device1, device2],
        selectedDeviceId: '123',
        loading: false,
        error: null
      }
    };

    it('should select all devices', () => {
      expect(selectAllDevices(state)).toEqual([device1, device2]);
    });

    it('should select device by id', () => {
      expect(selectDeviceById(state, '123')).toEqual(device1);
      expect(selectDeviceById(state, '456')).toEqual(device2);
      expect(selectDeviceById(state, '789')).toBeUndefined();
    });

    it('should select the selected device', () => {
      expect(selectSelectedDevice(state)).toEqual(device1);
    });
  });
}); 