// Nomad Cyber Terminal - Enhanced Waze-Style Map with Real-Time Tracking

let userAlertsLayer = null;
let trafficLayer = null;
let driverAvatarsLayer = null;
const USER_API_BASE = '/api/user-alerts';

// Alert icons configuration (Waze-style)
const ALERT_ICONS = {
  POLICE: { emoji: '🚓', color: '#3b82f6', label: 'Police' },
  HAZARD: { emoji: '⚠️', color: '#f59e0b', label: 'Hazard' },
  ACCIDENT: { emoji: '💥', color: '#ef4444', label: 'Accident' },
  TRAFFIC: { emoji: '🚗', color: '#f97316', label: 'Traffic' },
  ROAD_CLOSED: { emoji: '🚧', color: '#dc2626', label: 'Road Closed' },
  CONSTRUCTION: { emoji: '🚧', color: '#f59e0b', label: 'Construction' },
  WEATHER: { emoji: '⛈️', color: '#8b5cf6', label: 'Weather' },
  OTHER: { emoji: '📍', color: '#6b7280', label: 'Other' }
};

// Initialize map with Leaflet
function initMap() {
  if (typeof L === 'undefined') { 
    console.log('Leaflet not loaded, waiting...');
    setTimeout(initMap, 100); 
    return; 
  }
  
  // Check if element exists
  const mapContainer = document.getElementById('cyber-map');
  if (!mapContainer) { 
    console.log('Map container not found, waiting...');
    setTimeout(initMap, 100); 
    return; 
  }
  
  // Prevent double initialization
  if (window.cyberMap && window.cyberMap._init) { 
    console.log('Map already initialized');
    return; 
  }
  
  console.log('Initializing map...');
  
  try {
    window.cyberMap = L.map('cyber-map', { 
      zoomControl: false,
      center: [40.7128, -74.0060],
      zoom: 12
    });
    
    L.control.zoom({ position: 'bottomright' }).addTo(window.cyberMap);
    
    // Dark theme tile layer
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; OpenStreetMap &copy; CARTO',
      crossOrigin: true,
      maxZoom: 19
    }).addTo(window.cyberMap);
    
    // Initialize layers
    policeAlertsLayer = L.layerGroup().addTo(window.cyberMap);
    userAlertsLayer = L.layerGroup().addTo(window.cyberMap);
    trafficLayer = L.layerGroup().addTo(window.cyberMap);
    driverAvatarsLayer = L.layerGroup().addTo(window.cyberMap);
    
    // Custom marker icons
    const createIcon = (color, pulse = false) => {
      return L.divIcon({
        className: 'custom-marker',
        html: `<div style="
          width: 16px;
          height: 16px;
          background: ${color};
          border-radius: 50%;
          box-shadow: 0 0 10px ${color}, 0 0 20px ${color};
          ${pulse ? 'animation: pulse 1s infinite;' : ''}
        "></div>`,
        iconSize: [16, 16],
        iconAnchor: [8, 8]
      });
    };
    
    // Device markers
    L.marker([40.7128, -74.006], { icon: createIcon('#00ff88') }).addTo(window.cyberMap);
    L.marker([34.0522, -118.2437], { icon: createIcon('#ffaa00', true) }).addTo(window.cyberMap);
    L.marker([51.5074, -0.1278], { icon: createIcon('#ff3366') }).addTo(window.cyberMap);
    
    // Geofence polygon
    L.geoJSON({
      type: 'Polygon',
      coordinates: [[
        [40.71, -74.01],
        [40.71, -74.00],
        [40.72, -74.00],
        [40.72, -74.01],
        [40.71, -74.01]
      ]]
    }, {
      style: { color: '#00ffff', fillColor: '#00ffff', fillOpacity: 0.1, weight: 2 }
    }).addTo(window.cyberMap);
    
    // Toggle geofence switches
    document.querySelectorAll('.geofence-toggle').forEach(toggle => {
      toggle.addEventListener('click', () => {
        toggle.classList.toggle('active');
      });
    });
    
    // Device item click
    document.querySelectorAll('.device-item').forEach(item => {
      item.addEventListener('click', () => {
        document.querySelectorAll('.device-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
      });
    });
    
    // Add Waze-style quick-report button
    addQuickReportControl();
    
    // Load user-reported alerts
    loadUserAlerts();
    
    // Load police alerts
    loadPoliceAlerts();
    
    // Connect WebSocket for real-time updates
    connectWebSocket();
    
    // Start live location tracking
    startLiveTracking();
    
    console.log('Map initialized successfully with Waze features!');
  } catch(e) {
    console.error('Map initialization failed:', e);
  }
}

