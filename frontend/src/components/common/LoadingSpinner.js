import React from 'react';
import PropTypes from 'prop-types';

/**
 * Loading spinner component that can be displayed at different sizes
 * and optionally as a full-screen overlay.
 * 
 * @param {Object} props - Component props
 * @param {string} props.size - Size of the spinner ('small', 'medium', 'large')
 * @param {boolean} props.fullScreen - Whether to display as a full-screen overlay
 * @param {string} props.color - Color of the spinner ('primary', 'secondary', 'white')
 * @param {string} props.message - Optional message to display below the spinner
 */
const LoadingSpinner = ({ size = 'medium', fullScreen = false, color = 'primary', message }) => {
  // Size mapping
  const sizeMap = {
    small: { spinner: 20, track: 2 },
    medium: { spinner: 32, track: 3 },
    large: { spinner: 48, track: 4 }
  };
  
  // Color mapping
  const colorMap = {
    primary: '#007bff',
    secondary: '#6c757d',
    white: '#ffffff'
  };
  
  const { spinner, track } = sizeMap[size] || sizeMap.medium;
  const spinnerColor = colorMap[color] || colorMap.primary;
  
  // Styles
  const spinnerStyle = {
    width: `${spinner}px`,
    height: `${spinner}px`,
    border: `${track}px solid rgba(0, 0, 0, 0.1)`,
    borderTopColor: spinnerColor,
    borderRadius: '50%',
    animation: 'spin 1s linear infinite'
  };
  
  const containerStyle = fullScreen
    ? {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.8)',
        zIndex: 9999
      }
    : {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center'
      };
  
  return (
    <div className="loading-spinner-container" style={containerStyle} aria-live="polite" role="status">
      <style jsx global>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
      <div className="loading-spinner" style={spinnerStyle}></div>
      {message && <p className="loading-message" style={{ marginTop: '10px', color: fullScreen ? '#333' : 'inherit' }}>{message}</p>}
      <span className="sr-only">Loading...</span>
    </div>
  );
};

LoadingSpinner.propTypes = {
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  fullScreen: PropTypes.bool,
  color: PropTypes.oneOf(['primary', 'secondary', 'white']),
  message: PropTypes.string
};

export default LoadingSpinner; 