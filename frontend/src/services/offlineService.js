/**
 * OfflineService - Manages offline functionality and data synchronization
 * 
 * This service provides:
 * 1. Network status detection and monitoring
 * 2. Queue system for operations that fail while offline
 * 3. Background sync capabilities when connection is restored
 * 4. Local storage management for offline data persistence
 */

class OfflineService {
  constructor() {
    this.isOnline = navigator.onLine;
    this.offlineQueue = [];
    this.listeners = [];
    this.storageKey = 'nomad_offline_queue';
    
    // Initialize
    this._loadQueueFromStorage();
    this._setupEventListeners();
  }

  /**
   * Set up event listeners for online/offline status
   * @private
   */
  _setupEventListeners() {
    window.addEventListener('online', this._handleOnline.bind(this));
    window.addEventListener('offline', this._handleOffline.bind(this));
    
    // Check connection quality periodically
    setInterval(() => this._checkConnectionQuality(), 30000);
  }

  /**
   * Handle transition to online state
   * @private
   */
  _handleOnline = () => {
    console.log('Connection restored. Processing queued operations...');
    this.isOnline = true;
    this._notifyListeners({ type: 'ONLINE' });
    this._processQueue();
  }

  /**
   * Handle transition to offline state
   * @private
   */
  _handleOffline = () => {
    console.log('Connection lost. Operations will be queued.');
    this.isOnline = false;
    this._notifyListeners({ type: 'OFFLINE' });
  }

  /**
   * Check connection quality by performing a small fetch operation
   * @private
   */
  _checkConnectionQuality() {
    if (!navigator.onLine) return;

    // Perform a tiny fetch to check actual connectivity
    fetch('/api/ping', { 
      method: 'GET',
      headers: { 'pragma': 'no-cache', 'cache-control': 'no-cache' }
    })
    .then(response => {
      if (!response.ok) throw new Error('Connection check failed');
      this._notifyListeners({ type: 'CONNECTION_QUALITY', quality: 'good' });
    })
    .catch(err => {
      console.warn('Connection check failed:', err);
      this._notifyListeners({ type: 'CONNECTION_QUALITY', quality: 'poor' });
    });
  }

  /**
   * Load the operation queue from localStorage
   * @private
   */
  _loadQueueFromStorage() {
    try {
      const savedQueue = localStorage.getItem(this.storageKey);
      if (savedQueue) {
        this.offlineQueue = JSON.parse(savedQueue);
        console.log(`Loaded ${this.offlineQueue.length} queued operations from storage`);
      }
    } catch (error) {
      console.error('Failed to load offline queue from storage:', error);
      this.offlineQueue = [];
    }
  }

  /**
   * Save the operation queue to localStorage
   * @private
   */
  _saveQueueToStorage() {
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(this.offlineQueue));
    } catch (error) {
      console.error('Failed to save offline queue to storage:', error);
    }
  }

  /**
   * Process all operations in the queue
   * @private
   */
  async _processQueue() {
    if (!this.isOnline || this.offlineQueue.length === 0) return;

    console.log(`Processing ${this.offlineQueue.length} queued operations`);
    
    // Create a copy of the queue to process
    const queueToProcess = [...this.offlineQueue];
    this.offlineQueue = [];
    this._saveQueueToStorage();
    
    // Try to execute each operation
    for (const operation of queueToProcess) {
      try {
        await this._executeOperation(operation);
        this._notifyListeners({ 
          type: 'OPERATION_PROCESSED', 
          operation,
          success: true 
        });
      } catch (error) {
        console.error('Failed to process queued operation:', error);
        // Add back to queue if still in good shape
        if (this.isOnline && !error.permanent) {
          this.queueOperation(operation);
        }
        this._notifyListeners({ 
          type: 'OPERATION_PROCESSED', 
          operation, 
          success: false,
          error
        });
      }
    }
  }

  /**
   * Execute a specific operation
   * @private
   */
  async _executeOperation(operation) {
    // The implementation depends on the operation type
    switch (operation.type) {
      case 'MUTATION':
        return await this._executeMutation(operation);
      case 'HTTP_REQUEST':
        return await this._executeHttpRequest(operation);
      default:
        throw new Error(`Unknown operation type: ${operation.type}`);
    }
  }

  /**
   * Execute a GraphQL mutation
   * @private
   */
  async _executeMutation(operation) {
    const { client, mutation, variables } = operation.payload;
    return await client.mutate({
      mutation,
      variables,
      context: {
        ...operation.context,
        skipQueue: true // Prevent infinite looping
      }
    });
  }

  /**
   * Execute an HTTP request
   * @private
   */
  async _executeHttpRequest(operation) {
    const { url, options } = operation.payload;
    const response = await fetch(url, {
      ...options,
      headers: {
        ...options.headers,
        'X-Offline-Operation': 'retry'
      }
    });
    
    if (!response.ok) {
      throw new Error(`HTTP request failed with status ${response.status}`);
    }
    
    return await response.json();
  }

  /**
   * Notify all listeners about an event
   * @private
   */
  _notifyListeners(event) {
    this.listeners.forEach(listener => {
      try {
        listener(event);
      } catch (error) {
        console.error('Error in offline service listener:', error);
      }
    });
  }

  /**
   * Queue an operation to be executed when online
   * @public
   */
  queueOperation(operation) {
    // Add timestamp and ID to operation
    const queuedOp = {
      ...operation,
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      timestamp: Date.now()
    };
    
    this.offlineQueue.push(queuedOp);
    this._saveQueueToStorage();
    
    this._notifyListeners({ 
      type: 'OPERATION_QUEUED', 
      operation: queuedOp 
    });
    
    console.log(`Operation queued (${operation.type}). Queue size: ${this.offlineQueue.length}`);
    
    return queuedOp.id;
  }

  /**
   * Add a listener for offline/online events
   * @public
   */
  addListener(listener) {
    if (typeof listener === 'function') {
      this.listeners.push(listener);
    }
  }

  /**
   * Remove a previously added listener
   * @public
   */
  removeListener(listener) {
    const index = this.listeners.indexOf(listener);
    if (index !== -1) {
      this.listeners.splice(index, 1);
    }
  }

  /**
   * Get the current network status
   * @public
   */
  getNetworkStatus() {
    return {
      isOnline: this.isOnline,
      queueSize: this.offlineQueue.length
    };
  }

  /**
   * Clear all queued operations
   * @public
   */
  clearQueue() {
    this.offlineQueue = [];
    this._saveQueueToStorage();
    this._notifyListeners({ type: 'QUEUE_CLEARED' });
  }

  /**
   * Remove a specific operation from the queue by ID
   * @public
   */
  removeFromQueue(operationId) {
    const initialLength = this.offlineQueue.length;
    this.offlineQueue = this.offlineQueue.filter(op => op.id !== operationId);
    
    if (initialLength !== this.offlineQueue.length) {
      this._saveQueueToStorage();
      this._notifyListeners({ 
        type: 'OPERATION_REMOVED', 
        operationId 
      });
      return true;
    }
    
    return false;
  }
}

// Create and export a singleton instance
const offlineService = new OfflineService();
export default offlineService; 