// Add Waze-style quick report button
function addQuickReportControl() {
  const reportControl = L.control({ position: 'topright' });
  reportControl.onAdd = function(map) {
    const div = L.DomUtil.create('div', 'quick-report-control');
    div.innerHTML = `
      <button id="quick-report-btn" onclick="showReportModal()" style="
        background: #ff4444;
        color: white;
        border: none;
        border-radius: 8px;
        padding: 10px 15px;
        font-size: 14px;
        font-weight: bold;
        cursor: pointer;
        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        gap: 8px;
      ">
        <span style="font-size: 18px;">📍</span>
        Report
      </button>
    `;
    return div;
  };
  reportControl.addTo(window.cyberMap);
}

// Show report modal
function showReportModal() {
  const modal = document.createElement('div');
  modal.id = 'report-modal';
  modal.className = 'modal';
  modal.style.cssText = `
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 999999;
  `;
  modal.innerHTML = `
    <div class="modal-content" style="
      background: #1a1a1a;
      border: 2px solid #333;
      border-radius: 12px;
      padding: 20px;
      max-width: 400px;
      margin: 0;
      color: #fff;
      box-shadow: 0 4px 30px rgba(0,255,136,0.3), 0 0 60px rgba(0,0,0,0.8);
      font-family: 'JetBrains Mono', monospace;
    ">
      <h3 style="margin-bottom: 15px; color: #00ff88; font-size: 18px; display: flex; align-items: center; gap: 8px;">
        <span style="font-size: 20px;">📍</span> Report Incident
      </h3>
      
      <div style="margin-bottom: 15px;">
        <label style="display: block; margin-bottom: 5px; color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: 1px;">Type</label>
        <select id="alert-type" style="
          width: 100%;
          padding: 12px;
          background: #2a2a2a;
          border: 1px solid #00ff88;
          border-radius: 6px;
          color: #fff;
          font-family: 'JetBrains Mono', monospace;
          font-size: 14px;
          outline: none;
          transition: all 0.2s;
        " onfocus="this.style.borderColor='#00ff88'; this.style.boxShadow='0 0 15px rgba(0,255,136,0.3)'" onblur="this.style.borderColor='#444'; this.style.boxShadow='none'">
          <option value="POLICE">🚓 Police</option>
          <option value="HAZARD">⚠️ Hazard</option>
          <option value="ACCIDENT">💥 Accident</option>
          <option value="TRAFFIC">🚗 Traffic Jam</option>
          <option value="ROAD_CLOSED">🚧 Road Closed</option>
          <option value="CONSTRUCTION">🏗️ Construction</option>
          <option value="WEATHER">⛈️ Weather Hazard</option>
          <option value="OTHER">📍 Other</option>
        </select>
      </div>
      
      <div style="margin-bottom: 20px;">
        <label style="display: block; margin-bottom: 5px; color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: 1px;">Description</label>
        <input type="text" id="alert-description" placeholder="Brief description..." style="
          width: 100%;
          padding: 12px;
          background: #2a2a2a;
          border: 1px solid #444;
          border-radius: 6px;
          color: #fff;
          font-family: 'JetBrains Mono', monospace;
          font-size: 14px;
          outline: none;
          transition: all 0.2s;
        " onfocus="this.style.borderColor='#00ff88'; this.style.boxShadow='0 0 15px rgba(0,255,136,0.3)'" onblur="this.style.borderColor='#444'; this.style.boxShadow='none'">
      </div>
      
      <div style="display: flex; gap: 10px; justify-content: flex-end;">
        <button onclick="closeReportModal()" style="
          padding: 10px 20px;
          background: #333;
          border: 1px solid #555;
          border-radius: 6px;
          color: #fff;
          font-family: 'JetBrains Mono', monospace;
          font-size: 14px;
          font-weight: bold;
          cursor: pointer;
          transition: all 0.2s;
        " onmouseover="this.style.background='#444'" onmouseout="this.style.background='#333'">Cancel</button>
        <button onclick="submitAlert()" style="
          padding: 10px 20px;
          background: linear-gradient(135deg, #00ff88 0%, #00cc6a 100%);
          border: none;
          border-radius: 6px;
          color: #000;
          font-family: 'JetBrains Mono', monospace;
          font-size: 14px;
          font-weight: bold;
          cursor: pointer;
          transition: all 0.2s;
          box-shadow: 0 2px 10px rgba(0,255,136,0.3);
        " onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 4px 15px rgba(0,255,136,0.5)'" onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 2px 10px rgba(0,255,136,0.3)'">✨ Submit Report</button>
      </div>
    </div>
  `;
  
  // Add modal backdrop with higher z-index
  const backdrop = document.createElement('div');
  backdrop.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0,0,0,0.8);
    backdrop-filter: blur(4px);
    z-index: 999998;
    animation: fadeIn 0.2s ease;
  `;
  backdrop.onclick = closeReportModal;
  
  // Add animation keyframes
  if (!document.getElementById('modal-styles')) {
    const style = document.createElement('style');
    style.id = 'modal-styles';
    style.textContent = `
      @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
      @keyframes slideDown { from { transform: translate(-50%, -60%); opacity: 0; } to { transform: translate(-50%, -50%); opacity: 1; } }
    `;
    document.head.appendChild(style);
  }
  
  document.body.appendChild(backdrop);
  document.body.appendChild(modal);
}

// Close report modal
function closeReportModal() {
  const modal = document.getElementById('report-modal');
  const backdrop = modal?.previousElementSibling;
  modal?.remove();
  backdrop?.remove();
}

// Submit user alert
async function submitAlert() {
  const type = document.getElementById('alert-type').value;
  const description = document.getElementById('alert-description').value;
  
  // Get current map center as location
  const center = window.cyberMap.getCenter();
  
  const alert = {
    type: type,
    subtype: type === 'POLICE' ? 'POLICE_VISIBLE' : `HAZARD_ON_ROAD`,
    latitude: center.lat,
    longitude: center.lng,
    description: description || `${ALERT_ICONS[type]?.label || 'Incident'} reported`,
    reliability: 5,
    confidence: 5
  };
  
  try {
    const response = await fetch(USER_API_BASE + '/report', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(alert)
    });
    
    const result = await response.json();
    
    if (result.success) {
      showAlertOnMap(result.alert);
      closeReportModal();
      showNotification('✅ Alert reported successfully!', 'success');
    } else {
      showNotification('❌ Failed to report: ' + result.message, 'error');
    }
  } catch (error) {
    console.error('Error submitting alert:', error);
    showNotification('❌ Network error - try again', 'error');
  }
}

// Show notification toast
function showNotification(message, type = 'info') {
  const toast = document.createElement('div');
  toast.style.cssText = `
    position: fixed;
    top: 20px;
    left: 50%;
    transform: translateX(-50%) translateY(-20px);
    background: ${type === 'success' ? 'linear-gradient(135deg, #00ff88 0%, #00cc6a 100%)' : type === 'error' ? 'linear-gradient(135deg, #ff4444 0%, #cc0000 100%)' : 'linear-gradient(135deg, #0088ff 0%, #0066cc 100%)'};
    color: ${type === 'success' ? '#000' : '#fff'};
    padding: 14px 28px;
    border-radius: 8px;
    font-weight: bold;
    font-family: 'JetBrains Mono', monospace;
    font-size: 14px;
    z-index: 1000000;
    box-shadow: 0 4px 20px rgba(0,0,0,0.4), 0 0 30px ${type === 'success' ? 'rgba(0,255,136,0.4)' : type === 'error' ? 'rgba(255,68,68,0.4)' : 'rgba(0,136,255,0.4)'};
    animation: slideDownToast 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55) forwards;
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 280px;
    justify-content: center;
  `;
  
  const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
  toast.innerHTML = `<span style="font-size: 18px;">${icon}</span> <span>${message}</span>`;
  
  document.body.appendChild(toast);
  
  // Add animation
  const style = document.createElement('style');
  style.textContent = `
    @keyframes slideDownToast {
      to {
        transform: translateX(-50%) translateY(0);
      }
    }
  `;
  document.head.appendChild(style);
  
  setTimeout(() => {
    toast.style.animation = 'fadeIn 0.3s ease reverse forwards';
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

// Load user-reported alerts from API
async function loadUserAlerts() {
  try {
    const bounds = window.cyberMap.getBounds();
    const response = await fetch(`${USER_API_BASE}/area?north=${bounds.getNorth()}&south=${bounds.getSouth()}&east=${bounds.getEast()}&west=${bounds.getWest()}`);
    const result = await response.json();
    
    if (result.success) {
      result.alerts.forEach(alert => showAlertOnMap(alert));
    }
  } catch (error) {
    console.error('Error loading user alerts:', error);
  }
}

// Show alert on map
function showAlertOnMap(alert) {
  const iconConfig = ALERT_ICONS[alert.type] || ALERT_ICONS.OTHER;
  
  const marker = L.marker([alert.latitude, alert.longitude], {
    icon: L.divIcon({
      className: 'user-alert-marker',
      html: `<div style="
        font-size: 28px;
        filter: drop-shadow(0 2px 6px rgba(0,0,0,0.6));
        transition: transform 0.2s;
        animation: bounceIn 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
      ">${iconConfig.emoji}</div>`,
      iconSize: [36, 36],
      iconAnchor: [18, 18]
    })
  }).addTo(userAlertsLayer);
  
  marker.bindPopup(`
    <div style="
      min-width: 180px;
      font-family: 'JetBrains Mono', monospace;
      background: #1a1a1a;
      color: #fff;
      border-radius: 8px;
      overflow: hidden;
    ">
      <div style="
        background: ${iconConfig.color};
        color: #000;
        padding: 10px 12px;
        font-weight: bold;
        font-size: 14px;
        display: flex;
        align-items: center;
        gap: 8px;
      ">
        <span style="font-size: 18px;">${iconConfig.emoji}</span>
        ${iconConfig.label}
      </div>
      <div style="padding: 12px;">
        <div style="color: #888; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 6px;">Description</div>
        <div style="margin-bottom: 12px; color: #fff; font-size: 13px;">${alert.description || 'No description'}</div>
        <div style="color: #888; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 6px;">Reported</div>
        <div style="color: #fff; font-size: 13px; margin-bottom: 12px;">${new Date(alert.reportedAt).toLocaleString()}</div>
        <div style="color: #888; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 6px;">Community Score</div>
        <div style="font-size: 16px; font-weight: bold; color: ${iconConfig.color}; margin-bottom: 12px;">
          ${Math.round(alert.getScore())} pts
        </div>
        <div style="display: flex; gap: 8px;">
          <button onclick="upvoteAlert('${alert.id}')" style="
            flex: 1;
            background: linear-gradient(135deg, #00ff88 0%, #00cc6a 100%);
            border: none;
            padding: 8px 12px;
            border-radius: 6px;
            cursor: pointer;
            font-size: 13px;
            font-weight: bold;
            color: #000;
            transition: all 0.2s;
            box-shadow: 0 2px 8px rgba(0,255,136,0.3);
          " onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 4px 12px rgba(0,255,136,0.5)'" onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 2px 8px rgba(0,255,136,0.3)'">
            👍 <span id="upvote-${alert.id}">${alert.upvotes || 0}</span>
          </button>
          <button onclick="downvoteAlert('${alert.id}')" style="
            flex: 1;
            background: linear-gradient(135deg, #ff4444 0%, #cc0000 100%);
            border: none;
            padding: 8px 12px;
            border-radius: 6px;
            cursor: pointer;
            font-size: 13px;
            font-weight: bold;
            color: #fff;
            transition: all 0.2s;
            box-shadow: 0 2px 8px rgba(255,68,68,0.3);
          " onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 4px 12px rgba(255,68,68,0.5)'" onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 2px 8px rgba(255,68,68,0.3)'">
            👎 <span id="downvote-${alert.id}">${alert.downvotes || 0}</span>
          </button>
        </div>
      </div>
    </div>
  `);
}

