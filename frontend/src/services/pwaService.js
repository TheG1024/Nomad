/**
 * Service for managing Progressive Web App features
 */
class PWAService {
  /**
   * Initialize the PWA service
   */
  constructor() {
    this.serviceWorkerRegistration = null;
    this.installPrompt = null;
    
    // Save the beforeinstallprompt event to use later
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      this.installPrompt = e;
      this.dispatchInstallableEvent();
    });
    
    // Listen for service worker updates
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.addEventListener('controllerchange', () => {
        if (this.refreshing) return;
        this.refreshing = true;
        window.location.reload();
      });
    }
  }
  
  /**
   * Register the service worker
   * @returns {Promise} Promise that resolves when the service worker is registered
   */
  registerServiceWorker() {
    if ('serviceWorker' in navigator && process.env.NODE_ENV === 'production') {
      return navigator.serviceWorker.register('/service-worker.js')
        .then(registration => {
          this.serviceWorkerRegistration = registration;
          this.checkForUpdates();
          return registration;
        })
        .catch(error => {
          console.error('Service Worker registration failed:', error);
          throw error;
        });
    }
    return Promise.resolve(null);
  }
  
  /**
   * Check for service worker updates
   */
  checkForUpdates() {
    if (!this.serviceWorkerRegistration) return;
    
    // Check for updates every hour
    setInterval(() => {
      this.serviceWorkerRegistration.update()
        .then(() => {
          console.log('Checked for service worker updates');
        })
        .catch(error => {
          console.error('Error checking for service worker updates:', error);
        });
    }, 3600000); // 1 hour
  }
  
  /**
   * Show a prompt to install the app
   * @returns {Promise} Promise that resolves when the prompt is shown
   */
  showInstallPrompt() {
    if (!this.installPrompt) {
      return Promise.reject(new Error('App cannot be installed right now'));
    }
    
    // Show the install prompt
    return this.installPrompt.prompt()
      .then(choiceResult => {
        // Reset the deferred prompt variable
        this.installPrompt = null;
        return choiceResult.outcome === 'accepted';
      });
  }
  
  /**
   * Check if the app is already installed
   * @returns {Promise<boolean>} Promise that resolves with whether the app is installed
   */
  isAppInstalled() {
    return new Promise(resolve => {
      if (window.matchMedia('(display-mode: standalone)').matches) {
        resolve(true);
      } else {
        resolve(false);
      }
    });
  }
  
  /**
   * Dispatch an event when the app is installable
   */
  dispatchInstallableEvent() {
    const event = new CustomEvent('appinstallable', { 
      detail: { installPrompt: this.installPrompt } 
    });
    window.dispatchEvent(event);
  }
  
  /**
   * Sync pending updates from IndexedDB
   * @returns {Promise} Promise that resolves when the sync is complete
   */
  syncPendingUpdates() {
    if ('serviceWorker' in navigator && 'SyncManager' in window) {
      return navigator.serviceWorker.ready
        .then(registration => {
          return registration.sync.register('sync-device-updates');
        })
        .catch(error => {
          console.error('Background sync failed:', error);
          throw error;
        });
    }
    return Promise.resolve(null);
  }
  
  /**
   * Show a notification
   * @param {Object} options Notification options
   * @returns {Promise} Promise that resolves when the notification is shown
   */
  showNotification(options) {
    const { title, ...rest } = options;
    
    if (!('Notification' in window)) {
      console.warn('Notifications not supported');
      return Promise.resolve(null);
    }
    
    if (Notification.permission === 'granted') {
      return this.sendNotification(title, rest);
    } 
    
    if (Notification.permission !== 'denied') {
      return Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          return this.sendNotification(title, rest);
        }
        return null;
      });
    }
    
    return Promise.resolve(null);
  }
  
  /**
   * Send a notification via service worker
   * @param {string} title Notification title
   * @param {Object} options Notification options
   * @returns {Promise} Promise that resolves when the notification is sent
   */
  sendNotification(title, options) {
    if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
      return navigator.serviceWorker.ready.then(registration => {
        return registration.showNotification(title, options);
      });
    }
    
    // Fallback to standard notification if service worker is not available
    const notification = new Notification(title, options);
    return Promise.resolve(notification);
  }
}

// Export as singleton
const pwaService = new PWAService();
export default pwaService; 