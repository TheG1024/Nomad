# Nomad

A modern, real-time GPS tracking platform that handles streaming location data with elegance and reliability. Built with Spring Boot and WebSocket technology, Nomad provides seamless real-time tracking capabilities with Redis-powered data persistence.

## Architecture Overview

```mermaid
graph TD
    Client[GPS Device/Client] -->|WebSocket| WS[WebSocket Handler]
    WS --> Auth[Authentication]
    Auth --> Processor[Data Processor]
    Processor -->|Simulated insights| AI[AI Services]
    Processor -->|API| REST[REST API]
```

The platform uses a modular Spring Boot architecture with the following components:
- WebSocket server for real-time data streaming and subscriptions
- REST APIs for AI predictions, anomaly detection, and geofence management
- Service layer for simulated device data and predictive insights
- Spring Security for authentication and authorization (default credentials in development)

## Features

- Real-time GPS data streaming via WebSockets
- AI route prediction and anomaly detection endpoints
- Geofence management APIs
- Simulated device data for UI demos
- Basic security for API endpoints
- Error handling and retry mechanisms

## Prerequisites

### Option 1: Running with Docker (Recommended)
- Docker (20.10.x or higher)
- Docker Compose (2.x or higher)
- 2GB RAM minimum
- 10GB free disk space

### Option 2: Running Locally
- Java 11
- Maven 3.6+
- 4GB RAM minimum
- 20GB free disk space

## Project Structure

```
nomad/
├── src/main/
│   ├── java/com/gpstracker/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST endpoints + WebSocket handlers
│   │   ├── model/           # Data models
│   │   └── service/         # Business logic and simulators
│   └── resources/
│       ├── application.properties  # App configuration
│       └── templates/      # HTML templates
├── docker-compose.yml      # Docker configuration
└── pom.xml                # Maven dependencies
```

## Running the Application

### Using Docker (Recommended)

1. Clone the repository:
```bash
git clone https://github.com/yourusername/nomad.git
cd nomad
```

2. Build and start the services:
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

## API Documentation

### WebSocket Endpoints

#### GPS Data Stream
- URL: `ws://localhost:8080/gps`
- Authentication: Device ID (query param or header)
- Message Format: JSON

### REST Endpoints

#### Predict Routes
- Method: GET
- URL: `/api/ai/predict/route`
- Parameters:
  - deviceId (required): Device identifier
  - startTime (optional): Start timestamp (ISO-8601)

#### Detect Anomalies
- Method: POST
- URL: `/api/ai/detect/anomalies`
- Parameters:
  - deviceId (required): Device identifier
- Body: GPS data JSON payload

#### Manage Geofence
- Method: POST
- URL: `/api/gps/geofence`
- Parameters:
  - deviceId (required): Device identifier
  - centerLat (required): Geofence center latitude
  - centerLon (required): Geofence center longitude
  - radius (required): Radius in kilometers

## Security

### Authentication
- WebSocket connections require valid device IDs
- REST API endpoints use Basic Authentication
- Set `NOMAD_ADMIN_USER` and `NOMAD_ADMIN_PASSWORD` environment variables before starting the app
- Custom authentication can be configured in SecurityConfig.java

### Best Practices
1. Change default admin credentials
2. Use HTTPS in production
3. Implement rate limiting
4. Enable audit logging
5. Regular security updates
6. Use strong password policies

### Configuration

The main configuration properties in `application.properties`:

```properties
# Server Configuration
server.port=8080
server.ssl.enabled=false  # Enable in production

# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=  # Set in production

# Security
spring.security.user.name=${NOMAD_ADMIN_USER:}
spring.security.user.password=${NOMAD_ADMIN_PASSWORD:?NOMAD_ADMIN_PASSWORD must be set}

# CORS
cors.allowed-origins=${NOMAD_ALLOWED_ORIGINS:http://localhost:8080,http://127.0.0.1:8080}

# Logging
logging.level.com.gpstracker=INFO

# OpenWeatherMap API Key
openweathermap.api.key=${OPENWEATHERMAP_API_KEY:}
```

## Error Handling

Common errors and solutions:

1. WebSocket Connection Failed
   - Verify device ID is provided
   - Check network connectivity
   - Ensure server is running

2. Redis Connection Issues
   - Verify Redis is running
   - Check connection settings
   - Ensure sufficient memory

3. Export Failures
   - Validate date range
   - Check disk space
   - Verify file permissions

## Production Deployment

### Recommendations

1. Infrastructure
   - Use container orchestration (Kubernetes/ECS)
   - Implement load balancing
   - Set up monitoring and alerting

2. Security
   - Enable HTTPS
   - Use environment variables for secrets
   - Implement proper authentication
   - Set up firewalls

3. Performance
   - Configure JVM memory settings
   - Optimize Redis configuration
   - Enable compression
   - Use CDN for static content

### Monitoring
- JVM metrics
- Redis metrics
- WebSocket connections
- System resources
- Error rates

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement changes
4. Add tests
5. Submit pull request
6. Follow coding standards:
   - Use Java code style
   - Write documentation
   - Include unit tests
   - Follow commit message conventions

## Testing

### WebSocket Testing
```bash
# Install websocat
websocat ws://localhost:8080/gps?deviceId=device123

# Send test data
{"deviceId":"device123","latitude":37.7749,"longitude":-122.4194,"speed":30.5,"heading":180.0,"timestamp":"2023-12-25T10:30:00","additionalInfo":"Test data"}
```

### REST API Testing
```bash
# Export GPS data (Basic auth required)
curl -u admin:admin "http://localhost:8080/api/gps/export?deviceId=device123&startTime=2023-12-25T00:00:00&endTime=2023-12-25T23:59:59" -o export.csv
```

### Load Testing
```bash
# Using k6 for load testing
k6 run load-test.js
```

## Troubleshooting

1. Application Won't Start
   - Check Java version
   - Verify Redis connection
   - Review application logs
   - Check port availability
   - Verify OpenWeatherMap API key is provided

2. Redis Connection Issues
   - Verify Redis server is running
   - Check Redis connection settings in application.properties
   - Ensure Redis server is accessible from the application
   - Check Redis server logs for errors

3. Data Not Streaming
   - Verify WebSocket connection
   - Check device authentication
   - Monitor Redis memory
   - Review network settings

4. Export Issues
   - Validate date range format
   - Check file permissions
   - Verify available disk space
   - Review export service logs

## License

This project is licensed under the MIT License - see the LICENSE file for details.