// Upvote alert
async function upvoteAlert(alertId) {
  try {
    const response = await fetch(`${USER_API_BASE}/${alertId}/upvote`, { method: 'POST' });
    const result = await response.json();
    if (result.success) {
      showNotification('✅ Alert confirmed!', 'success');
      // Refresh alerts
      userAlertsLayer.clearLayers();
      loadUserAlerts();
    }
  } catch (error) {
    console.error('Error upvoting:', error);
  }
}

// Downvote alert
async function downvoteAlert(alertId) {
  try {
    const response = await fetch(`${USER_API_BASE}/${alertId}/downvote`, { method: 'POST' });
    const result = await response.json();
    if (result.success) {
      showNotification('🗑️ Alert removed', 'info');
      // Refresh alerts
      userAlertsLayer.clearLayers();
      loadUserAlerts();
    }
  } catch (error) {
    console.error('Error downvoting:', error);
  }
}

// Load police alerts
function loadPoliceAlerts() {
  fetch('/api/police-alerts/active', { headers: { 'Authorization': 'Basic ' + btoa('admin:admin') } })
    .then(r => r.json())
    .then(alerts => {
      document.getElementById('alert-count').textContent = alerts.length || 0;
      alerts.forEach(addPoliceAlertToMap);
    })
    .catch(e => console.error('Failed to load police alerts:', e));
}

