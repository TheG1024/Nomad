import React from 'react';
import PropTypes from 'prop-types';
import { ApolloError } from '@apollo/client';

/**
 * Error boundary component specifically for handling GraphQL errors.
 * This component shows different error UI based on network status and error types.
 */
const GraphQLErrorBoundary = ({ 
  error, 
  loading, 
  children, 
  networkStatus,
  refetch,
  onReset,
  fallback
}) => {
  // No error, render children
  if (!error) {
    return children;
  }
  
  // Check if there's a custom fallback
  if (fallback && typeof fallback === 'function') {
    return fallback(error, refetch);
  }

  // Get error details
  const isNetworkError = error.networkError !== undefined;
  const isGraphQLError = error.graphQLErrors && error.graphQLErrors.length > 0;
  const errorMessages = [];
  
  // Extract GraphQL error messages
  if (isGraphQLError) {
    error.graphQLErrors.forEach((gqlError) => {
      const message = gqlError.message || 'Unknown GraphQL error';
      errorMessages.push(message);
    });
  }
  
  // Extract network error message
  if (isNetworkError) {
    errorMessages.push(error.networkError.message || 'Network error occurred');
  }
  
  // If no specific errors found, use generic message
  if (errorMessages.length === 0) {
    errorMessages.push(error.message || 'An unexpected error occurred');
  }
  
  // Handle specific network error scenarios
  let title = 'Error';
  let subtitle = 'Something went wrong while fetching data.';
  
  if (isNetworkError && error.networkError.statusCode === 401) {
    title = 'Authentication Error';
    subtitle = 'Your session has expired. Please log in again.';
  } else if (isNetworkError && error.networkError.statusCode === 403) {
    title = 'Permission Denied';
    subtitle = 'You don\'t have permission to access this resource.';
  } else if (isNetworkError && (error.networkError.statusCode === 0 || !navigator.onLine)) {
    title = 'Network Connection Error';
    subtitle = 'Please check your internet connection and try again.';
  }

  return (
    <div className="graphql-error">
      <div className="graphql-error-content">
        <h3>{title}</h3>
        <p>{subtitle}</p>
        
        <div className="error-details">
          {errorMessages.map((message, index) => (
            <div key={index} className="error-message">
              {message}
            </div>
          ))}
        </div>
        
        <div className="error-actions">
          {refetch && (
            <button 
              onClick={() => refetch()} 
              className="retry-button"
              disabled={loading}
            >
              {loading ? 'Retrying...' : 'Retry'}
            </button>
          )}
          
          {onReset && (
            <button 
              onClick={onReset}
              className="reset-button"
              disabled={loading}
            >
              Reset
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

GraphQLErrorBoundary.propTypes = {
  error: PropTypes.oneOfType([PropTypes.instanceOf(ApolloError), PropTypes.object]),
  loading: PropTypes.bool,
  networkStatus: PropTypes.number,
  refetch: PropTypes.func,
  onReset: PropTypes.func,
  children: PropTypes.node.isRequired,
  fallback: PropTypes.func
};

export default GraphQLErrorBoundary; 