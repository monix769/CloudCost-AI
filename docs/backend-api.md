# CloudCost AI – REST API Specification (Milestone 4)

## Overview & Base Configuration
- **Base URL**: `http://localhost:8080`
- **Interactive Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI 3 JSON Spec**: `http://localhost:8080/v3/api-docs`
- **Authentication Scheme**: HTTP Bearer JWT (`Authorization: Bearer <TOKEN>`)

---

## 1. Authentication APIs

### 1.1 Register User & Tenant
- **URL**: `POST /api/auth/register`
- **Access**: Public
- **Request Body**:
```json
{
  "fullName": "Sarah Connor",
  "email": "sarah.connor@cyberdyne.local",
  "password": "Password@2026",
  "organizationName": "Cyberdyne Systems"
}
```
- **Responses**:
  - `201 Created`: User successfully registered. Returns signed JWT and profile.
  - `400 Bad Request`: Validation failure (weak password, missing fields).
  - `409 Conflict`: Email address already registered.

#### Response Example (`201 Created`):
```json
{
  "success": true,
  "message": "User registered successfully",
  "timestamp": "2026-11-04T15:00:00Z",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresInMs": 86400000,
    "user": {
      "id": 12,
      "email": "sarah.connor@cyberdyne.local",
      "fullName": "Sarah Connor",
      "role": "ROLE_USER",
      "organizationId": 3,
      "organizationName": "Cyberdyne Systems",
      "organizationSlug": "cyberdyne-systems",
      "isActive": true,
      "createdAt": "2026-11-04T15:00:00Z"
    }
  }
}
```

---

### 1.2 User Login
- **URL**: `POST /api/auth/login`
- **Access**: Public
- **Request Body**:
```json
{
  "email": "admin@cloudcostai.local",
  "password": "Admin@1234"
}
```
- **Responses**:
  - `200 OK`: Successful authentication. Returns JWT and user profile.
  - `401 Unauthorized`: Invalid email or password.

---

### 1.3 Current User Profile
- **URL**: `GET /api/auth/me`
- **Access**: `Bearer Token Required`
- **Header**: `Authorization: Bearer <JWT_TOKEN>`
- **Responses**:
  - `200 OK`: Returns authenticated user profile.
  - `401 Unauthorized`: Missing, invalid, expired, or revoked token.

---

### 1.4 User Logout
- **URL**: `POST /api/auth/logout`
- **Access**: `Bearer Token Required`
- **Header**: `Authorization: Bearer <JWT_TOKEN>`
- **Description**: Revokes the active token via server-side denylist until natural expiration.
- **Responses**:
  - `200 OK`: Token successfully revoked.

---

## 2. Administration APIs

### 2.1 List Organization Users
- **URL**: `GET /api/admin/users`
- **Access**: `ROLE_ADMIN Only`
- **Header**: `Authorization: Bearer <ADMIN_JWT_TOKEN>`
- **Responses**:
  - `200 OK`: Returns list of user profiles belonging to the admin's organization.
  - `403 Forbidden`: Caller lacks `ROLE_ADMIN` privilege.

---

## 3. Business REST APIs (Multi-Tenant Scoped)

All requests require `Authorization: Bearer <TOKEN>` and are scoped automatically to the caller's organization.

### 3.1 Cloud Accounts (`/api/cloud-accounts`)
- `GET /api/cloud-accounts`: List all cloud provider accounts for tenant (optional `?provider=AWS|AZURE|GCP`).
- `GET /api/cloud-accounts/{id}`: Get account by ID (IDOR protected: 404 if belonging to another tenant).
- `POST /api/cloud-accounts`: Register new cloud provider connection.
- `PUT /api/cloud-accounts/{id}`: Update cloud account metadata.
- `DELETE /api/cloud-accounts/{id}`: Remove cloud account connection.

### 3.2 Billing Management (`/api/billing`)
- `GET /api/billing`: Paginated billing records (`?page=0&size=20&cloudAccountId=&serviceName=&startDate=&endDate=`).
- `GET /api/billing/{id}`: Get billing record by ID.
- `POST /api/billing`: Create individual billing record.
- `DELETE /api/billing/{id}`: Delete individual billing record.
- `POST /api/billing/upload-csv`: Ingest multipart CSV billing dataset scoped to caller's organization.

### 3.3 Cost Analytics (`/api/costs`)
- `GET /api/costs/summary`: Aggregate spend, record count, and average daily cost.
- `GET /api/costs/by-provider`: Breakdown by cloud provider (AWS, Azure, GCP).
- `GET /api/costs/by-service`: Breakdown by individual cloud service (`?limit=5`).
- `GET /api/costs/trends`: Monthly time-series expenditure trends.

### 3.4 Budget Management (`/api/budgets`)
- `GET /api/budgets`: List budgets with live calculated spend utilization.
- `GET /api/budgets/{id}`: Get budget by ID.
- `POST /api/budgets`: Create budget allocation and alerting threshold.
- `PUT /api/budgets/{id}`: Update budget amount limit or alerts.
- `DELETE /api/budgets/{id}`: Remove budget.

### 3.5 AI Anomaly Management (`/api/anomalies`)
- `GET /api/anomalies`: List detected cost anomalies (`?severity=CRITICAL|HIGH|MEDIUM|LOW&status=OPEN|ACKNOWLEDGED|RESOLVED|IGNORED`).
- `GET /api/anomalies/{id}`: Get anomaly details and root cause summary.
- `PATCH /api/anomalies/{id}/status`: Transition anomaly lifecycle status.

---

## 4. System Discovery & Diagnostics (Public)
- `GET /`: Platform root discovery payload.
- `GET /api/health`: Spring Boot and MySQL database health probe.
- `GET /api/status`: JVM memory, runtime parameters, and active profile metadata.
