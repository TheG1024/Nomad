import React, { useState, useRef, useEffect } from 'react';
import L from 'leaflet';
import WebSocketService from '../services/WebSocketService';

/**
 * CyberTracker - Main Dashboard Component
 * Implements Cyber Terminal UI from Mockup 1
 */
const CyberTracker = () => {
  const [activeTab, setActiveTab] = useState("map");
  const mapContainerRef = useRef(null);
  const mapRef = useRef(null);
  const deviceMarkersRef = useRef({});
  const devicePathsRef = useRef({});
  
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedDeviceId, setSelectedDeviceId] = useState("dev-001");
  
  const [devices, setDevices] = useState([
    {
      id: "dev-001",
      name: "Vehicle Alpha",
      location: [40.7128, -74.006],
      status: "online",
      battery: 78,
      lastSeen: "Just now",
      speed: 28,
      direction: "NE",
      fuelLevel: 65,
      lastMaintenance: "2023-02-15",
      assignedDriver: "John Smith",
      history: [
        {lat: 40.7130, lng: -74.0065, timestamp: Date.now() - 1000 * 60 * 30},
        {lat: 40.7129, lng: -74.0062, timestamp: Date.now() - 1000 * 60 * 20},
        {lat: 40.7128, lng: -74.0060, timestamp: Date.now() - 1000 * 60 * 10},
      ],
    },
    {
      id: "dev-002",
      name: "Drone Beta",
      location: [34.0522, -118.2437],
      status: "warning",
      battery: 23,
      lastSeen: "5 min ago",
      speed: 15,
      direction: "SW",
      altitude: 120,
      missionTime: 45,
      operationalRadius: 500,
      history: [
        {lat: 34.0525, lng: -118.2440, timestamp: Date.now() - 1000 * 60 * 15},
        {lat: 34.0523, lng: -118.2438, timestamp: Date.now() - 1000 * 60 * 10},
        {lat: 34.0522, lng: -118.2437, timestamp: Date.now() - 1000 * 60 * 5},
      ],
    },
    {
      id: "dev-003",
      name: "Tracker Gamma",
      location: [51.5074, -0.1278],
      status: "offline",
      battery: 0,
      lastSeen: "2 hours ago",
      speed: 0,
      direction: "N/A",
      lastTransmission: "Signal lost at 14:35",
      signalStrength: 0,
      powerSavingMode: true,
      history: [
        {lat: 51.5080, lng: -0.1285, timestamp: Date.now() - 1000 * 60 * 130},
        {lat: 51.5076, lng: -0.1280, timestamp: Date.now() - 1000 * 60 * 125},
        {lat: 51.5074, lng: -0.1278, timestamp: Date.now() - 1000 * 60 * 120},
      ],
    }
  ]);
  
  const [geofences, setGeofences] = useState([
    { id: "geo-001", name: "Safe Zone Alpha", color: "#00ffff", active: true },
    { id: "geo-002", name: "Restricted Area", color: "#ff00ff", active: true },
    { id: "geo-003", name: "Perimeter Delta", color: "#ffff00", active: false }
  ]);

  const [terminalLog, setTerminalLog] = useState([
    { time: "10:42:01", cmd: "SYSTEM", msg: "Connected to Vehicle Alpha" },
    { time: "10:42:02", cmd: "GPS", msg: "Signal acquired - 12 satellites" },
    { time: "10:42:05", cmd: "GEOFENCE", msg: "Entered Safe Zone Alpha" },
    { time: "10:42:15", cmd: "ALERT", msg: "Drone Beta battery low" },
  ]);

  // Initialize map
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    // Create map container
    const mapElement = document.createElement('div');
    mapElement.id = 'cyber-map';
    mapContainerRef.current.appendChild(mapElement);

    // Initialize Leaflet
    mapRef.current = L.map('cyber-map', {
      zoomControl: false
    }).setView([40.7128, -74.006], 13);

    // Dark tiles
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '© OpenStreetMap © CARTO',
      subdomains: 'abcd',
      maxZoom: 19
    }).addTo(mapRef.current);

    // Add zoom control in corner
    L.control.zoom({ position: 'topright' }).addTo(mapRef.current);

    // Initialize markers
    devices.forEach(device => addDeviceToMap(device));
  }, []);

  // Add device marker
  const addDeviceToMap = (device) => {
    if (!mapRef.current) return;

    const color = device.status === 'online' ? '#00ff88' : 
                  device.status === 'warning' ? '#ffaa00' : '#ff3366';

    const cyberIcon = L.divIcon({
      className: 'custom-marker',
      html: `<div style="
        width: 16px;
        height: 16px;
        background: ${color};
        border-radius: 50%;
        box-shadow: 0 0 10px ${color}, 0 0 20px ${color};
      "></div>`,
      iconSize: [16, 16],
      iconAnchor: [8, 8]
    });

    const marker = L.marker(device.location, { icon: cyberIcon }).addTo(mapRef.current);
    deviceMarkersRef.current[device.id] = marker;

    // Path
    devicePathsRef.current[device.id] = L.polyline([device.location], {
      color: color,
      weight: 2,
      opacity: 0.6,
      dashArray: '5, 10'
    }).addTo(mapRef.current);
  };

  // Handle selecting device
  const handleDeviceSelect = (device) => {
    setSelectedDevice(device);
    setSelectedDeviceId(device.id);
    
    if (mapRef.current) {
      mapRef.current.flyTo(device.location, 13, { duration: 1 });
    }
  };

  // Toggle geofence
  const toggleGeofence = (geoId) => {
    setGeofences(prev => prev.map(g => 
      g.id === geoId ? { ...g, active: !g.active } : g
    ));
  };

  // Get current device
  const currentDevice = devices.find(d => d.id === selectedDeviceId) || devices[0];
  const onlineCount = devices.filter(d => d.status === 'online').length;
  const activeGeofences = geofences.filter(g => g.active).length;

  return (
    <div className="cyber-dashboard">
      {/* Header */}
      <header className="cyber-header">
        <div className="cyber-logo">NOMAD<span>_TRACKER</span></div>
        <div className="header-controls">
          <div className="status-indicator">
            <div className="status-dot"></div>
            <span>SYSTEMS ONLINE</span>
          </div>
          <input 
            type="text" 
            className="cyber-search" 
            placeholder="Search devices..."
          />
        </div>
      </header>

      {/* Sidebar - Device List */}
      <aside className="cyber-sidebar">
        <div className="sidebar-section">
          <div className="sidebar-title">Active Devices</div>
          <div className="device-list">
            {devices.map(device => (
              <div 
                key={device.id}
                className={`device-item ${selectedDeviceId === device.id ? 'active' : ''}`}
                onClick={() => handleDeviceSelect(device)}
              >
                <div className="device-name">{device.name}</div>
                <div className={`device-status ${device.status}`}>
                  <span style={{ textTransform: 'uppercase' }}>{device.status}</span>
                  <span>{device.battery}% batt</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="sidebar-section">
          <div className="sidebar-title">Quick Actions</div>
          <div className="quick-actions">
            <button className="action-btn">+ Add Device</button>
            <button className="action-btn">+ New Zone</button>
            <button className="action-btn">Export</button>
            <button className="action-btn primary">Refresh</button>
          </div>
        </div>
      </aside>

      {/* Main Map */}
      <main className="cyber-main">
        <div className="map-overlay">
          <div className="stat-badge">
            <div className="stat-value">{devices.length}</div>
            <div className="stat-label">Devices</div>
          </div>
          <div className="stat-badge">
            <div className="stat-value">{activeGeofences}</div>
            <div className="stat-label">Active Zones</div>
          </div>
          <div className="stat-badge">
            <div className="stat-value">99.9%</div>
            <div className="stat-label">Uptime</div>
          </div>
        </div>
        <div className="map-container">
          <div ref={mapContainerRef} className="cyber-map"></div>
        </div>
      </main>

      {/* Right Panel */}
      <aside className="cyber-right-panel">
        <div className="panel-card">
          <div className="panel-header">Device Details</div>
          <div className="panel-body">
            <div className="metric-row">
              <span className="metric-label">Name</span>
              <span className="metric-value">{currentDevice.name}</span>
            </div>
            <div className="metric-row">
              <span className="metric-label">Status</span>
              <span className="metric-value" style={{ color: currentDevice.status === 'online' ? '#00ff88' : currentDevice.status === 'warning' ? '#ffaa00' : '#ff3366' }}>
                {currentDevice.status.toUpperCase()}
              </span>
            </div>
            <div className="metric-row">
              <span className="metric-label">Speed</span>
              <span className="metric-value">{currentDevice.speed} km/h</span>
            </div>
            <div className="metric-row">
              <span className="metric-label">Direction</span>
              <span className="metric-value">{currentDevice.direction}</span>
            </div>
            <div className="metric-row">
              <span className="metric-label">Battery</span>
              <span className={`metric-value ${currentDevice.battery < 20 ? 'danger' : currentDevice.battery < 40 ? 'warning' : ''}`}>
                {currentDevice.battery}%
              </span>
            </div>
            <div className="battery-bar">
              <div 
                className={`battery-fill ${currentDevice.battery < 20 ? 'critical' : currentDevice.battery < 40 ? 'low' : ''}`} 
                style={{ width: `${currentDevice.battery}%` }}
              ></div>
            </div>
          </div>
        </div>

        <div className="panel-card">
          <div className="panel-header">Geofences</div>
          <div className="panel-body" style={{ padding: 0 }}>
            {geofences.map(geo => (
              <div key={geo.id} className="geofence-item">
                <div className="geofence-color" style={{ background: geo.color }}></div>
                <span className="geofence-name">{geo.name}</span>
                <div 
                  className={`geofence-toggle ${geo.active ? 'active' : ''}`}
                  onClick={() => toggleGeofence(geo.id)}
                ></div>
              </div>
            ))}
          </div>
        </div>

        <div className="panel-card">
          <div className="panel-header">Terminal Log</div>
          <div className="panel-body" style={{ padding: 0 }}>
            <div className="terminal-output">
              {terminalLog.map((log, i) => (
                <div key={i} className="terminal-line">
                  <span className="time">[{log.time}]</span>
                  <span className="cmd"> {log.cmd}:</span>
                  <span className="msg"> {log.msg}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </aside>
    </div>
  );
};

export default CyberTracker;