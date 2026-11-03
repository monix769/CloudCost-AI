# CloudCost AI – REST API Documentation

## Base URL
- **Local Development**: `http://localhost:8080`
- **Interactive Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI v3 JSON Spec**: `http://localhost:8080/v3/api-docs`

---

## Initial Endpoints (Milestone 1)

### 1. Root Platform Discovery
- **URL**: `/`
- **Method**: `GET`
- **Authorization**: Public
- **Description**: Returns platform metadata, active version, status, and discovery links for all available primary REST endpoints.
- **Success Status**: `200 OK`

#### Response Body Example:
```json
{
  "success": true,
  "message": "Welcome to CloudCost AI FinOps Platform",
  "timestamp": "2026-11-03T15:00:00.123Z",
  "data": {
    "platform": "CloudCost AI",
    "title": "Intelligent Cloud Cost Monitoring & FinOps Analytics Platform",
    "version": "1.0.0-SNAPSHOT",
    "status": "ONLINE",
    "currentMilestone": "Milestone 1 - Foundation & Architecture",
    "documentationUrl": "/swagger-ui.html",
    "openApiSpecUrl": "/v3/api-docs",
    "endpoints": {
      "root": "GET /",
      "health": "GET /api/health",
      "status": "GET /api/status",
      "swaggerUi": "GET /swagger-ui.html",
      "apiDocs": "GET /v3/api-docs"
    },
    "serverTime": "2026-11-03T15:00:00.123Z"
  }
}
```

---

### 2. Application Health Probe
- **URL**: `/api/health`
- **Method**: `GET`
- **Authorization**: Public
- **Description**: Probes the operational readiness of the Spring Boot application, performs a live `SELECT 1` query against the MySQL database, and measures connection latency and disk space availability.
- **Success Status**: `200 OK` (or `503 SERVICE UNAVAILABLE` if degraded)

#### Response Body Example:
```json
{
  "success": true,
  "message": "System operational",
  "timestamp": "2026-11-03T15:00:00.123Z",
  "data": {
    "status": "UP",
    "database": "CONNECTED",
    "version": "1.0.0-SNAPSHOT",
    "uptimeSeconds": 145,
    "components": {
      "database": {
        "status": "UP",
        "databaseProduct": "MySQL",
        "databaseVersion": "8.0.35",
        "driverName": "MySQL Connector/J",
        "catalog": "cloudcost_ai",
        "pingLatencyMs": 4,
        "connectionValid": true
      },
      "diskSpace": {
        "status": "UP",
        "freeBytes": 128456789012,
        "totalBytes": 512000000000
      }
    },
    "timestamp": "2026-11-03T15:00:00.123Z"
  }
}
```

---

### 3. Detailed Architectural & System Status
- **URL**: `/api/status`
- **Method**: `GET`
- **Authorization**: Public
- **Description**: Provides comprehensive runtime diagnostics, active Spring profiles, JVM heap memory meters, and list of loaded FinOps platform modules.
- **Success Status**: `200 OK`

#### Response Body Example:
```json
{
  "success": true,
  "message": "Status retrieved successfully",
  "timestamp": "2026-11-03T15:00:00.123Z",
  "data": {
    "application": "CloudCost AI - FinOps Analytics Platform",
    "environment": "development",
    "activeProfiles": [
      "dev"
    ],
    "javaVersion": "21.0.6",
    "springBootVersion": "3.3.5",
    "securityMode": "STATELESS_PERMIT_ALL_M1",
    "completedMilestones": [
      "Milestone 1: Complete Project Foundation & Architecture"
    ],
    "activeModules": [
      "REST API Engine (Spring Web)",
      "MySQL Persistence Layer (Spring Data JPA / Hibernate)",
      "Swagger OpenAPI 3 Documentation Engine",
      "Security Architecture Layer (Spring Security 6)",
      "Centralized Exception Handling",
      "Health & Actuator Metrics Monitor",
      "React.js + Vite Client Integration"
    ],
    "memory": {
      "usedMemoryMb": 85,
      "freeMemoryMb": 170,
      "totalMemoryMb": 256,
      "maxMemoryMb": 4096
    },
    "timestamp": "2026-11-03T15:00:00.123Z"
  }
}
```