// Add police alert to map
function addPoliceAlertToMap(alert) {
  if (!window.cyberMap || !policeAlertsLayer) return;
  
  const color = { 
    'LOW': '#00ff88', 
    'MEDIUM': '#ffaa00', 
    'HIGH': '#ff6600', 
    'CRITICAL': '#ff3366' 
  }[alert.severity] || '#ff3366';
  
  // Alert circle
  L.circle([alert.latitude, alert.longitude], {
    color: color, fillColor: color, fillOpacity: 0.2, radius: alert.radius || 500
  }).addTo(policeAlertsLayer);
  
  // Alert marker with pulse
  const alertIcon = L.divIcon({
    className: 'alert-marker',
    html: `<div style="
      width:16px;
      height:16px;
      background:${color};
      border:2px solid #fff;
      border-radius:50%;
      box-shadow:0 0 10px ${color};
      animation: pulse 1s infinite;
    "></div>`,
    iconSize: [16, 16], 
    iconAnchor: [8, 8]
  });
  
  L.marker([alert.latitude, alert.longitude], { icon: alertIcon })
    .addTo(policeAlertsLayer)
    .bindPopup(`<b style="color:${color}">${alert.name}</b><br>
      ${alert.alertType} | ${alert.severity}<br>
      ${alert.description}`);
}

