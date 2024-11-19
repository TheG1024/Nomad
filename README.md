# GPS Tracker Service

A Spring Boot application that handles real-time GPS data streaming from GPS trackers via WebSocket connections and caches the data using Redis.

## Features

- Real-time GPS data streaming via WebSockets
- Redis caching with 7-day data retention
- CSV export functionality (on-demand and scheduled)
- Device authentication
- Basic security for API endpoints

## Prerequisites

### Option 1: Running with Docker (Recommended)
- Docker
- Docker Compose

### Option 2: Running Locally
- Java 11 or higher
- Maven
- Redis server running on localhost:6379

## Running the Application

### Using Docker (Recommended)

1. Build and start the services:
```bash
docker-compose up --build
```

This will start both the Spring Boot application and Redis server. The application will be available at http://localhost:8080.

To stop the services:
```bash
docker-compose down
```

### Running Locally

1. Start Redis server

2. Build the application:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

## Usage

### WebSocket Connection

Connect to the WebSocket endpoint with your device ID:
```
ws://localhost:8080/gps?deviceId=YOUR_DEVICE_ID
```

Or include the device ID in the header:
```
X-Device-ID: YOUR_DEVICE_ID
```

### Sending GPS Data

Send GPS data in JSON format:
```json
{
    "deviceId": "device123",
    "latitude": 37.7749,
    "longitude": -122.4194,
    "speed": 30.5,
    "heading": 180.0,
    "timestamp": "2023-12-25T10:30:00",
    "additionalInfo": "Test data"
}
```

### Exporting Data

To export GPS data as CSV:
```
GET http://localhost:8080/api/gps/export?deviceId=device123&startTime=2023-12-25T00:00:00&endTime=2023-12-25T23:59:59
```

Basic authentication is required for API access:
- Username: admin
- Password: admin

## Configuration

Main configuration properties in `application.properties`:
- Server port: 8080
- Redis host: localhost (or 'redis' when using Docker)
- Redis port: 6379
- Logging level: DEBUG for com.gpstracker package

## Security

- WebSocket connections require device authentication via device ID
- REST API endpoints are secured with basic authentication
- CSRF is disabled for WebSocket connections

## Testing the Application

### Using curl for WebSocket Testing
```bash
# Install websocat if you haven't already (https://github.com/vi/websocat)
websocat ws://localhost:8080/gps?deviceId=device123
```

Then send a JSON message:
```json
{"deviceId":"device123","latitude":37.7749,"longitude":-122.4194,"speed":30.5,"heading":180.0,"timestamp":"2023-12-25T10:30:00","additionalInfo":"Test data"}
```

### Using curl for REST API Testing
```bash
# Export GPS data (requires basic auth)
curl -u admin:admin "http://localhost:8080/api/gps/export?deviceId=device123&startTime=2023-12-25T00:00:00&endTime=2023-12-25T23:59:59" -o export.csv
