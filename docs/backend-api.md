# CloudCost AI – REST API Specification (Milestones 1–6)

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

---

## 5. Multi-Cloud Ingestion & Real-Time Billing APIs (Milestone 5)

### 5.1 Cloud Provider Discovery (`/api/cloud-providers`)
- `GET /api/cloud-providers`: List all supported cloud providers (AWS, Azure, GCP) with capabilities, reporting latency, authentication types, and IAM requirements.
- `GET /api/cloud-providers/{provider}/capabilities`: Retrieve capabilities for a specific provider (`AWS`, `AZURE`, `GCP`).
- `POST /api/cloud-providers/{provider}/test-connection`: Validate credentials in sandbox/mock mode or probe live API connectivity without persisting account state.

### 5.2 Cloud Account Credential Integration (`/api/cloud-accounts`)
- `POST /api/cloud-accounts/{id}/connect`: Validate credentials and store them encrypted using AES-256-GCM (`v1`). Returns sanitized `CloudAccountResponse`.
- `POST /api/cloud-accounts/{id}/validate`: Test live IAM/RBAC permissions for an already connected account.
- `POST /api/cloud-accounts/{id}/disconnect`: Purge stored encrypted credentials and revert connection status to `DISCONNECTED`.
- `GET /api/cloud-accounts/{id}/connection-status`: Returns `ConnectionStatusResponse` with `connectionStatus`, `credentialsConfigured`, `keyVersion`, and `syncInProgress`.

### 5.3 Billing Synchronization & Idempotency Pipeline (`/api/billing/sync`)
- `POST /api/billing/sync/{cloudAccountId}`: Trigger manual ingestion for a specific cloud account. Optional query params `startDate` and `endDate` (YYYY-MM-DD). Enforces concurrency locking and returns `SyncHistoryResponse`.
- `GET /api/billing/sync/history`: Retrieve paginated audit trail of all ingestion executions (`?page=0&size=10&cloudAccountId={id}`).
- `GET /api/billing/sync/history/{syncId}`: Retrieve detailed audit information for an individual synchronization run.
- `GET /api/billing/sync/status/{cloudAccountId}`: Check current sync health, progress, and most recent execution summary for an account.

---

## 6. Advanced FinOps Analytics & Cost Forecasting APIs (Milestone 6)

All Milestone 6 endpoints require `Authorization: Bearer <TOKEN>` and strictly scope data to the caller's organization. Cross-tenant queries are blocked.

### 6.1 Executive Overview & Trends (`/api/analytics`)
- `GET /api/analytics/overview`: Holistic FinOps dashboard payload combining summary KPIs, month-over-month trajectory, provider distribution, budget health, and active insights. Optional `?currency=USD`.
- `GET /api/analytics/spending-trends`: Chronological time series. Query params:
  - `granularity`: `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` (default: `MONTHLY`).
  - `startDate`: `YYYY-MM-DD` (inclusive).
  - `endDate`: `YYYY-MM-DD` (inclusive).
  - `currency`: Monetary currency filter (e.g. `USD`).
  - `provider`: Optional provider filter (`AWS`, `AZURE`, `GCP`).

### 6.2 Multi-Cloud Distribution & Breakdowns
- `GET /api/analytics/provider-comparison`: Multi-cloud expenditure distribution, percentages, and record metrics across AWS, Azure, and GCP.
- `GET /api/analytics/service-breakdown`: Ranked expenditure grouped by cloud service with percentage contribution (`?limit=10&startDate=&endDate=&currency=`).
- `GET /api/analytics/top-spending-services`: Quick access to top N highest-spend cloud services (`?limit=5&currency=`).
- `GET /api/analytics/account-breakdown`: Multi-cloud spend broken down by connected cloud account (`?limit=10&startDate=&endDate=&currency=`).
- `GET /api/analytics/top-spending-accounts`: Quick access to top N highest-spend cloud accounts (`?limit=5&currency=`).

### 6.3 Month-over-Month Comparative Analytics
- `GET /api/analytics/month-over-month`: Financial comparison between the current calendar month and previous calendar month. Includes absolute delta, percentage delta, provider breakdown, top moving services, and human-readable explainable observations (`?currency=USD`).

### 6.4 Statistical Cost Forecasting Engine
- `GET /api/analytics/forecast`: Run native Java time-series forecast models.
  - `horizon`: `7D`, `30D`, `90D` (default: `30D`).
  - `method`: `EXPONENTIAL_SMOOTHING` (SES) or `MOVING_AVERAGE` (MA) (default: `EXPONENTIAL_SMOOTHING`).
  - `alpha`: Smoothing factor between `0.1` and `0.9` (default: `0.3`).
  - `windowSize`: Rolling window size for moving average (default: `14`).
  - `currency`: Monetary filter.
  - Returns: Historical series, future predicted points with 95% statistical confidence bounds ($\pm 1.96 \cdot \sigma$), and model limitations.

### 6.5 Budget Utilization & Run-Rate
- `GET /api/analytics/budget-utilization`: Evaluates live spend against configured budgets, burn velocity per day, projected end-of-period spending, and risk classification (`HEALTHY`, `WARNING`, `CRITICAL`, `EXCEEDED`) (`?currency=USD`).

### 6.6 Multi-Dimensional Cost Allocation
- `GET /api/analytics/cost-allocation`: Allocate spend across architectural dimensions:
  - `dimension`: `PROVIDER`, `ACCOUNT`, `SERVICE`, `REGION`, `TAG` (default: `SERVICE`).
  - `startDate`, `endDate`, `currency`.

### 6.7 Actionable FinOps Insights
- `GET /api/analytics/insights`: Evaluates rule-based financial engine against actual billing records to surface prioritized recommendations with financial impact and severity levels (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) (`?currency=USD`).

### 6.8 CSV Exports
- `GET /api/analytics/export/spending-trends`: Stream CSV attachment of trend data points.
- `GET /api/analytics/export/provider-comparison`: Stream CSV of provider distribution.
- `GET /api/analytics/export/service-breakdown`: Stream CSV of service cost breakdown.
- `GET /api/analytics/export/forecast`: Stream CSV of historical actuals and forecast projections with confidence intervals.
- `GET /api/analytics/export/insights`: Stream CSV of prioritized FinOps recommendations.