// WebSocket connection
function connectWebSocket() {
  try {
    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, (frame) => {
      const statusDot = document.querySelector('.status-dot');
      if (statusDot) statusDot.style.background = '#00ff88';
      const statusText = document.querySelector('.status-dot')?.nextElementSibling;
      if (statusText) statusText.textContent = 'SYSTEMS ONLINE';
      
      // Subscribe to user alerts
      stompClient.subscribe('/topic/user-alerts', (msg) => {
        try {
          const data = JSON.parse(msg.body);
          if (data.action === 'removed') {
            // Remove alert from map
            userAlertsLayer.eachLayer(layer => {
              if (layer.getPopup() && layer.getPopup().getContent().includes(data.alertId)) {
                userAlertsLayer.removeLayer(layer);
              }
            });
          } else {
            showAlertOnMap(data);
          }
        } catch(e) {
          console.error('Error processing user alert:', e);
        }
      });
      
      // Subscribe to police alerts
      stompClient.subscribe('/topic/police-alerts', (msg) => {
        try {
          addPoliceAlertToMap(JSON.parse(msg.body));
          const countEl = document.getElementById('alert-count');
          if (countEl) countEl.textContent = parseInt(countEl.textContent) + 1;
        } catch(e) {
          console.error('Error processing police alert:', e);
        }
      });
    }, (error) => {
      console.warn('WebSocket connection failed (expected if no client connected):', error);
    });
  } catch(e) {
    console.warn('WebSocket error:', e);
  }
}

