import React, { Suspense, lazy } from 'react';
import { ApolloProvider } from '@apollo/client';
import { Provider as ReduxProvider } from 'react-redux';
import { BrowserRouter as Router } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import client from './graphql/client';
import store from './redux/store';
import { OfflineProvider } from './contexts/OfflineContext';
import ErrorBoundary from './components/common/ErrorBoundary';
import LoadingSpinner from './components/common/LoadingSpinner';
import OfflineIndicator from './components/common/OfflineIndicator';
import InstallBanner from './components/InstallBanner';
import 'react-toastify/dist/ReactToastify.css';
import './App.css';

// Lazy load the main application component
const CyberTracker = lazy(() => 
  import('./components/CyberTracker')
    .then(module => {
      // Track lazy loading performance
      if (window.performance && window.performance.mark) {
        window.performance.mark('cyberTracker-loaded');
      }
      return module;
    })
);

/**
 * App Component
 * 
 * Main application wrapper that provides:
 * - Apollo Client for GraphQL operations
 * - Redux store for state management
 * - OfflineProvider for offline status and operations
 * - Error handling with ErrorBoundary
 * - Routing with BrowserRouter
 * - PWA installation banner
 * - Offline status indicator
 * - Toast notifications
 */
function App() {
  return (
    <ErrorBoundary>
      <ReduxProvider store={store}>
        <ApolloProvider client={client}>
          <OfflineProvider>
            <Router>
              <div className="app">
                <Suspense fallback={<LoadingSpinner fullscreen size="large" />}>
                  <CyberTracker />
                </Suspense>
                
                {/* PWA Installation Banner */}
                <InstallBanner />
                
                {/* Offline Status Indicator */}
                <OfflineIndicator />
                
                {/* Toast Notifications Container */}
                <ToastContainer
                  position="top-right"
                  autoClose={5000}
                  hideProgressBar={false}
                  newestOnTop
                  closeOnClick
                  rtl={false}
                  pauseOnFocusLoss
                  draggable
                  pauseOnHover
                />
              </div>
            </Router>
          </OfflineProvider>
        </ApolloProvider>
      </ReduxProvider>
    </ErrorBoundary>
  );
}

export default App; 