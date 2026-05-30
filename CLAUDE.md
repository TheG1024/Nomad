# Nomad GPS Tracker

Modern GPS tracking platform with AI and social features built on Spring Boot 3.4.1.

## Project Structure

```
Nomad/
├── src/main/java/com/gpstracker/
│   ├── config/       # Spring configuration (Security, Redis, WebSocket)
│   ├── controller/   # REST controllers & WebSocket
│   ├── dto/          # Data Transfer Objects
│   ├── exception/    # Custom exceptions
│   ├── mapper/       # MapStruct mappers
│   ├── model/        # Entity models
│   ├── service/     # Business logic
│   └── websocket/   # WebSocket handlers
├── src/main/resources/
│   ├── static/      # CSS, JS, vendor assets
│   └── templates/   # Thymeleaf templates
└── frontend/        # React frontend (if used)
```

## Key Technologies

- **Spring Boot 3.4.1** with Java 21
- **Spring WebSocket** - Real-time GPS device updates
- **Redis** - Caching and pub/sub (with in-memory fallback)
- **Spring Security** - Basic auth, permitAll for public endpoints
- **Thymeleaf** - Server-side rendering (cyber-terminal.html)
- **Leaflet** - Interactive maps

## Running

```bash
# Development
cd /home/tsugiri/Desktop/Nomad
mvn spring-boot:run

# Access
http://localhost:8080
```

## API Endpoints

| Endpoint | Description | Auth |
|----------|------------|------|
| `/` | Main dashboard | Public |
| `/api/health` | Health check | Public |
| `/api/devices` | Device list | Public |
| `/api/gps/**` | GPS data | Public |
| `/api/geofences/**` | Geofence management | Public |
| `/api/police-alerts/**` | Police alert tracking | Public |
| `/ws` | WebSocket endpoint | Public |

## WebSocket Topics

- `/topic/gps` - Device location updates
- `/topic/geofence` - Geofence alerts
- `/topic/police-alerts` - Police alert broadcasts

## Development Notes

- OpenAPI docs: http://localhost:8080/swagger-ui.html
- Mock devices: dev-001 (NYC), dev-002 (LA)
- Security config: `.authorizeHttpRequests(auth -> auth.requestMatchers("/**").permitAll())`
- Redis fallback: In-memory map when Redis unavailable