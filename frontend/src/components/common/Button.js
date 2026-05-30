import React from 'react';
import PropTypes from 'prop-types';

/**
 * Reusable Button component with various styles.
 * 
 * @param {Object} props - Component props
 * @param {Function} props.onClick - Click handler function
 * @param {React.ReactNode} props.children - Button content
 * @param {boolean} props.disabled - Whether the button is disabled
 * @param {string} props.variant - Button style variant ('primary', 'secondary', 'danger', 'success')
 * @param {string} props.size - Button size ('small', 'medium', 'large')
 * @param {string} props.className - Additional CSS classes
 * @param {string} props.type - Button type attribute
 */
const Button = ({
  onClick,
  children,
  disabled = false,
  variant = 'primary',
  size = 'medium',
  className = '',
  type = 'button',
  ...rest
}) => {
  // Define color schemes for different variants
  const variantStyles = {
    primary: {
      backgroundColor: disabled ? '#cccccc' : '#4a6da7',
      color: 'white',
      border: 'none',
    },
    secondary: {
      backgroundColor: disabled ? '#f5f5f5' : '#e9ecef',
      color: '#444444',
      border: '1px solid #ced4da',
    },
    danger: {
      backgroundColor: disabled ? '#f5c6cb' : '#dc3545',
      color: 'white',
      border: 'none',
    },
    success: {
      backgroundColor: disabled ? '#c3e6cb' : '#28a745',
      color: 'white',
      border: 'none',
    },
  };

  // Define sizes
  const sizeStyles = {
    small: {
      padding: '4px 8px',
      fontSize: '0.875rem',
    },
    medium: {
      padding: '8px 16px',
      fontSize: '1rem',
    },
    large: {
      padding: '12px 24px',
      fontSize: '1.125rem',
    },
  };

  // Combine styles
  const buttonStyle = {
    borderRadius: '4px',
    fontWeight: '500',
    cursor: disabled ? 'not-allowed' : 'pointer',
    transition: 'background-color 0.2s ease-in-out',
    ...variantStyles[variant],
    ...sizeStyles[size],
  };

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`gps-button ${variant} ${size} ${className}`}
      style={buttonStyle}
      {...rest}
    >
      {children}
    </button>
  );
};

Button.propTypes = {
  onClick: PropTypes.func,
  children: PropTypes.node.isRequired,
  disabled: PropTypes.bool,
  variant: PropTypes.oneOf(['primary', 'secondary', 'danger', 'success']),
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  className: PropTypes.string,
  type: PropTypes.string,
};

export default Button; 