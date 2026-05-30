// Nomad Cyber Terminal - Map Initialization

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
    window.cyberMap = L.map('cyber-map', { zoomControl: false }).setView([40.7128, -74.0060], 12);
    L.control.zoom({ position: 'bottomright' }).addTo(window.cyberMap);
    
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; OpenStreetMap',
      crossOrigin: true
    }).addTo(window.cyberMap);
    
    policeAlertsLayer = L.layerGroup().addTo(window.cyberMap);
    
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
    
    if (typeof loadPoliceAlerts === 'function') {
      loadPoliceAlerts();
    }
    
    // Defer WebSocket connection to improve page load
    setTimeout(connectWebSocket, 1500);
    
    console.log('Map initialized successfully!');
  } catch(e) {
    console.error('Map initialization failed:', e);
  }
}

// Run when DOM is ready
if (document.readyState === 'complete' || document.readyState !== 'loading') {
  initMap();
} else {
  document.addEventListener('DOMContentLoaded', initMap);
}