import React, { Component } from 'react';
import PropTypes from 'prop-types';

/**
 * Error Boundary component that catches JavaScript errors in its child component tree,
 * logs those errors, and displays a fallback UI instead of crashing the whole app.
 */
class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { 
      hasError: false,
      error: null,
      errorInfo: null
    };
  }

  /**
   * Update state when an error occurs
   */
  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  /**
   * Catch errors and log details
   */
  componentDidCatch(error, errorInfo) {
    this.setState({
      error: error,
      errorInfo: errorInfo
    });
    
    // Log error to monitoring service
    console.error('Error caught by boundary:', error, errorInfo);
    
    // Could send to error tracking service like Sentry
    // if (window.Sentry) {
    //   window.Sentry.captureException(error);
    // }
  }

  /**
   * Try to recover the application
   */
  handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
  };

  render() {
    const { hasError, error, errorInfo } = this.state;
    const { children, fallback } = this.props;

    if (hasError) {
      // Custom fallback UI if provided
      if (fallback) {
        return typeof fallback === 'function'
          ? fallback(error, errorInfo, this.handleReset)
          : fallback;
      }

      // Default error UI
      return (
        <div className="error-boundary">
          <div className="error-content">
            <h2>Something went wrong</h2>
            <p>We're sorry, but an error occurred while rendering this page.</p>
            
            <div className="error-actions">
              <button 
                onClick={this.handleReset}
                className="retry-button"
              >
                Try Again
              </button>
              
              <button 
                onClick={() => window.location.href = '/'}
                className="home-button"
              >
                Go Home
              </button>
            </div>
            
            {process.env.NODE_ENV !== 'production' && (
              <details className="error-details">
                <summary>Error Details</summary>
                <pre>{error && error.toString()}</pre>
                <pre>{errorInfo && errorInfo.componentStack}</pre>
              </details>
            )}
          </div>
        </div>
      );
    }

    return children;
  }
}

ErrorBoundary.propTypes = {
  children: PropTypes.node.isRequired,
  fallback: PropTypes.oneOfType([
    PropTypes.node,
    PropTypes.func
  ])
};

export default ErrorBoundary; 