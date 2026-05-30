"use strict";

const CyberTracker = () => {
  const [activeTab, setActiveTab] = React.useState("map");
  const [showGrid, setShowGrid] = React.useState(true);
  const canvasRef = React.useRef(null);
  const mapRef = React.useRef(null);

  // Add new state variables for map features
  const [mapLayers, setMapLayers] = React.useState({
    heatmap: false,
    traffic: false,
    satellite: false,
    deviceHistory: true
  });

  const [selectedDevice, setSelectedDevice] = React.useState(null);
  const [drawingMode, setDrawingMode] = React.useState(false);
  const [showDeviceDetails, setShowDeviceDetails] = React.useState(false);
  const [deviceDetailId, setDeviceDetailId] = React.useState(null);

  // Command Center state
  const [commandDevice, setCommandDevice] = React.useState("dev-001");
  const [commandHistory, setCommandHistory] = React.useState([]);
  const [pendingCommands, setPendingCommands] = React.useState(new Set());
  
  const [devices, setDevices] = React.useState([
    {
      id: "dev-001",
      name: "Vehicle Alpha",
      location: [40.7128, -74.006],
      status: "online",
      battery: 78,
      lastSeen: "Just now",
      history: [
        {lat: 40.7130, lng: -74.0065, timestamp: Date.now() - 1000 * 60 * 30},
        {lat: 40.7129, lng: -74.0062, timestamp: Date.now() - 1000 * 60 * 20},
        {lat: 40.7128, lng: -74.0060, timestamp: Date.now() - 1000 * 60 * 10},
      ],
      speed: 28,
      direction: "NE",
      fuelLevel: 65,
      lastMaintenance: "2023-02-15",
      assignedDriver: "John Smith"
    },
    {
      id: "dev-002",
      name: "Drone Beta",
      location: [34.0522, -118.2437],
      status: "warning",
      battery: 23,
      lastSeen: "5 min ago",
      history: [
        {lat: 34.0525, lng: -118.2440, timestamp: Date.now() - 1000 * 60 * 15},
        {lat: 34.0523, lng: -118.2438, timestamp: Date.now() - 1000 * 60 * 10},
        {lat: 34.0522, lng: -118.2437, timestamp: Date.now() - 1000 * 60 * 5},
      ],
      speed: 15,
      direction: "SW",
      altitude: 120,
      missionTime: 45,
      operationalRadius: 500
    },
    {
      id: "dev-003",
      name: "Tracker Gamma",
      location: [51.5074, -0.1278],
      status: "offline",
      battery: 0,
      lastSeen: "2 hours ago",
      history: [
        {lat: 51.5080, lng: -0.1285, timestamp: Date.now() - 1000 * 60 * 130},
        {lat: 51.5076, lng: -0.1280, timestamp: Date.now() - 1000 * 60 * 125},
        {lat: 51.5074, lng: -0.1278, timestamp: Date.now() - 1000 * 60 * 120},
      ],
      speed: 0,
      direction: "N/A",
      lastTransmission: "Signal lost at 14:35",
      signalStrength: 0,
      powerSavingMode: true
    }
  ]);
  
  const [geofences, setGeofences] = React.useState([
    { id: "geo-001", name: "Safe Zone Alpha", color: "#00ffff", active: true },
    { id: "geo-002", name: "Restricted Area", color: "#ff00ff", active: true },
    { id: "geo-003", name: "Perimeter Delta", color: "#ffff00", active: false }
  ]);
  
  React.useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !showGrid) return;
    
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    
    let animationFrameId;
    let time = 0;
    
    const resize = () => {
      if (!canvas) return;
      canvas.width = canvas.offsetWidth;
      canvas.height = canvas.offsetHeight;
    };
    
    const drawGrid = () => {
      if (!canvas || !ctx) return;
      
      ctx.fillStyle = "rgba(0,0,0,.1)";
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      
      const size = Math.min(canvas.width, canvas.height) / 20;
      const gridWidth = Math.ceil(canvas.width / size) * 2;
      const gridHeight = Math.ceil(canvas.height / (size * 0.5)) * 2;
      const centerX = canvas.width / 2;
      const centerY = canvas.height / 2;
      
      for (let y = -gridHeight; y < gridHeight; y++) {
        for (let x = -gridWidth; x < gridWidth; x++) {
          const posX = centerX + ((x - y) * size) / 2;
          const posY = centerY + ((x + y) * size) / 4;
          const distance = Math.sqrt(x * x + y * y);
          const maxDistance = Math.sqrt(gridWidth * gridWidth + gridHeight * gridHeight);
          const scale = 1 - distance / maxDistance;
          const height = size * scale * Math.abs(Math.sin(distance * 0.3 + time));
          
          ctx.beginPath();
          ctx.moveTo(posX, posY - height);
          ctx.lineTo(posX + size / 2, posY - size / 4 - height);
          ctx.lineTo(posX + size, posY - height);
          ctx.lineTo(posX + size, posY);
          ctx.lineTo(posX + size / 2, posY + size / 4);
          ctx.lineTo(posX, posY);
          ctx.closePath();
          
          const gradient = ctx.createLinearGradient(posX, posY - height, posX + size, posY);
          gradient.addColorStop(0, "rgba(0,255,255,.4)");
          gradient.addColorStop(1, "rgba(255,0,255,.4)");
          ctx.fillStyle = gradient;
          ctx.fill();
          ctx.strokeStyle = "rgba(255,255,0,.3)";
          ctx.stroke();
        }
      }
    };
    
    const animate = () => {
      if (!canvas || !ctx) return;
      drawGrid();
      time += 0.02;
      animationFrameId = requestAnimationFrame(animate);
    };
    
    window.addEventListener("resize", resize);
    resize();
    animate();
    
    return () => {
      window.removeEventListener("resize", resize);
      cancelAnimationFrame(animationFrameId);
    };
  }, [showGrid]);
  
  const getStatusColor = (status) => {
    switch (status) {
      case "online": return "status-online";
      case "warning": return "status-warning";
      case "offline": return "status-offline";
      default: return "status-unknown";
    }
  };
  
  // Functions to handle API calls
  const fetchGeofences = () => {
    fetch('/api/geofence', {
      headers: {
        'Authorization': 'Basic ' + btoa('admin:admin')
      }
    })
      .then(response => response.json())
      .then(data => {
        if (data.success && data.data) {
          // Transform the data to match our state structure
          const mappedGeofences = data.data.map(gf => ({
            id: gf.id,
            name: gf.name,
            color: gf.color || "#00ffff", // Default color if none provided
            active: gf.active
          }));
          setGeofences(mappedGeofences);
        }
      })
      .catch(error => console.error('Error fetching geofences:', error));
  };
  
  const createGeofence = () => {
    const name = prompt("Enter geofence name:");
    if (!name) return;
    
    fetch('/api/geofence/circle', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Basic ' + btoa('admin:admin')
      },
      body: JSON.stringify({
        deviceId: "test-device-01",
        name: name,
        description: "Created from CyberTrack UI",
        centerLatitude: 40.7128,
        centerLongitude: -74.0060,
        radiusMeters: 1000,
        category: "ui-created",
        alertLevel: 1
      })
    })
      .then(response => response.json())
      .then(data => {
        if (data.success) {
          fetchGeofences(); // Refresh the list
        } else {
          alert("Failed to create geofence: " + data.message);
        }
      })
      .catch(error => console.error('Error creating geofence:', error));
  };
  
  // Load geofences when component mounts
  React.useEffect(() => {
    fetchGeofences();
  }, []);
  
  // New function to toggle map layers
  const toggleMapLayer = (layer) => {
    setMapLayers({
      ...mapLayers,
      [layer]: !mapLayers[layer]
    });
    
    // Apply the changes to the map if it exists
    if (mapRef.current) {
      // Implementation would depend on the Leaflet map instance
      console.log(`Toggling ${layer} layer to ${!mapLayers[layer]}`);
    }
  };
  
  // New function to handle device selection
  const handleDeviceSelect = (deviceId) => {
    const device = devices.find(d => d.id === deviceId);
    setSelectedDevice(device);
    
    // Center map on selected device
    if (mapRef.current && device) {
      mapRef.current.setView(device.location, 13);
    }
  };
  
  // Function to toggle device details panel
  const toggleDeviceDetails = (deviceId) => {
    if (deviceDetailId === deviceId && showDeviceDetails) {
      setShowDeviceDetails(false);
      setDeviceDetailId(null);
    } else {
      setShowDeviceDetails(true);
      setDeviceDetailId(deviceId);
      // Also select the device on the map
      handleDeviceSelect(deviceId);
    }
  };
  
  // Function to start drawing a geofence
  const startDrawingGeofence = () => {
    setDrawingMode(true);
    // This would enable drawing on the map
    // Implementation would depend on the Leaflet drawing plugin
  };

  // ── Command Center ──────────────────────────────────────────────

  const sendCommand = (commandType, params) => {
    const cmdId = 'CMD-' + Date.now().toString(36).toUpperCase();
    const entry = {
      id: cmdId,
      deviceId: commandDevice,
      type: commandType,
      status: 'pending',
      message: 'Command sent...',
      issuedAt: new Date().toLocaleTimeString()
    };

    setCommandHistory(prev => [entry, ...prev].slice(0, 50));
    setPendingCommands(prev => new Set([...prev, cmdId]));

    fetch('/api/commands', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        deviceId: commandDevice,
        commandType: commandType,
        issuedBy: 'operator'
      })
    })
    .then(r => r.json())
    .then(data => {
      if (!data.success) {
        updateCommandEntry(cmdId, 'failed', data.message || 'Command rejected');
      }
    })
    .catch(err => {
      updateCommandEntry(cmdId, 'failed', err.message);
    });
  };

  const updateCommandEntry = (cmdId, status, message) => {
    setCommandHistory(prev => prev.map(e =>
      e.id === cmdId ? { ...e, status, message } : e
    ));
    setPendingCommands(prev => {
      const next = new Set(prev);
      next.delete(cmdId);
      return next;
    });
  };

  // Listen for command response events from the WebSocket
  React.useEffect(() => {
    const handler = (e) => {
      const resp = e.detail;
      // Find matching pending entry by deviceId + status
      setCommandHistory(prev => {
        const idx = prev.findIndex(
          h => h.deviceId === resp.deviceId && h.status === 'pending'
        );
        if (idx === -1) return prev;
        const next = [...prev];
        next[idx] = {
          ...next[idx],
          status: resp.status === 'SUCCESS' ? 'success' : 'failed',
          message: resp.message || resp.resultMessage || `Command ${resp.status}`
        };
        setPendingCommands(p => {
          const n = new Set(p);
          n.delete(next[idx].id);
          return n;
        });
        return next;
      });
    };
    window.addEventListener('commandResponse', handler);
    return () => window.removeEventListener('commandResponse', handler);
  }, []);

  const commandDefs = [
    { type: 'LOCK',        label: 'Lock',          icon: 'icon lock',         danger: true  },
    { type: 'UNLOCK',      label: 'Unlock',         icon: 'icon lock-open',    danger: false },
    { type: 'REBOOT',      label: 'Reboot',         icon: 'icon refresh-ccw',  danger: true  },
    { type: 'PING',        label: 'Ping',           icon: 'icon radio',        danger: false },
    { type: 'SHUTDOWN',    label: 'Shutdown',       icon: 'icon power',        danger: true  },
    { type: 'SET_SPEED_THRESHOLD', label: 'Speed Limit', icon: 'icon gauge',    danger: false },
  ];

  const getCommandStatusIcon = (status) => {
    switch (status) {
      case 'pending':  return '⟳';
      case 'success':  return '✓';
      case 'failed':   return '✗';
      default:         return '·';
    }
  };

  const getCommandStatusClass = (status) => {
    switch (status) {
      case 'pending':  return 'status-pending';
      case 'success':  return 'status-success';
      case 'failed':   return 'status-failed';
      default:         return '';
    }
  };

  return (
    <div className="cyber-tracker">
      {/* Background Grid */}
      {showGrid && (
        <canvas 
          ref={canvasRef} 
          className="grid-canvas"
        />
      )}
      
      {/* Header */}
      <div className="cyber-header">
        <div className="logo">
          <i className="icon zap"></i>
          <h1 className="title">
            CYBER<span className="highlight">TRACK</span>
          </h1>
        </div>
        <div className="controls">
          {selectedDevice && (
            <div className="selected-device-chip">
              <span className={`status-indicator ${getStatusColor(selectedDevice.status)}`}></span>
              <span className="selected-name">{selectedDevice.name}</span>
              <button className="cyber-button micro" onClick={() => setSelectedDevice(null)}>
                <i className="icon x"></i>
              </button>
            </div>
          )}
          <button className="cyber-button small">
            <i className="icon settings"></i>
            System
          </button>
          <div className="toggle-control">
            <span className="label">Grid</span>
            <label className="switch">
              <input 
                type="checkbox" 
                checked={showGrid} 
                onChange={e => setShowGrid(e.target.checked)} 
              />
              <span className="slider"></span>
            </label>
          </div>
        </div>
      </div>
      
      {/* Main Content */}
      <div className="cyber-content">
        {/* Left Sidebar */}
        <div className="cyber-sidebar">
          <div className="cyber-card">
            <div className="card-header">
              <h2 className="card-title">
                <i className="icon smartphone"></i>
                Devices
              </h2>
            </div>
            <div className="card-content">
              {devices.map(device => (
                <div 
                  key={device.id} 
                  className={`device-item ${selectedDevice && selectedDevice.id === device.id ? 'selected' : ''}`}
                  onClick={() => handleDeviceSelect(device.id)}
                  onDoubleClick={() => toggleDeviceDetails(device.id)}
                >
                  <div className="device-header">
                    <span className="device-name">{device.name}</span>
                    <span className={`status-badge ${getStatusColor(device.status)}`}>
                      {device.status}
                    </span>
                  </div>
                  <div className="device-details">
                    <span>Batt: {device.battery}%</span>
                    <span>{device.lastSeen}</span>
                  </div>
                  <div className="device-actions">
                    <button 
                      className="cyber-button micro" 
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleDeviceDetails(device.id);
                      }}
                    >
                      <i className="icon info"></i>
                    </button>
                    <button 
                      className="cyber-button micro" 
                      onClick={(e) => {
                        e.stopPropagation();
                        // Center on map
                        handleDeviceSelect(device.id);
                      }}
                    >
                      <i className="icon crosshair"></i>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
          
          <div className="cyber-card">
            <div className="card-header">
              <h2 className="card-title">
                <i className="icon shield"></i>
                Geofences
              </h2>
            </div>
            <div className="card-content">
              {geofences.map(fence => (
                <div key={fence.id} className="geofence-item">
                  <div className="geofence-header">
                    <div className="geofence-name">
                      <div 
                        className="color-dot" 
                        style={{ backgroundColor: fence.color }}
                      ></div>
                      <span>{fence.name}</span>
                    </div>
                    <label className="switch small">
                      <input 
                        type="checkbox" 
                        checked={fence.active} 
                        onChange={() => {}} 
                      />
                      <span className="slider"></span>
                    </label>
                  </div>
                </div>
              ))}
              <button className="cyber-button full" onClick={createGeofence}>
                + Add Geofence
              </button>
            </div>
          </div>
          
          {showDeviceDetails && deviceDetailId && (
            <div className="cyber-card device-details-card">
              <div className="card-header">
                <h2 className="card-title">
                  <i className="icon cpu"></i>
                  Device Details
                </h2>
                <button 
                  className="close-button" 
                  onClick={() => setShowDeviceDetails(false)}
                >
                  <i className="icon x"></i>
                </button>
              </div>
              <div className="card-content">
                {(() => {
                  const device = devices.find(d => d.id === deviceDetailId);
                  if (!device) return <p>Device not found</p>;
                  
                  return (
                    <div className="device-detail-content">
                      <h3 className="device-title">{device.name}</h3>
                      <div className="detail-grid">
                        <div className="detail-item">
                          <span className="detail-label">Status</span>
                          <span className={`detail-value ${getStatusColor(device.status)}`}>
                            {device.status.toUpperCase()}
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Battery</span>
                          <div className="battery-bar">
                            <div 
                              className="battery-level" 
                              style={{
                                width: `${device.battery}%`,
                                backgroundColor: device.battery > 50 ? 'var(--status-online)' : 
                                                device.battery > 20 ? 'var(--status-warning)' : 
                                                'var(--status-offline)'
                              }}
                            ></div>
                          </div>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Speed</span>
                          <span className="detail-value">{device.speed} km/h</span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Direction</span>
                          <span className="detail-value">{device.direction}</span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">Last Seen</span>
                          <span className="detail-value">{device.lastSeen}</span>
                        </div>
                        {/* Add device-specific details */}
                        {device.id === "dev-001" && (
                          <>
                            <div className="detail-item">
                              <span className="detail-label">Fuel Level</span>
                              <span className="detail-value">{device.fuelLevel}%</span>
                            </div>
                            <div className="detail-item">
                              <span className="detail-label">Driver</span>
                              <span className="detail-value">{device.assignedDriver}</span>
                            </div>
                          </>
                        )}
                        {device.id === "dev-002" && (
                          <>
                            <div className="detail-item">
                              <span className="detail-label">Altitude</span>
                              <span className="detail-value">{device.altitude} m</span>
                            </div>
                            <div className="detail-item">
                              <span className="detail-label">Mission Time</span>
                              <span className="detail-value">{device.missionTime} min</span>
                            </div>
                          </>
                        )}
                        {device.id === "dev-003" && (
                          <>
                            <div className="detail-item">
                              <span className="detail-label">Last Transmission</span>
                              <span className="detail-value">{device.lastTransmission}</span>
                            </div>
                            <div className="detail-item">
                              <span className="detail-label">Power Saving</span>
                              <span className="detail-value">{device.powerSavingMode ? "Enabled" : "Disabled"}</span>
                            </div>
                          </>
                        )}
                      </div>
                      <div className="detail-actions">
                        <button className="cyber-button small">
                          <i className="icon message-square"></i>
                          Send Command
                        </button>
                        <button className="cyber-button small">
                          <i className="icon refresh-ccw"></i>
                          Refresh Status
                        </button>
                      </div>
                    </div>
                  );
                })()}
              </div>
            </div>
          )}
        </div>
        
        {/* Main Map Area */}
        <div className="cyber-main">
          <div className="tabs">
            <div className="tabs-header">
              <div className="tab-list">
                <button 
                  className={`tab-button ${activeTab === "map" ? "active" : ""}`}
                  onClick={() => setActiveTab("map")}
                >
                  <i className="icon map-pin"></i>
                  Map View
                </button>
                <button 
                  className={`tab-button ${activeTab === "layers" ? "active" : ""}`}
                  onClick={() => setActiveTab("layers")}
                >
                  <i className="icon layers"></i>
                  Layers
                </button>
                <button 
                  className={`tab-button ${activeTab === "alerts" ? "active" : ""}`}
                  onClick={() => setActiveTab("alerts")}
                >
                  <i className="icon alert-triangle"></i>
                  Alerts
                </button>
                <button
                  className={`tab-button ${activeTab === "commands" ? "active" : ""}`}
                  onClick={() => setActiveTab("commands")}
                >
                  <i className="icon terminal"></i>
                  Command Center
                </button>
              </div>
              
              <div className="view-controls">
                <button 
                  className={`cyber-button small ${drawingMode ? 'active' : ''}`}
                  onClick={() => startDrawingGeofence()}
                >
                  <i className="icon edit"></i>
                  {drawingMode ? 'Drawing Mode' : 'Draw Geofence'}
                </button>
                <button className="cyber-button small">
                  <i className="icon eye"></i>
                  Live View
                </button>
              </div>
            </div>
            
            <div className="tab-content">
              {activeTab === "map" && (
                <div className="cyber-card map-card">
                  <div id="cyber-map" className="map-container">
                    {/* Map will be loaded here by external library */}
                  </div>
                  
                  <div className="map-controls">
                    <div className="slider-control">
                      <div className="slider-header">
                        <span>Zoom Level</span>
                        <span>1.5x</span>
                      </div>
                      <input type="range" min="1" max="100" value="50" className="cyber-slider" />
                    </div>
                    <div className="layer-toggles">
                      <button 
                        className={`layer-toggle ${mapLayers.heatmap ? 'active' : ''}`}
                        onClick={() => toggleMapLayer('heatmap')}
                      >
                        <i className="icon thermometer"></i>
                        Heat Map
                      </button>
                      <button 
                        className={`layer-toggle ${mapLayers.deviceHistory ? 'active' : ''}`}
                        onClick={() => toggleMapLayer('deviceHistory')}
                      >
                        <i className="icon clock"></i>
                        History
                      </button>
                      <button 
                        className={`layer-toggle ${mapLayers.traffic ? 'active' : ''}`}
                        onClick={() => toggleMapLayer('traffic')}
                      >
                        <i className="icon truck"></i>
                        Traffic
                      </button>
                    </div>
                  </div>
                </div>
              )}
              
              {activeTab === "layers" && (
                <div className="cyber-card">
                  <div className="card-content">
                    <h3 className="feature-title">Layer Management</h3>
                    <div className="layers-grid">
                      {Object.entries(mapLayers).map(([layer, enabled]) => (
                        <div key={layer} className="layer-item">
                          <div className="layer-header">
                            <span className="layer-name">
                              {layer.charAt(0).toUpperCase() + layer.slice(1)} Layer
                            </span>
                            <label className="switch">
                              <input 
                                type="checkbox" 
                                checked={enabled} 
                                onChange={() => toggleMapLayer(layer)} 
                              />
                              <span className="slider"></span>
                            </label>
                          </div>
                          <div className="layer-description">
                            {layer === 'heatmap' && 'Shows activity density on the map'}
                            {layer === 'traffic' && 'Displays traffic conditions'}
                            {layer === 'satellite' && 'Shows satellite imagery'}
                            {layer === 'deviceHistory' && 'Shows device movement history'}
                          </div>
                          <div className="layer-preview" style={{ 
                            backgroundImage: `url(/images/${layer}-preview.jpg)`,
                            opacity: enabled ? 1 : 0.5
                          }}></div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
              
              {activeTab === "alerts" && (
                <div className="cyber-card">
                  <div className="card-content">
                    <h3 className="feature-title">Alert Configuration</h3>
                    {/* We'll add alert configuration here in a future update */}
                  </div>
                </div>
              )}

              {activeTab === "commands" && (
                <div className="cyber-card">
                  <div className="card-content command-center">
                    <h3 className="feature-title">
                      <i className="icon terminal"></i>
                      Command Center
                    </h3>

                    {/* Device selector */}
                    <div className="command-device-selector">
                      <label className="command-label">Target Device</label>
                      <select
                        className="cyber-select"
                        value={commandDevice}
                        onChange={e => setCommandDevice(e.target.value)}
                      >
                        {devices.map(d => (
                          <option key={d.id} value={d.id}>
                            {d.name} ({d.id})
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Command buttons */}
                    <div className="command-buttons">
                      {commandDefs.map(cmd => (
                        <button
                          key={cmd.type}
                          className={`cyber-button command-btn ${cmd.danger ? 'danger' : ''}`}
                          onClick={() => {
                            if (cmd.type === 'SET_SPEED_THRESHOLD') {
                              const val = prompt('Enter speed threshold (km/h):', '120');
                              if (val) {
                                fetch('/api/commands/speed-threshold/' + commandDevice, {
                                  method: 'POST',
                                  headers: { 'Content-Type': 'application/json' },
                                  body: JSON.stringify({ speedThreshold: parseInt(val, 10) })
                                })
                                .then(r => r.json())
                                .then(data => {
                                  if (data.success) {
                                    const entry = {
                                      id: 'CMD-' + Date.now().toString(36).toUpperCase(),
                                      deviceId: commandDevice,
                                      type: cmd.type,
                                      status: 'pending',
                                      message: `Speed limit set to ${val} km/h`,
                                      issuedAt: new Date().toLocaleTimeString()
                                    };
                                    setCommandHistory(prev => [entry, ...prev].slice(0, 50));
                                  }
                                });
                              }
                            } else {
                              sendCommand(cmd.type);
                            }
                          }}
                          disabled={pendingCommands.size >= 3}
                        >
                          <i className={cmd.icon}></i>
                          <span>{cmd.label}</span>
                        </button>
                      ))}
                    </div>

                    {/* Pending spinner */}
                    {pendingCommands.size > 0 && (
                      <div className="pending-indicator">
                        <span className="spinner">⟳</span>
                        Executing command... ({pendingCommands.size} pending)
                      </div>
                    )}

                    {/* Command history log */}
                    <div className="command-history">
                      <div className="command-history-header">
                        <span className="command-label">Command Log</span>
                        {commandHistory.length > 0 && (
                          <button
                            className="cyber-button micro"
                            onClick={() => setCommandHistory([])}
                          >
                            Clear
                          </button>
                        )}
                      </div>
                      <div className="command-log">
                        {commandHistory.length === 0 && (
                          <div className="command-log-empty">
                            No commands issued yet. Select a device and send a command above.
                          </div>
                        )}
                        {commandHistory.map(entry => (
                          <div key={entry.id} className="command-log-entry">
                            <span className={`cmd-status ${getCommandStatusClass(entry.status)}`}>
                              {getCommandStatusIcon(entry.status)}
                            </span>
                            <span className="cmd-id">{entry.id}</span>
                            <span className="cmd-type">{entry.type}</span>
                            <span className="cmd-device">{entry.deviceId}</span>
                            <span className="cmd-message">{entry.message}</span>
                            <span className="cmd-time">{entry.issuedAt}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
      
      {/* Context menu for map interactions */}
      <div id="map-context-menu" className="context-menu">
        <div className="context-menu-item">
          <i className="icon navigation"></i>
          <span>Set as destination</span>
        </div>
        <div className="context-menu-item">
          <i className="icon plus-circle"></i>
          <span>Add geofence here</span>
        </div>
        <div className="context-menu-item">
          <i className="icon flag"></i>
          <span>Add marker</span>
        </div>
        <div className="context-menu-item">
          <i className="icon clipboard"></i>
          <span>Copy coordinates</span>
        </div>
      </div>
    </div>
  );
}; 