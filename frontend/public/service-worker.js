// Service Worker for GPS Tracker PWA
const CACHE_NAME = 'gps-tracker-v1';
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/favicon.ico',
  // CSS and JS files will be added dynamically with precache
];

// Install event - cache static assets
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Caching static assets');
        return cache.addAll(STATIC_ASSETS);
      })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.filter(cacheName => {
          return cacheName !== CACHE_NAME;
        }).map(cacheName => {
          console.log('Deleting old cache:', cacheName);
          return caches.delete(cacheName);
        })
      );
    })
  );
});

// Fetch event - serve from cache or network
self.addEventListener('fetch', (event) => {
  // Skip cross-origin requests
  if (!event.request.url.startsWith(self.location.origin)) {
    return;
  }

  // Skip non-GET requests
  if (event.request.method !== 'GET') {
    return;
  }

  // Handle API requests differently (network-first strategy)
  if (event.request.url.includes('/api/') || event.request.url.includes('/ws')) {
    event.respondWith(
      fetch(event.request)
        .catch(error => {
          console.log('Fetch failed; returning offline data', error);
          return caches.match('/offline.html');
        })
    );
    return;
  }

  // For other requests, use cache-first strategy
  event.respondWith(
    caches.match(event.request)
      .then(cachedResponse => {
        // Return cached response if available
        if (cachedResponse) {
          return cachedResponse;
        }

        // Otherwise fetch from network
        return fetch(event.request)
          .then(response => {
            // Don't cache if not a valid response
            if (!response || response.status !== 200 || response.type !== 'basic') {
              return response;
            }

            // Cache the fetched response
            const responseToCache = response.clone();
            caches.open(CACHE_NAME)
              .then(cache => {
                cache.put(event.request, responseToCache);
              });

            return response;
          })
          .catch(error => {
            // If both cache and network fail, show offline page
            console.log('Network fetch failed', error);
            return caches.match('/offline.html');
          });
      })
  );
});

// Background sync for offline updates
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-device-updates') {
    event.waitUntil(syncDeviceUpdates());
  }
});

// Function to sync device updates when back online
async function syncDeviceUpdates() {
  try {
    const db = await openDB();
    const pendingUpdates = await db.getAll('pendingUpdates');
    
    for (const update of pendingUpdates) {
      const response = await fetch('/api/device/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(update)
      });
      
      if (response.ok) {
        await db.delete('pendingUpdates', update.id);
      }
    }
    
    return Promise.resolve();
  } catch (error) {
    console.error('Error syncing updates:', error);
    return Promise.reject(error);
  }
}

// Helper function to open IndexedDB
function openDB() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('gpsTrackerDB', 1);
    
    request.onupgradeneeded = (event) => {
      const db = event.target.result;
      if (!db.objectStoreNames.contains('pendingUpdates')) {
        db.createObjectStore('pendingUpdates', { keyPath: 'id', autoIncrement: true });
      }
    };
    
    request.onsuccess = (event) => {
      resolve(event.target.result);
    };
    
    request.onerror = (event) => {
      reject('Error opening IndexedDB');
    };
  });
} 