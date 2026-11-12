# CloudCost AI – REST API Specification (Milestones 1–8)

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

---

## 7. Real-Time Cost Anomaly Detection Engine (Milestone 7)

### 7.1 Anomaly Queries & KPI Summaries
- `GET /api/anomalies`: List detected cost anomalies with optional filters:
  - `severity`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
  - `status`: `OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `DISMISSED`
  - `provider`: `AWS`, `AZURE`, `GCP`
  - `serviceName`: Case-insensitive substring
  - `startDate`, `endDate`: ISO `YYYY-MM-DD`
  - `page`, `size`: Pagination controls (returns `Page` when `page` is provided, otherwise `List`)
- `GET /api/anomalies/{id}`: Retrieve single anomaly details with root cause and recommended action.
- `GET /api/anomalies/summary`: Aggregate metrics including `totalAnomalies`, `criticalCount`, `highCount`, `openCount`, and `totalExcessCost` (`?currency=USD`).
- `GET /api/anomalies/trends`: Time-series daily anomaly counts and total excess spending (`?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&currency=USD`).

### 7.2 Anomaly Execution & Lifecycle Management
- `POST /api/anomalies/detect`: Execute multi-algorithm statistical scan across workloads and budgets (`?targetDate=YYYY-MM-DD`). Returns `AnomalyDetectionResult` with count of newly flagged anomalies and active duplicates skipped.
- `PATCH /api/anomalies/{id}/status`: Transition workflow lifecycle state (`OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `DISMISSED`) with optional operator `notes`. Automatically persists transition to audit history.
- `GET /api/anomalies/{id}/history`: Retrieve chronological audit log of all status transitions for the anomaly.

### 7.3 Tenant Detection Parameters
- `GET /api/anomalies/config`: Inspect configured statistical parameters (Z-score threshold, moving average window, spike percentage, budget alerts).
- `PUT /api/anomalies/config`: Update statistical parameters and automated background scheduling toggle for the tenant organization.

---

## 8. In-App Notification Subsystem (Milestone 7)

- `GET /api/notifications`: Retrieve in-app alerts for tenant organization (`?unreadOnly=true&limit=20`).
- `GET /api/notifications/unread-count`: Retrieve unread count for real-time header bell badge.
- `PATCH /api/notifications/{id}/read`: Transition single notification to read status.
- `POST /api/notifications/read-all`: Mark all notifications as read for current organization.

---

## 9. Automated Cost Optimization & Rightsizing Engine (Milestone 8)

### 9.1 Optimization Overview & Recommendations
- `GET /api/optimization/overview`: Returns KPI metrics (`totalPotentialMonthlySavings`, `totalPotentialAnnualSavings`, `totalRealizedMonthlySavings`, `totalRealizedAnnualSavings`, priority counts, and status counts).
- `GET /api/optimization/recommendations`: Filterable paginated list of recommendations:
  - `provider`: `AWS`, `AZURE`, `GCP`
  - `type`: `RIGHTSIZING`, `IDLE_RESOURCE`, `COMMITMENT_SAVINGS_PLAN`, `COMMITMENT_RESERVED_INSTANCE`, `MODERNIZATION`
  - `priority`: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`
  - `status`: `NEW`, `REVIEWED`, `APPROVED`, `IMPLEMENTED`, `REJECTED`
  - `minSavings`: Minimum monthly savings filter
  - `search`: Substring filter on resource ID, name, or type
  - `page`, `size`, `direction`: Pagination and sorting parameters (default sorted by `estimatedMonthlySavings` descending)
- `GET /api/optimization/recommendations/{id}`: Detailed recommendation with telemetry enrichment (CPU avg, CPU peak, memory, runtime).

### 9.2 Recommendation Lifecycle & History
- `PATCH /api/optimization/recommendations/{id}/status`: Transition status (`REVIEWED`, `APPROVED`, `IMPLEMENTED`, `REJECTED`) with operator notes and optional `realizedMonthlySavings`.
- `GET /api/optimization/recommendations/{id}/history`: Retrieve full chronological audit log of all status transitions for the recommendation.

### 9.3 Savings & Telemetry Analytics
- `GET /api/optimization/savings-summary`: Aggregated breakdown of monthly savings opportunities across providers, recommendation categories, and resource types.
- `GET /api/optimization/resource-utilization`: Paginated list of monitored resource utilization snapshots with CPU/memory telemetry and status classification (`HEALTHY`, `UNDERUTILIZED`, `IDLE`, `MONITORING_REQUIRED`).
- `GET /api/optimization/commitment-analysis`: Evaluates continuous 30-day baseline compute expenditure and models 1-Year (28% discount) and 3-Year (42% discount) Savings Plans/RIs with explicit assumptions.
- `POST /api/optimization/analyze`: Trigger an on-demand analysis scan across all cloud accounts.
- `GET /api/optimization/export`: Download full recommendations report in CSV format (`cost-optimization-recommendations.csv`).


