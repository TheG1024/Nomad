import {
  ApolloClient,
  InMemoryCache,
  ApolloLink,
  HttpLink,
  from,
  split
} from '@apollo/client';
import { onError } from '@apollo/client/link/error';
import { RetryLink } from '@apollo/client/link/retry';
import { WebSocketLink } from '@apollo/client/link/ws';
import { getMainDefinition } from '@apollo/client/utilities';
import { persistCache, LocalStorageWrapper } from 'apollo3-cache-persist';
import { offlineLink } from './offlineLink';
import performanceService from '../services/performanceService';

// Global configuration
const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:4000/graphql';
const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:4000/graphql';
const PERSIST_CACHE = true;
const DEFAULT_CACHE_TTL = 30 * 60 * 1000; // 30 minutes

// Set up the Apollo Cache with type policies for proper cache normalization
const cache = new InMemoryCache({
  typePolicies: {
    Query: {
      fields: {
        devices: {
          merge(existing, incoming) {
            return incoming;
          }
        },
        geofences: {
          merge(existing, incoming) {
            return incoming;
          }
        }
      }
    },
    Device: {
      // Use deviceId as unique identifier for cache normalization
      keyFields: ['deviceId'],
      fields: {
        // Configure custom merge functions for nested data
        lastKnownLocation: {
          merge(existing, incoming) {
            return incoming;
          }
        },
        // Specify fields that should be cached with a time-to-live
        batteryLevel: {
          read(batteryLevel, { readField, variables }) {
            // Check if the battery level data is stale
            const timestamp = readField('updatedAt');
            if (timestamp && Date.now() - timestamp > DEFAULT_CACHE_TTL) {
              return undefined; // This will force a refetch
            }
            return batteryLevel;
          }
        }
      }
    },
    Geofence: {
      keyFields: ['geofenceId'],
      fields: {
        // Handle polygon points merging
        polygonPoints: {
          merge(existing, incoming) {
            return incoming;
          }
        }
      }
    }
  }
});

// Create the HTTP Link
const httpLink = new HttpLink({
  uri: API_URL,
  credentials: 'include'
});

// Create the WebSocket Link for subscriptions
const wsLink = new WebSocketLink({
  uri: WS_URL,
  options: {
    reconnect: true,
    connectionParams: {
      // Add authentication as needed
      authToken: localStorage.getItem('token')
    }
  }
});

// Error handling link with enhanced logging and UX improvement
const errorLink = onError(({ graphQLErrors, networkError, operation, forward }) => {
  // Mark the start time for performance monitoring
  const startTime = performanceService.markOperationStart(operation.operationName);

  // Log GraphQL errors
  if (graphQLErrors) {
    graphQLErrors.forEach(({ message, locations, path }) => {
      console.error(
        `[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`
      );
    });
  }

  // Log network errors
  if (networkError) {
    console.error(`[Network error]: ${networkError}`);
    performanceService.recordError('network', operation.operationName);
  }

  // Mark the end time for performance monitoring
  performanceService.markOperationEnd(operation.operationName, startTime);
});

// Retry link for transient errors
const retryLink = new RetryLink({
  delay: {
    initial: 300, // ms
    max: 10000,   // max 10 seconds
    jitter: true  // add randomness to avoid thundering herd
  },
  attempts: {
    max: 5,
    retryIf: (error, operation) => {
      // Only retry on network errors and 5xx server errors
      if (!error.networkError) return false;
      
      const isServerError = error.networkError.statusCode >= 500;
      const isNetworkError = !error.networkError.statusCode;
      
      return isServerError || isNetworkError;
    }
  }
});

// Performance monitoring link
const performanceLink = new ApolloLink((operation, forward) => {
  const startTime = performanceService.markOperationStart(operation.operationName);
  
  return forward(operation).map(response => {
    performanceService.markOperationEnd(operation.operationName, startTime);
    return response;
  });
});

// Split traffic between HTTP and WebSocket based on operation type
const splitLink = split(
  ({ query }) => {
    const definition = getMainDefinition(query);
    return (
      definition.kind === 'OperationDefinition' &&
      definition.operation === 'subscription'
    );
  },
  wsLink,
  httpLink
);

// Combine all links
const link = from([
  performanceLink,
  offlineLink,
  errorLink,
  retryLink,
  splitLink
]);

// Initialize the Apollo Client
const client = new ApolloClient({
  link,
  cache,
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-and-network',
      errorPolicy: 'all',
    },
    query: {
      fetchPolicy: 'cache-first',
      errorPolicy: 'all',
    },
    mutate: {
      errorPolicy: 'all',
    },
  },
  assumeImmutableResults: true,
  connectToDevTools: process.env.NODE_ENV === 'development',
});

// Persist cache if enabled
if (PERSIST_CACHE) {
  // Initialize cache persistence
  persistCache({
    cache,
    storage: new LocalStorageWrapper(window.localStorage),
    maxSize: 2097152, // 2MB max size
    debug: process.env.NODE_ENV === 'development',
  }).catch(error => {
    console.error('Error initializing cache persistence:', error);
  });
}

export default client; 