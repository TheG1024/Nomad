module.exports = {
  // Root directory for tests
  rootDir: '.',
  
  // Specify test files pattern
  testMatch: ['**/__tests__/**/*.test.js'],
  
  // Files to ignore
  testPathIgnorePatterns: ['/node_modules/', '/dist/'],
  
  // Transform files
  transform: {
    '^.+\\.(js|jsx)$': 'babel-jest',
  },
  
  // Module file extensions
  moduleFileExtensions: ['js', 'jsx', 'json'],
  
  // Module name mapper for CSS and file imports
  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': '<rootDir>/src/__mocks__/styleMock.js',
    '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': '<rootDir>/src/__mocks__/fileMock.js',
  },
  
  // Setup files
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.js'],
  
  // Code coverage configuration
  collectCoverage: true,
  coverageThreshold: {
    global: {
      statements: 15,
      branches: 15,
      functions: 15,
      lines: 15,
    },
  },
  
  // Test environment
  testEnvironment: 'jsdom',
  
  // Timing
  verbose: true,
  testTimeout: 10000,
}; 