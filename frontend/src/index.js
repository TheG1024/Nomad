import React from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { ApolloProvider } from '@apollo/client';
import CyberTracker from './components/CyberTracker';
import NotificationManager from './components/NotificationManager';
import store from './redux/store';
import apolloClient from './graphql/client';
import pwaService from './services/pwaService';
import performanceService from './services/performanceService';
import './styles/main.css';

// Register service worker and start performance monitoring
if (process.env.NODE_ENV === 'production') {
  // Initialize PWA features
  pwaService.registerServiceWorker();
  
  // Listen for app installable event
  window.addEventListener('appinstallable', () => {
    console.log('App can be installed');
    // You can show install button or prompt here
  });
}

// Start performance monitoring
performanceService.startMonitoring();
// Apply performance optimizations once DOM is loaded
window.addEventListener('DOMContentLoaded', () => {
  performanceService.applyOptimizations();
});

// Main application component
const App = () => {
  return (
    <>
      <CyberTracker />
      <NotificationManager />
    </>
  );
};

// Mount the app to the root element with Redux and Apollo providers
const root = createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <Provider store={store}>
      <ApolloProvider client={apolloClient}>
        <App />
      </ApolloProvider>
    </Provider>
  </React.StrictMode>
);

// Handle WebSocket disconnection on page unload
window.addEventListener('beforeunload', () => {
  // Clean up any resources before the page unloads
  if (window.WebSocketService) {
    window.WebSocketService.disconnect();
  }
  
  // Stop performance monitoring
  performanceService.stopMonitoring();
}); 