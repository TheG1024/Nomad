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
  modal.innerHTML = `
    <div class="modal-content" style="
      background: #1a1a1a;
      border: 2px solid #333;
      border-radius: 12px;
      padding: 20px;
      max-width: 400px;
      margin: 100px auto;
      color: #fff;
      box-shadow: 0 4px 20px rgba(0,0,0,0.5);
    ">
      <h3 style="margin-bottom: 15px; color: #00ff88;">📍 Report Incident</h3>
      
      <div style="margin-bottom: 15px;">
        <label style="display: block; margin-bottom: 5px;">Type:</label>
        <select id="alert-type" style="
          width: 100%;
          padding: 8px;
          background: #2a2a2a;
          border: 1px solid #444;
          border-radius: 6px;
          color: #fff;
        ">
          <option value="POLICE">🚓 Police</option>
          <option value="HAZARD">⚠️ Hazard</option>
          <option value="ACCIDENT">💥 Accident</option>
          <option value="TRAFFIC">🚗 Traffic</option>
          <option value="ROAD_CLOSED">🚧 Road Closed</option>
          <option value="CONSTRUCTION">🏗️ Construction</option>
          <option value="WEATHER">⛈️ Weather</option>
        </select>
      </div>
      
      <div style="margin-bottom: 15px;">
        <label style="display: block; margin-bottom: 5px;">Description:</label>
        <input type="text" id="alert-description" placeholder="Brief description..." style="
          width: 100%;
          padding: 8px;
          background: #2a2a2a;
          border: 1px solid #444;
          border-radius: 6px;
          color: #fff;
        ">
      </div>
      
      <div style="display: flex; gap: 10px; justify-content: flex-end;">
        <button onclick="closeReportModal()" style="
          padding: 8px 16px;
          background: #444;
          border: none;
          border-radius: 6px;
          color: #fff;
          cursor: pointer;
        ">Cancel</button>
        <button onclick="submitAlert()" style="
          padding: 8px 16px;
          background: #00ff88;
          border: none;
          border-radius: 6px;
          color: #000;
          font-weight: bold;
          cursor: pointer;
        ">Submit</button>
      </div>
    </div>
  `;
  
  // Add modal backdrop
  const backdrop = document.createElement('div');
  backdrop.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0,0,0,0.7);
    z-index: 10000;
  `;
  backdrop.onclick = closeReportModal;
  
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
    transform: translateX(-50%);
    background: ${type === 'success' ? '#00ff88' : type === 'error' ? '#ff4444' : '#0088ff'};
    color: ${type === 'success' ? '#000' : '#fff'};
    padding: 12px 24px;
    border-radius: 8px;
    font-weight: bold;
    z-index: 10001;
    box-shadow: 0 4px 12px rgba(0,0,0,0.3);
    animation: slideDown 0.3s ease;
  `;
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
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
        font-size: 24px;
        filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));
      ">${iconConfig.emoji}</div>`,
      iconSize: [32, 32],
      iconAnchor: [16, 16]
    })
  }).addTo(userAlertsLayer);
  
  marker.bindPopup(`
    <div style="min-width: 150px;">
      <strong style="color: ${iconConfig.color};">${iconConfig.emoji} ${iconConfig.label}</strong><br>
      ${alert.description || ''}<br>
      <small style="color: #888;">${new Date(alert.reportedAt).toLocaleTimeString()}</small><br>
      <div style="margin-top: 8px;">
        <button onclick="upvoteAlert('${alert.id}')" style="
          background: #00ff88;
          border: none;
          padding: 4px 8px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
        ">👍 ${alert.upvotes || 0}</button>
        <button onclick="downvoteAlert('${alert.id}')" style="
          background: #ff4444;
          border: none;
          padding: 4px 8px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
          margin-left: 4px;
        ">👎 ${alert.downvotes || 0}</button>
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
  
  setInterval(() => {
    // Simulate movement
    const speed = 0.001; // ~100m per second
    heading += (Math.random() - 0.5) * 20; // Random turn +/- 10 degrees
    
    position[0] += speed * Math.cos(heading * Math.PI / 180);
    position[1] += speed * Math.sin(heading * Math.PI / 180);
    
    // Update device marker (you'd replace this with actual device tracking)
    // For now, just log the position
    console.log('Live position:', position);
    
    // WebSocket would broadcast this in production
    // webSocket.send(JSON.stringify({ type: 'location', lat: position[0], lng: position[1] }));
  }, 1000);
}

// Run when DOM is ready
if (document.readyState === 'complete' || document.readyState !== 'loading') {
  initMap();
} else {
  document.addEventListener('DOMContentLoaded', initMap);
}