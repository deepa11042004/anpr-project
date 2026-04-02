# ANPR Access Control System - Handover and Operations Guide

## 1. Product Overview
The ANPR Access Control System is a Spring Boot + React middleware used to:
- Receive ANPR events from camera webhooks.
- Apply business authorization rules against scheduled vehicles.
- Trigger boom gate relay endpoints when authorized.
- Record audit logs and system logs for traceability.
- Provide admin UI for connectivity checks, database switching, migration, and reset.

## 2. Architecture
### 2.1 Backend
- Java 17
- Spring Boot 3.x
- Spring Security (JWT RBAC)
- Spring Data JPA
- H2 (default) with runtime switch capability to MySQL

### 2.2 Frontend
- React (Vite)
- Tailwind CSS
- Axios with JWT interceptor

### 2.3 Runtime Data Stores
- `users` for RBAC
- `audit_logs` for decision traces
- `scheduled_vehicles` for ERP schedule source-of-truth

## 3. Deployment on Linux
### 3.1 Prerequisites
- OpenJDK 17+
- Network access to camera devices and DB host
- Port 8080 open internally

### 3.2 Files to Deploy
- `anpr-access-control-1.0.0.jar`
- Optional: `Database_Schema.csv`
- Optional runtime override path: `config/runtime-db.properties`

### 3.3 Start Command
```bash
java -jar anpr-access-control-1.0.0.jar
```

### 3.4 Stop Command
```bash
pkill -f anpr-access-control-1.0.0.jar
```

### 3.5 Recommended systemd Service
```ini
[Unit]
Description=ANPR Access Control
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/anpr
ExecStart=/usr/bin/java -jar /opt/anpr/anpr-access-control-1.0.0.jar
Restart=always
RestartSec=5
User=anpr

[Install]
WantedBy=multi-user.target
```

## 4. First Login and Access
### 4.1 Default Users
- superadmin / SuperAdmin@123
- admin / Admin@123
- operator / Operator@123

### 4.2 URLs
- App: `http://<host>:8080`
- Swagger: `http://<host>:8080/swagger-ui/index.html`
- H2 Console (default mode only): `http://<host>:8080/h2-console`

## 5. ANPR Webhook Integration
### 5.1 Middleware Webhook URL to Share with Camera Team
`http://<middleware-host>:8080/api/anpr/event`

### 5.2 Required Camera Push Configuration
- Event Type: ANPR
- Method: POST
- Content-Type: application/json
- Payload fields must include plate and confidence

### 5.3 Sample Payload
```json
{
  "ipAddress": "192.168.1.25",
  "channelID": 1,
  "dateTime": "2026-03-31T10:00:00+02:00",
  "eventType": "ANPR",
  "ANPR": {
    "licensePlate": "ABC1234",
    "confidence": 94,
    "country": "ZM",
    "vehicleType": "Truck",
    "direction": "Approach",
    "image": {
      "plateImage": "BASE64",
      "vehicleImage": "BASE64"
    }
  }
}
```

## 6. Boom Gate Integration
### 6.1 Relay Trigger Endpoint (Camera-Side)
`http://<camera-ip>/ISAPI/System/IO/outputs/1/trigger`

### 6.2 Trigger XML Body
```xml
<IOPortData><outputState>high</outputState></IOPortData>
```

### 6.3 What Client Must Provide
- Camera reachable IP/port
- Relay permissions/credentials
- Confirmation of physical wiring from relay output to gate control input

## 7. Connectivity Module Usage (UI)
Open Connectivity tab and perform:
1. Camera connectivity test
2. Verify generated webhook URL
3. Boom gate trigger test

Use this before UAT and before switching live traffic.

## 8. Database Management (Settings Module)
### 8.1 Test Connection
Enter MySQL host/port/schema/user/password and click Test Connection.

### 8.2 Migrate Data
Click Migrate H2 Data to copy:
- users
- scheduled_vehicles
- audit_logs

### 8.3 Apply Runtime Config
Click Apply Config to generate `config/runtime-db.properties`.

### 8.4 Restart Application
Click Restart App (SUPER_ADMIN only).

### 8.5 Reset to Default H2
Click Reset to H2 Default, then restart.
This removes runtime override and returns to packaged default behavior.

## 9. Decision Engine Behavior
The system validates ANPR entries using:
- Confidence threshold
- Direction allow-list
- Status allow-list
- Date checks
- Queue order checks
- Duplicate cooldown checks
- Data quality checks

Decision outcomes are persisted in `audit_logs`.

## 10. System Logs Module
Use System Logs tab to monitor live runtime logs.
- ERROR: red
- WARN: yellow
- INFO: green
- DEBUG: blue

Use filter/search and pause/resume controls for troubleshooting.

## 11. Security Model
- JWT-based auth
- Roles: SUPER_ADMIN, ADMIN, OPERATOR
- Admin modules protected with role checks
- Rotate default credentials on first deployment
- Restrict access by network/firewall policy

## 12. Operational Checklists
### 12.1 Pre-Go-Live
- Validate DB connectivity
- Migrate and verify row counts
- Validate camera webhook delivery
- Validate gate trigger test
- Confirm system logs and dashboard visibility

### 12.2 UAT Scenarios
- Approved plate opens gate
- Rejected plate stays closed
- Low-confidence rejected
- Queue-order rejection
- Manual override logged

## 13. Backup and Recovery
- Backup external DB before migration
- Keep previous jar and config for rollback
- If issues after external switch: Reset to H2 Default and restart

## 14. Troubleshooting
### 14.1 Dashboard "Failed to load statistics"
- Check active DB config
- Ensure required tables exist
- Verify JWT token and role
- Check System Logs tab for stack traces

### 14.2 Camera Test Fails
- Verify network route, port, credentials
- Confirm camera ISAPI availability
- Try HTTP/HTTPS toggle in Connectivity tab

### 14.3 Gate Trigger Fails
- Verify relay endpoint and camera auth
- Confirm physical relay wiring
- Confirm gate controller accepts pulse/open command

## 15. API Summary
### Public
- `POST /api/auth/login`
- `POST /api/anpr/event`

### Protected
- `GET /api/auth/validate`
- Dashboard, Logs, Gate Override
- Connectivity APIs
- Settings APIs
- System Logs API

## 16. Handover Notes for Client Development Team
- This middleware is runtime-configurable and can switch from H2 to MySQL without rebuilding.
- Use the in-app Settings and Connectivity modules for environment onboarding.
- Keep this document with your internal runbook and update when infrastructure changes.
