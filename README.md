# ANPR-Based Vehicle Entry Access Control System

A comprehensive middleware system for Automatic Number Plate Recognition (ANPR) based vehicle access control. This system integrates with ANPR cameras, validates vehicles against an authorization data source, and controls boom gates.

## Features

- **ANPR Integration**: Receives events from ANPR cameras via webhook
- **Authorization Source Integration**: Validates vehicles against scheduled entries from a configurable source
- **Boom Gate Control**: Automatically triggers gate opening for authorized vehicles
- **JWT Authentication**: Secure role-based access control (SUPER_ADMIN, ADMIN, OPERATOR)
- **Real-time Dashboard**: Monitor vehicle entries in real-time
- **Historical Logs**: Search and filter past entry records
- **Manual Override**: Allow operators to manually open gates with logging

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.x
- Spring Security with JWT
- Spring Data JPA
- H2 Database (embedded)

### Frontend
- React 18
- Vite
- Tailwind CSS
- React Router
- Axios

## Project Structure

```
anpr-project/
├── pom.xml
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── tailwind.config.js
│   ├── index.html
│   └── src/
│       ├── main.jsx
│       ├── App.jsx
│       ├── index.css
│       ├── api/
│       │   └── client.js
│       ├── context/
│       │   └── AuthContext.jsx
│       ├── components/
│       │   └── Layout.jsx
│       └── pages/
│           ├── Login.jsx
│           ├── Dashboard.jsx
│           ├── LiveFeed.jsx
│           └── Logs.jsx
└── src/main/
    ├── java/com/anpr/
    │   ├── AnprAccessControlApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   ├── WebConfig.java
    │   │   └── DataInitializer.java
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   ├── AnprController.java
    │   │   ├── DashboardController.java
    │   │   ├── LogsController.java
    │   │   └── GateController.java
    │   ├── dto/
    │   │   ├── AnprEventRequest.java
    │   │   ├── AnprEventResponse.java
    │   │   ├── AuthRequest.java
    │   │   ├── AuthResponse.java
    │   │   ├── DashboardStats.java
    │   │   ├── ManualOverrideRequest.java
    │   │   ├── VehicleSchedule.java
    │   │   └── AuditLogResponse.java
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── Role.java
    │   │   ├── AuditLog.java
    │   │   ├── AuthorizationOutcome.java
    │   │   └── GateAction.java
    │   ├── exception/
    │   │   └── GlobalExceptionHandler.java
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   └── AuditLogRepository.java
    │   ├── security/
    │   │   ├── JwtUtil.java
    │   │   └── JwtAuthenticationFilter.java
    │   └── service/
    │       ├── CustomUserDetailsService.java
    │       ├── AuthService.java
    │       ├── AnprService.java
    │       ├── AuditLogService.java
    │       └── BoomGateTriggerService.java
    └── resources/
        └── application.yml
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Node.js 18+ (will be auto-installed by Maven during build)

### Authorization Source Setup

Configure your external authorization source and ensure it provides the vehicle and schedule fields needed by the middleware.

### Configuration

Update `src/main/resources/application.yml`:

```yaml
anpr:
  min-confidence-score: 85
  erp:
    bootstrap-enabled: true
    reload-on-startup: false
    csv-resource: file:./Database_Schema.csv
  decision:
    allowed-statuses: SECURITY,ACTIVE
    allowed-directions: APPROACH
    allowed-locations:
    max-quantity: 100000
    enforce-queue-order: true
    reopen-cooldown-minutes: 5
  camera:
    username: admin
    password: your-camera-password
```

### Build & Run

```bash
# Build the entire project (includes frontend)
mvn clean package

# Run the application
java -jar target/anpr-access-control-1.0.0.jar
```

### Development Mode

For development with hot-reload:

```bash
# Terminal 1 - Run Spring Boot
mvn spring-boot:run

# Terminal 2 - Run React dev server
cd frontend
npm install
npm run dev
```

Access the application:
- Production: http://localhost:8080
- Development (React): http://localhost:5173

## Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Super Admin | superadmin | SuperAdmin@123 |
| Admin | admin | Admin@123 |
| Operator | operator | Operator@123 |

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login and get JWT token

### ANPR Webhook (No authentication required)
- `POST /api/anpr/event` - Receive ANPR camera events
- `GET /api/anpr/health` - Health check for cameras

### Dashboard
- `GET /api/dashboard/stats` - Get statistics (query param: period=today|week|month|year|all)
- `GET /api/dashboard/live-feed` - Get recent 50 entries
- `GET /api/dashboard/poll` - Poll for new entries since timestamp

### Logs
- `GET /api/logs` - Get paginated audit logs with filters
- `GET /api/logs/{id}` - Get single log entry

### Gate Control
- `POST /api/gate/override` - Manual gate override (requires OPERATOR role or higher)

## ANPR Event Payload

```json
{
  "ipAddress": "192.168.1.100",
  "channelID": 1,
  "dateTime": "2024-03-15T10:30:00",
  "eventType": "ANPR",
  "ANPR": {
    "licensePlate": "ABC1234",
    "confidence": 94,
    "country": "ZM",
    "vehicleType": "SUV",
    "direction": "Approach",
    "image": {
      "plateImage": "base64_encoded_image",
      "vehicleImage": "base64_encoded_image"
    }
  }
}
```

## Camera Relay Trigger

The system sends an HTTP PUT request to open the boom gate:

```
PUT http://<camera-ip>/ISAPI/System/IO/outputs/1/trigger
Content-Type: application/xml

<IOPortData>
  <outputState>high</outputState>
</IOPortData>
```

## H2 Database Console

Access the H2 database console at: http://localhost:8080/h2-console

- JDBC URL: `jdbc:h2:file:./data/anprdb`
- Username: `sa`
- Password: (empty)

## License

This project is proprietary software.