// Start live GPS tracking simulation
function startLiveTracking() {
  let position = [40.7128, -74.0060]; // Start in NYC
  let heading = 45; // Northeast
  
  // Add driver avatar
  const driverAvatar = L.divIcon({
    className: 'driver-avatar',
    html: `<div style="
      font-size: 32px;
      filter: drop-shadow(0 2px 6px rgba(0,0,0,0.6));
      animation: drive 0.5s ease infinite alternate;
    ">🚗</div>`,
    iconSize: [40, 40],
    iconAnchor: [20, 20]
  });
  
  const driverMarker = L.marker(position, { icon: driverAvatar }).addTo(driverAvatarsLayer);
  driverMarker.bindPopup('<b style="color: #00ff88;">Vehicle Alpha</b><br>🚗 Online<br>⚡ 78% Battery');
  
  let updateCount = 0;
  
  setInterval(() => {
    // Simulate movement
    const speed = 0.0008; // ~80m per second
    heading += (Math.random() - 0.5) * 15; // Random turn
    
    position[0] += speed * Math.cos(heading * Math.PI / 180);
    position[1] += speed * Math.sin(heading * Math.PI / 180);
    
    // Update marker position
    driverMarker.setLatLng(position);
    
    // Update popup content
    driverMarker.setPopupContent(`<b style="color: #00ff88;">Vehicle Alpha</b><br>🚗 Online<br>⚡ 78% Battery<br>📍 ${position[0].toFixed(4)}, ${position[1].toFixed(4)}`);
    
    updateCount++;
    if (updateCount % 10 === 0) {
      console.log('Live position update:', position);
    }
  }, 1000);
  
  // Add more driver avatars for demo
  const otherDrivers = [
    { lat: 40.72, lng: -74.01, emoji: '🚙', name: 'Drone Beta' },
    { lat: 40.70, lng: -73.99, emoji: '🛵', name: 'Tracker Gamma' },
    { lat: 40.73, lng: -74.02, emoji: '🚚', name: 'Fleet Alpha-7' }
  ];
  
  otherDrivers.forEach(driver => {
    const avatar = L.divIcon({
      className: 'driver-avatar',
      html: `<div style="
        font-size: 28px;
        filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));
      ">${driver.emoji}</div>`,
      iconSize: [36, 36],
      iconAnchor: [18, 18]
    });
    
    const marker = L.marker([driver.lat, driver.lng], { icon: avatar })
      .addTo(driverAvatarsLayer);
    
    marker.bindPopup(`<b style="color: #ffaa00;">${driver.name}</b><br>${driver.emoji} Active`);
  });
  
  console.log('Live tracking started with driver avatars');
}

// Run when DOM is ready
if (document.readyState === 'complete' || document.readyState !== 'loading') {
  initMap();
} else {
  document.addEventListener('DOMContentLoaded', initMap);
}