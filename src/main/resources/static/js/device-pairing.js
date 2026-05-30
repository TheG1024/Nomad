// Device Pairing Modal - Auto-shows on first visit
let hasRegisteredDevice = false;
let currentDeviceId = null;
let currentApiKey = null;

function showPairingModal() {
  // Check if already registered in this session
  if (sessionStorage.getItem('deviceRegistered')) {
    console.log('Device already registered in this session');
    return;
  }
  
  const modal = document.createElement('div');
  modal.id = 'pairing-modal';
  modal.style.cssText = `
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 999999;
    animation: slideDown 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  `;
  
  modal.innerHTML = `
    <div style="
      background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
      border: 2px solid #00ff88;
      border-radius: 16px;
      padding: 30px;
      max-width: 500px;
      width: 90%;
      color: #fff;
      box-shadow: 0 4px 40px rgba(0,255,136,0.4), 0 0 80px rgba(0,0,0,0.8);
      font-family: 'JetBrains Mono', monospace;
      position: relative;
    ">
      <!-- Close button -->
      <button onclick="closePairingModal()" style="
        position: absolute;
        top: 15px;
        right: 15px;
        background: transparent;
        border: none;
        color: #888;
        font-size: 24px;
        cursor: pointer;
        transition: all 0.2s;
      " onmouseover="this.style.color='#ff4444'; this.style.transform='rotate(90deg)'" onmouseout="this.style.color='#888'; this.style.transform='rotate(0)'">✕</button>
      
      <!-- Header -->
      <div style="text-align: center; margin-bottom: 25px;">
        <div style="font-size: 48px; margin-bottom: 10px;">📡</div>
        <h2 style="color: #00ff88; font-size: 24px; margin-bottom: 8px;">Pair Your Device</h2>
        <p style="color: #888; font-size: 12px;">Track your phone, car, or any GPS device in real-time</p>
      </div>
      
      <!-- Step 1: Register -->
      <div id="step1" style="margin-bottom: 20px;">
        <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 15px;">
          <div style="
            background: #00ff88;
            color: #000;
            width: 28px;
            height: 28px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            font-size: 14px;
          ">1</div>
          <h3 style="color: #fff; font-size: 16px; margin: 0;">Register Your Device</h3>
        </div>
        
        <input type="text" id="pair-device-name" placeholder="Device Name (e.g., 'My iPhone', 'Car Tracker')" style="
          width: 100%;
          padding: 14px;
          background: rgba(255,255,255,0.05);
          border: 1px solid #444;
          border-radius: 8px;
          color: #fff;
          font-family: 'JetBrains Mono', monospace;
          font-size: 14px;
          margin-bottom: 15px;
          outline: none;
          transition: all 0.2s;
        " onfocus="this.style.borderColor='#00ff88'; this.style.boxShadow='0 0 15px rgba(0,255,136,0.3)'" onblur="this.style.borderColor='#444'; this.style.boxShadow='none'">
        
        <select id="pair-device-type" style="
          width: 100%;
          padding: 14px;
          background: rgba(255,255,255,0.05);
          border: 1px solid #444;
          border-radius: 8px;
          color: #fff;
          font-family: 'JetBrains Mono', monospace;
          font-size: 14px;
          margin-bottom: 20px;
          outline: none;
          transition: all 0.2s;
          cursor: pointer;
        " onfocus="this.style.borderColor='#00ff88'" onblur="this.style.borderColor='#444'">
          <option value="mobile_app">📱 Mobile App (OwnTracks, GPS Logger)</option>
          <option value="hardware_tracker">🔧 Hardware Tracker (TK103, GT06)</option>
          <option value="custom_iot">🤖 Custom IoT (ESP32, Arduino)</option>
          <option value="web_browser">🌐 This Web Browser</option>
          <option value="other">📍 Other</option>
        </select>
        
        <button onclick="registerPairDevice()" style="
          width: 100%;
          padding: 14px;
          background: linear-gradient(135deg, #00ff88 0%, #00cc6a 100%);
          border: none;
          border-radius: 8px;
          color: #000;
          font-family: 'JetBrains Mono', monospace;
          font-size: 15px;
          font-weight: bold;
          cursor: pointer;
          transition: all 0.2s;
          box-shadow: 0 2px 12px rgba(0,255,136,0.4);
        " onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 4px 20px rgba(0,255,136,0.6)'" onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 2px 12px rgba(0,255,136,0.4)'">
          ✨ Register Device
        </button>
      </div>
      
      <!-- Step 2: Success & Location -->
      <div id="step2" style="display: none;">
        <div style="text-align: center; margin-bottom: 20px;">
          <div style="font-size: 48px; margin-bottom: 10px;">✅</div>
          <h3 style="color: #00ff88; font-size: 18px;">Device Registered!</h3>
          <p style="color: #888; font-size: 12px; margin-top: 5px;">Now let's get your location</p>
        </div>
        
        <div style="
          background: rgba(0,255,136,0.1);
          border-left: 3px solid #00ff88;
          padding: 15px;
          border-radius: 6px;
          margin-bottom: 20px;
          font-size: 12px;
          color: #00ff88;
          font-family: monospace;
          word-break: break-all;
        ">
          <div style="color: #888; margin-bottom: 5px;">Device ID:</div>
          <div id="device-id-display" style="margin-bottom: 10px;"></div>
          <div style="color: #888; margin-bottom: 5px;">API Key:</div>
          <div id="api-key-display"></div>
        </div>
        
        <button onclick="getLocationAndFinish()" style="
          width: 100%;
          padding: 14px;
          background: linear-gradient(135deg, #0088ff 0%, #0066cc 100%);
          border: none;
          border-radius: 8px;
          color: #fff;
          font-family: 'JetBrains Mono', monospace;
          font-size: 15px;
          font-weight: bold;
          cursor: pointer;
          transition: all 0.2s;
          box-shadow: 0 2px 12px rgba(0,136,255,0.4);
        " onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 4px 20px rgba(0,136,255,0.6)'" onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 2px 12px rgba(0,136,255,0.4)'">
          📍 Get My Location & Finish
        </button>
        
        <button onclick="closePairingModal()" style="
          width: 100%;
          padding: 12px;
          background: transparent;
          border: 1px solid #444;
          border-radius: 8px;
          color: #888;
          font-family: 'JetBrains Mono', monospace;
          font-size: 13px;
          cursor: pointer;
          transition: all 0.2s;
          margin-top: 10px;
        " onmouseover="this.style.borderColor='#666'; this.style.color='#fff'" onmouseout="this.style.borderColor='#444'; this.style.color='#888'">
          Done (I'll send location later)
        </button>
      </div>
      
      <!-- Loading state -->
      <div id="loading-state" style="display: none; text-align: center; padding: 20px;">
        <div style="font-size: 32px; animation: spin 1s linear infinite;">⚙️</div>
        <p style="color: #888; margin-top: 15px;">Registering your device...</p>
      </div>
    </div>
  `;
  
  // Add backdrop
  const backdrop = document.createElement('div');
  backdrop.id = 'pairing-backdrop';
  backdrop.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0,0,0,0.85);
    backdrop-filter: blur(6px);
    z-index: 999998;
    animation: fadeIn 0.3s ease;
  `;
  backdrop.onclick = closePairingModal;
  
  // Add animations
  const style = document.createElement('style');
  style.textContent = `
    @keyframes slideDown {
      from { transform: translate(-50%, -60%); opacity: 0; }
      to { transform: translate(-50%, -50%); opacity: 1; }
    }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
  `;
  document.head.appendChild(style);
  
  document.body.appendChild(backdrop);
  document.body.appendChild(modal);
}

function closePairingModal() {
  const modal = document.getElementById('pairing-modal');
  const backdrop = document.getElementById('pairing-backdrop');
  if (modal) modal.remove();
  if (backdrop) backdrop.remove();
}

async function registerPairDevice() {
  const name = document.getElementById('pair-device-name').value;
  const type = document.getElementById('pair-device-type').value;
  
  if (!name || name.trim() === '') {
    alert('Please enter a device name');
    return;
  }
  
  // Show loading
  document.getElementById('step1').style.display = 'none';
  document.getElementById('loading-state').style.display = 'block';
  
  try {
    const response = await fetch('/api/devices/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, type })
    });
    
    const data = await response.json();
    
    if (data.success) {
      currentDeviceId = data.deviceId;
      currentApiKey = data.apiKey;
      
      // Show success step
      document.getElementById('loading-state').style.display = 'none';
      document.getElementById('step2').style.display = 'block';
      document.getElementById('device-id-display').textContent = data.deviceId;
      document.getElementById('api-key-display').textContent = data.apiKey;
    } else {
      alert('Registration failed: ' + data.message);
      document.getElementById('step1').style.display = 'block';
      document.getElementById('loading-state').style.display = 'none';
    }
  } catch (error) {
    alert('Registration failed: ' + error.message);
    document.getElementById('step1').style.display = 'block';
    document.getElementById('loading-state').style.display = 'none';
  }
}

async function getLocationAndFinish() {
  if (!navigator.geolocation) {
    alert('Geolocation is not supported by your browser');
    return;
  }
  
  const btn = event.target;
  const originalText = btn.innerHTML;
  btn.innerHTML = '🛰️ Getting location...';
  btn.disabled = true;
  
  navigator.geolocation.getCurrentPosition(async (position) => {
    const { latitude, longitude, accuracy } = position.coords;
    
    try {
      const response = await fetch(`/api/devices/${currentDeviceId}/location?apiKey=${currentApiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          latitude,
          longitude,
          accuracy: accuracy,
          speed: 0
        })
      });
      
      const data = await response.json();
      
      if (data.success) {
        // Mark as registered
        sessionStorage.setItem('deviceRegistered', 'true');
        sessionStorage.setItem('deviceId', currentDeviceId);
        sessionStorage.setItem('apiKey', currentApiKey);
        
        // Show success message
        btn.innerHTML = '✅ Location Sent!';
        btn.style.background = 'linear-gradient(135deg, #00ff88 0%, #00cc6a 100%)';
        
        setTimeout(() => {
          closePairingModal();
          showNotification('Device paired! Your location is now being tracked', 'success');
        }, 1500);
      } else {
        alert('Failed to send location: ' + data.message);
        btn.innerHTML = originalText;
        btn.disabled = false;
      }
    } catch (error) {
      alert('Failed to send location: ' + error.message);
      btn.innerHTML = originalText;
      btn.disabled = false;
    }
  }, (error) => {
    alert('Location error: ' + error.message);
    btn.innerHTML = originalText;
    btn.disabled = false;
  }, {
    enableHighAccuracy: true,
    timeout: 10000,
    maximumAge: 0
  });
}

// Auto-show modal when page loads
document.addEventListener('DOMContentLoaded', () => {
  // Auto-show pairing modal on first visit
  setTimeout(() => {
    if (!sessionStorage.getItem('deviceRegistered')) {
      showPairingModal();
    }
  }, 1000); // Wait 1 second after page load
});

// Also expose for manual trigger
window.showPairingModal = showPairingModal;
window.closePairingModal = closePairingModal;