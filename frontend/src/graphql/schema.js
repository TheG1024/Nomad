import { gql } from '@apollo/client';

// Device fragments
export const DEVICE_FRAGMENTS = {
  deviceBase: gql`
    fragment DeviceBase on Device {
      deviceId
      name
      status
      timestamp
    }
  `,
  deviceFull: gql`
    fragment DeviceFull on Device {
      ...DeviceBase
      latitude
      longitude
      speed
      direction
      batteryLevel
      lastUpdated
    }
  `
};

// Geofence fragments
export const GEOFENCE_FRAGMENTS = {
  geofenceBase: gql`
    fragment GeofenceBase on Geofence {
      geofenceId
      name
      type
    }
  `,
  geofenceFull: gql`
    fragment GeofenceFull on Geofence {
      ...GeofenceBase
      points
      radius
      color
      description
      createdAt
    }
  `
};

// Device queries
export const GET_DEVICES = gql`
  query GetDevices {
    devices {
      ...DeviceBase
    }
  }
  ${DEVICE_FRAGMENTS.deviceBase}
`;

export const GET_DEVICE = gql`
  query GetDevice($deviceId: ID!) {
    device(deviceId: $deviceId) {
      ...DeviceFull
    }
  }
  ${DEVICE_FRAGMENTS.deviceFull}
`;

export const GET_DEVICE_HISTORY = gql`
  query GetDeviceHistory($deviceId: ID!, $startTime: String!, $endTime: String!) {
    deviceHistory(deviceId: $deviceId, startTime: $startTime, endTime: $endTime) {
      deviceId
      latitude
      longitude
      timestamp
      status
    }
  }
`;

// Geofence queries
export const GET_GEOFENCES = gql`
  query GetGeofences {
    geofences {
      ...GeofenceBase
    }
  }
  ${GEOFENCE_FRAGMENTS.geofenceBase}
`;

export const GET_GEOFENCE = gql`
  query GetGeofence($geofenceId: ID!) {
    geofence(geofenceId: $geofenceId) {
      ...GeofenceFull
    }
  }
  ${GEOFENCE_FRAGMENTS.geofenceFull}
`;

// Device mutations
export const CREATE_DEVICE = gql`
  mutation CreateDevice($input: DeviceInput!) {
    createDevice(input: $input) {
      ...DeviceFull
    }
  }
  ${DEVICE_FRAGMENTS.deviceFull}
`;

export const UPDATE_DEVICE = gql`
  mutation UpdateDevice($deviceId: ID!, $input: DeviceInput!) {
    updateDevice(deviceId: $deviceId, input: $input) {
      ...DeviceFull
    }
  }
  ${DEVICE_FRAGMENTS.deviceFull}
`;

export const DELETE_DEVICE = gql`
  mutation DeleteDevice($deviceId: ID!) {
    deleteDevice(deviceId: $deviceId) {
      success
      message
    }
  }
`;

// Geofence mutations
export const CREATE_GEOFENCE = gql`
  mutation CreateGeofence($input: GeofenceInput!) {
    createGeofence(input: $input) {
      ...GeofenceFull
    }
  }
  ${GEOFENCE_FRAGMENTS.geofenceFull}
`;

export const UPDATE_GEOFENCE = gql`
  mutation UpdateGeofence($geofenceId: ID!, $input: GeofenceInput!) {
    updateGeofence(geofenceId: $geofenceId, input: $input) {
      ...GeofenceFull
    }
  }
  ${GEOFENCE_FRAGMENTS.geofenceFull}
`;

export const DELETE_GEOFENCE = gql`
  mutation DeleteGeofence($geofenceId: ID!) {
    deleteGeofence(geofenceId: $geofenceId) {
      success
      message
    }
  }
`;

// Subscriptions
export const DEVICE_UPDATED = gql`
  subscription OnDeviceUpdated {
    deviceUpdated {
      ...DeviceFull
    }
  }
  ${DEVICE_FRAGMENTS.deviceFull}
`;

export const GEOFENCE_EVENT = gql`
  subscription OnGeofenceEvent {
    geofenceEvent {
      deviceId
      geofenceId
      eventType
      timestamp
    }
  }
`;

export const NOTIFICATION_ADDED = gql`
  subscription OnNotificationAdded {
    notificationAdded {
      id
      type
      message
      timestamp
    }
  }
`; 