# GPS Tracker Frontend

This is the frontend application for the GPS Tracker system, built with modern web technologies and advanced performance optimizations.

## Features

- Modern React application with performant rendering
- Redux state management with Redux Toolkit
- GraphQL API integration with Apollo Client
- Progressive Web App (PWA) capabilities for offline support
- WebSocket real-time communication with reconnection handling
- Jest and React Testing Library for comprehensive testing
- Optimized production builds with code splitting and minification

## Getting Started

### Prerequisites

- Node.js (v14+)
- npm (v6+)

### Installation

```bash
# Install dependencies
npm install
```

### Development

```bash
# Start the development server
npm run dev
```

The development server will run on port 3000 with hot module replacement.

### Testing

The GPS Tracker frontend includes a comprehensive testing setup using Jest and React Testing Library. The tests are organized in the `src/__tests__` directory, mirroring the structure of the source code:

- `src/__tests__/components/`: Tests for React components
- `src/__tests__/redux/`: Tests for Redux slices and store
- `src/__tests__/services/`: Tests for services like WebSocket

### Running Tests

You can run the tests using the following commands:

```bash
# Run all tests
npm test

# Run tests in watch mode (development)
npm run test:watch

# Run tests with coverage report
npm run test:coverage
```

### Test Structure

Tests follow a standard pattern:

1. Import the component/slice/service to test
2. Set up any necessary mocks
3. Define test cases using `describe` and `test` blocks
4. Assert expected behavior

Example component test:

```javascript
import { render, screen, fireEvent } from '@testing-library/react';
import Button from '../../components/common/Button';

describe('Button Component', () => {
  test('renders button with correct text', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  test('calls onClick when clicked', () => {
    const handleClick = jest.fn();
    render(<Button onClick={handleClick}>Click me</Button>);
    
    fireEvent.click(screen.getByText('Click me'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
```

### Mock Files

The testing setup includes mock files for handling non-JavaScript assets:

- `src/__mocks__/fileMock.js`: Mocks file imports
- `src/__mocks__/styleMock.js`: Mocks CSS/style imports

### Test Configuration

Test configuration is defined in `jest.config.js` at the root of the project.

### Building for Production

```bash
# Create production build
npm run build
```

The production build will be output to `../src/main/resources/static` for integration with the Spring Boot backend.

## Architecture

### State Management

The application uses Redux Toolkit for state management, with separate slices for:

- Devices
- Geofences
- Notifications

### GraphQL Integration

Apollo Client is used for GraphQL communication, with WebSocket subscriptions for real-time updates.

### PWA Support

The application includes:

- Service Worker for offline capabilities
- Manifest for installable app experience
- Offline data synchronization
- Background sync for device updates

## Performance Optimizations

- Code splitting for optimized bundle sizes
- Cache optimization with content hashes
- Tree shaking to remove unused code
- Lazy loading for components
- Optimized asset loading 