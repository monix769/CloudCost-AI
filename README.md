# CloudCost AI
### Intelligent Cloud Cost Monitoring & FinOps Analytics Platform

[![Java](https://img.shields.io/badge/Java-21%20LTS-007396?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8.x-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3.4-38B2AC?logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## 1. Project Overview & Objectives
**CloudCost AI** is a professional full-stack FinOps SaaS platform engineered to help enterprises monitor multi-cloud expenditures across **Amazon Web Services (AWS)**, **Google Cloud Platform (GCP)**, and **Microsoft Azure**. The platform tracks granular cloud billing records, enforces budget limits, detects unusual spending anomalies, and provides automated cost-optimization insights.

The platform is designed following industry-standard software engineering best practices, SOLID design principles, clean architecture, and decoupled client-server paradigms suitable for enterprise deployment and technical demonstrations in Full Stack Developer interviews.

---

## 2. Mandatory Technology Stack

- **Backend**:
  - **Language**: Java 21 (LTS)
  - **Framework**: Spring Boot 3.3.5
  - **Modules**: Spring Web, Spring Data JPA, Hibernate ORM, Spring Boot Validation, Spring Security 6, Spring Boot Actuator
  - **Database Driver**: MySQL Connector/J 8.x
  - **Connection Pool**: HikariCP
  - **API Documentation**: Springdoc OpenAPI v2.6.0 (Swagger UI v3)
  - **Testing**: JUnit 5, Mockito, AssertJ, Spring MockMvc
  - **Build Tool**: Apache Maven 3.9+ / Maven Wrapper (`mvnw`)
- **Frontend**:
  - **Language**: JavaScript (ES6+ / JSX)
  - **Library**: React.js 19
  - **Tooling**: Vite 8.x
  - **Styling**: Tailwind CSS 3.4 (with dark mode tokens & glassmorphism)
  - **Routing**: React Router DOM v7
  - **HTTP Client**: Axios (centralized interceptors)
  - **Visualization**: Chart.js & React-Chartjs-2
  - **Icons**: Lucide React
- **Database**:
  - **Engine**: MySQL 8.0+
  - **Schema**: `cloudcost_ai` (InnoDB, UTF-8 MB4)
- **Deployment & DevOps**:
  - **Containers**: Docker & Docker Compose
  - **Frontend Target**: Vercel
  - **Backend Target**: Render (Web Service + Managed MySQL)
  - **Testing**: Postman Collection & Environment

---

## 3. System Architecture

CloudCost AI is organized into a clean 4-tier modular architecture:

```
[ Browser / Client ]
        │  (HTTP / JSON)
        ▼
[ Presentation Tier: React 19 + Vite + Tailwind CSS ]
        │  (Axios Interceptor / BaseURL: http://localhost:8080)
        ▼
[ Security & Gateway Tier: Spring Security 6 ]
   ├── Stateless Session FilterChain
   └── Configured CORS Filter (Origin: http://localhost:5173)
        ▼
[ Application Core Tier: Spring Boot 3 + Java 21 ]
   ├── RootController       -> GET /
   ├── HealthController     -> GET /api/health
   ├── StatusController     -> GET /api/status
   ├── GlobalExceptionHandler (@RestControllerAdvice)
   └── HealthService / StatusService / DatabaseHealthIndicatorService
        ▼
[ Persistence & Data Tier: Spring Data JPA + Hibernate + HikariCP ]
        ▼
[ Relational Database: MySQL 8.0+ (`cloudcost_ai`) ]
```

---

## 4. Folder Structure

```
CloudCost-AI/
├── .gitignore                                # Comprehensive Git ignore rules
├── .env.example                              # Documented environment variables template
├── docker-compose.yml                        # Multi-container orchestration (MySQL, Backend, Frontend)
├── README.md                                 # Complete project documentation
│
├── backend/                                  # Spring Boot 3 Application (Java 21)
│   ├── Dockerfile                            # Multi-stage Dockerfile (Eclipse Temurin 21)
│   ├── mvnw / mvnw.cmd                       # Maven Wrapper executables
│   ├── pom.xml                               # Maven project definition and dependencies
│   └── src/
│       ├── main/
│       │   ├── java/com/cloudcostai/
│       │   │   ├── CloudCostAiApplication.java # Spring Boot entry point
│       │   │   ├── config/                   # WebMvc, CORS, and OpenAPI configurations
│       │   │   ├── controller/               # REST API Controllers (Root, Health, Status)
│       │   │   ├── dto/                      # Data Transfer Objects & ApiResponse envelope
│       │   │   ├── entity/                   # BaseEntity and domain entities
│       │   │   ├── exception/                # GlobalExceptionHandler & custom exceptions
│       │   │   ├── repository/               # BaseRepository and persistence interfaces
│       │   │   ├── security/                 # SecurityConfig & JwtProperties
│       │   │   ├── service/                  # Business logic & health indicators
│       │   │   └── validation/               # Validation annotations & helpers
│       │   └── resources/
│       │       ├── application.properties    # Base environment configuration
│       │       ├── application-dev.properties # Development profile
│       │       └── application-prod.properties# Production profile
│       └── test/java/com/cloudcostai/
│           ├── CloudCostAiApplicationTests.java
│           ├── controller/                   # MockMvc Controller Integration Tests
│           └── service/                      # Mockito Service Unit Tests
│
├── frontend/                                 # React.js Application (Vite)
│   ├── Dockerfile                            # Multi-stage Dockerfile (Node.js + Nginx)
│   ├── nginx.conf                            # Nginx production reverse proxy & SPA fallback
│   ├── package.json                          # Dependencies & build scripts
│   ├── tailwind.config.js                    # Tailwind theme configuration
│   ├── postcss.config.js                     # PostCSS plugins
│   ├── vite.config.js                        # Vite bundler configuration
│   ├── .env / .env.example                   # Frontend environment variables
│   └── src/
│       ├── assets/                           # Static graphics and icons
│       ├── components/                       # Reusable UI components
│       ├── context/                          # SystemContext for reactive health state
│       ├── hooks/                            # Custom hooks (useHealth, useSystemStatus)
│       ├── layouts/                          # Application shell layouts
│       ├── pages/                            # Routed pages (HomePage, HealthPage, StatusPage, DocsPage)
│       ├── services/                         # Centralized Axios client (api.js, systemService.js)
│       ├── utils/                            # Formatting utilities
│       ├── App.jsx                           # Route configuration
│       ├── index.css                         # Tailwind directives & design tokens
│       └── main.jsx                          # React application entry point
│
├── database/                                 # Database scripts & schema documentation
│   ├── schema.sql                            # Complete DDL script for MySQL 8+
│   ├── milestone6_migration.sql              # B-Tree optimization indexes for FinOps analytics
│   ├── milestone7_migration.sql              # Anomaly engine schema, history, configs & notifications
│   └── README.md                             # Database design and migration guide
│
├── sample_data/                              # Mock and seed test datasets
│   ├── seed_data.sql                         # Initial organization and user seed records
│   └── sample_billing_records.json           # Sample cloud spend records (AWS, GCP)
│
├── postman/                                  # API Testing Suite
│   ├── CloudCost AI – API Testing.postman_collection.json # 11 folders, 110+ requests
│   └── CloudCost AI – Local Development.postman_environment.json
│
└── docs/                                     # In-depth architectural & API documents
    ├── architecture.md                       # Comprehensive design specifications
    ├── backend-api.md                        # Complete REST endpoint specifications & contracts
    ├── advanced-analytics.md                 # Milestone 6 FinOps analytics formulas & architecture
    ├── cost-forecasting.md                   # Statistical forecasting models (MA & SES) + 95% CI
    ├── finops-insights.md                    # Rule-based FinOps intelligence engine & severity tiers
    ├── anomaly-detection.md                  # Milestone 7 Real-time statistical anomaly engine & rules
    ├── billing-ingestion.md                  # Multi-cloud ingestion, idempotency & deduplication
    ├── cloud-integrations.md                 # AWS, Azure & GCP credentials and SPI integrations
    ├── security.md                           # AES-256-GCM encryption & JWT RBAC tenant isolation
    └── deployment.md                         # Vercel & Render deployment guide
```

---

## 5. Prerequisites

Before running the application locally, ensure you have:
- **Java JDK 21+** (`java -version`)
- **Apache Maven 3.9+** (`mvn -version`) or use the included `./mvnw`
- **Node.js v18+ & npm v9+** (`node -v && npm -v`)
- **MySQL Server 8.0+** (`mysql -V`) running on port `3306`
- **Docker Desktop** (optional, for containerized execution)

---

## 6. Installation & Quick Start

### 6.1 Database Initialization (MySQL)
Ensure your MySQL server is running, then execute the schema script:

```bash
# Connect to MySQL CLI
mysql -u root -p

# Create database and apply schema
CREATE DATABASE IF NOT EXISTS cloudcost_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;

# Import schema and seed data
mysql -u root -p cloudcost_ai < database/schema.sql
mysql -u root -p cloudcost_ai < sample_data/seed_data.sql
```

### 6.2 Backend Startup (Spring Boot)
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Configure database credentials if different from defaults (`root` / `root`):
   ```bash
   # Set environment variables (or rely on application.properties defaults)
   export DB_URL="jdbc:mysql://localhost:3306/cloudcost_ai?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   export DB_USERNAME="root"
   export DB_PASSWORD="your_password"
   ```
3. Run tests and start the server:
   ```bash
   # Execute unit & integration tests
   ./mvnw test

   # Launch Spring Boot
   ./mvnw spring-boot:run
   ```
4. Verify backend status:
   - Root Discovery: `http://localhost:8080/`
   - Health Probe: `http://localhost:8080/api/health`
   - System Status: `http://localhost:8080/api/status`
   - Interactive Swagger UI: `http://localhost:8080/swagger-ui.html`

### 6.3 Frontend Startup (React + Vite)
1. In a separate terminal, navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Verify or adjust `.env`:
   ```bash
   VITE_API_BASE_URL=http://localhost:8080
   ```
4. Launch development server:
   ```bash
   npm run dev
   ```
5. Open your browser at `http://localhost:5173`.

---

## 7. Automated Verification & Testing

### 7.1 Backend Unit & Integration Tests (JUnit 5 + Mockito)
Execute all backend tests:
```bash
cd backend
./mvnw clean test
```
**Test Results:**
- `CloudCostAiApplicationTests`: Context load verification.
- `RootControllerTest`: MockMvc assertion on `GET /` returning 200 OK, JSON structure, and endpoint links.
- `HealthControllerTest`: MockMvc assertion on `GET /api/health` returning 200 OK and database health status.
- `StatusControllerTest`: MockMvc assertion on `GET /api/status` returning 200 OK, Java version, and active modules.
- `HealthServiceTest`: Mockito unit test validating health calculation under UP and DEGRADED database states.

### 7.2 Frontend Production Build Verification
Verify that the React production bundle compiles without warnings or errors:
```bash
cd frontend
npm run build
```

### 7.3 Postman API Testing
1. Launch **Postman**.
2. Click **Import** and select:
   - `postman/CloudCost AI – API Testing.postman_collection.json`
   - `postman/CloudCost AI – Local Development.postman_environment.json`
3. Select the active environment: **CloudCost AI – Local Development**.
4. Run the collection using the **Postman Collection Runner**.
5. All test scripts will assert:
   - HTTP 200 OK status codes
   - `Content-Type: application/json` headers
   - Standardized `ApiResponse` envelope (`success: true`)
   - Accuracy of health and runtime diagnostics data

---

## 8. REST API Endpoints Specification

| Method | Endpoint | Description | Auth Level |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register new user account and organization tenant | Public |
| `POST` | `/api/auth/login` | Authenticate credentials and receive signed JWT | Public |
| `GET` | `/api/auth/me` | Retrieve authenticated user profile and tenant context | `AUTHENTICATED` |
| `POST` | `/api/auth/logout` | Revoke active JWT token via server-side denylist | `AUTHENTICATED` |
| `GET` | `/api/admin/users` | Retrieve organization user roster | `ROLE_ADMIN` |
| `GET` | `/` | Root API platform metadata and endpoint discovery | Public |
| `GET` | `/api/health` | Application health and live MySQL `SELECT 1` ping | Public |
| `GET` | `/api/status` | Runtime diagnostics, JVM memory meters, and modules | Public |
| `GET` | `/api/cloud-accounts` | List all registered cloud provider accounts (Tenant Isolated) | `AUTHENTICATED` |
| `POST` | `/api/cloud-accounts` | Register new cloud account connection | `AUTHENTICATED` |
| `POST` | `/api/cloud-accounts/{id}/connect` | Securely encrypt (AES-256-GCM) and connect credentials | `AUTHENTICATED` |
| `POST` | `/api/cloud-accounts/{id}/validate`| Validate live IAM/RBAC permissions for connected account | `AUTHENTICATED` |
| `POST` | `/api/cloud-accounts/{id}/disconnect`| Disconnect and wipe stored encrypted credentials | `AUTHENTICATED` |
| `GET` | `/api/cloud-accounts/{id}/connection-status`| Get live connection, credential, and sync status | `AUTHENTICATED` |
| `GET` | `/api/cloud-providers` | List supported cloud providers and capabilities | `AUTHENTICATED` |
| `GET` | `/api/cloud-providers/{p}/capabilities`| Get specific provider API and permission capabilities | `AUTHENTICATED` |
| `POST` | `/api/cloud-providers/{p}/test-connection`| Probe credentials connectivity without saving | `AUTHENTICATED` |
| `POST` | `/api/billing/sync/{id}` | Trigger multi-cloud billing synchronization | `AUTHENTICATED` |
| `GET` | `/api/billing/sync/history` | Paginated audit trail of all ingestion runs | `AUTHENTICATED` |
| `GET` | `/api/billing/sync/status/{id}` | Get latest synchronization summary for account | `AUTHENTICATED` |
| `GET` | `/api/billing` | Paginated billing line-items and filters | `AUTHENTICATED` |
| `POST` | `/api/billing/upload` | Ingest multipart CSV billing records | `AUTHENTICATED` |
| `GET` | `/api/costs/summary` | Multi-cloud cost aggregation summary (Currency Verified) | `AUTHENTICATED` |
| `GET` | `/api/costs/by-provider`| Aggregated cost distribution by cloud provider | `AUTHENTICATED` |
| `GET` | `/api/costs/by-service` | Top spend by cloud service | `AUTHENTICATED` |
| `GET` | `/api/costs/trends` | Monthly time-series expenditure trends | `AUTHENTICATED` |
| `GET` | `/api/analytics/overview` | Unified FinOps executive summary & KPI metrics | `AUTHENTICATED` |
| `GET` | `/api/analytics/spending-trends` | Temporal trends (Daily, Weekly, Monthly, Quarterly) | `AUTHENTICATED` |
| `GET` | `/api/analytics/provider-comparison` | Multi-cloud expenditure share and percentage split | `AUTHENTICATED` |
| `GET` | `/api/analytics/service-breakdown` | Ranked service expenditure & percentage contributions | `AUTHENTICATED` |
| `GET` | `/api/analytics/account-breakdown` | Cost per cloud account & provider mapping | `AUTHENTICATED` |
| `GET` | `/api/analytics/month-over-month` | MoM delta comparison with provider & service shifts | `AUTHENTICATED` |
| `GET` | `/api/analytics/forecast` | Statistical forecasting (Moving Avg & SES) + 95% CI | `AUTHENTICATED` |
| `GET` | `/api/analytics/budget-utilization` | Budget burn rates, projected EOP, & health status | `AUTHENTICATED` |
| `GET` | `/api/analytics/cost-allocation` | Multi-dimensional allocation (Provider, Account, Service, Region) | `AUTHENTICATED` |
| `GET` | `/api/analytics/insights` | Deterministic rule-based FinOps recommendations | `AUTHENTICATED` |
| `GET` | `/api/analytics/top-spending-services` | Top-N cloud services by expenditure | `AUTHENTICATED` |
| `GET` | `/api/analytics/top-spending-accounts` | Top-N cloud accounts by expenditure | `AUTHENTICATED` |
| `GET` | `/api/analytics/export/spending-trends` | Stream spending trends CSV report | `AUTHENTICATED` |
| `GET` | `/api/analytics/export/provider-comparison` | Stream multi-cloud provider comparison CSV | `AUTHENTICATED` |
| `GET` | `/api/analytics/export/service-breakdown` | Stream service breakdown CSV report | `AUTHENTICATED` |
| `GET` | `/api/analytics/export/forecast` | Stream cost forecast projections CSV | `AUTHENTICATED` |
| `GET` | `/api/analytics/export/insights` | Stream actionable FinOps insights CSV | `AUTHENTICATED` |
| `GET` | `/api/budgets` | List budgets with live calculated spend utilization | `AUTHENTICATED` |
| `POST` | `/api/budgets` | Create budget allocation and alerting threshold | `AUTHENTICATED` |
| `GET` | `/api/anomalies` | List detected cost anomalies with filters & pagination | `AUTHENTICATED` |
| `GET` | `/api/anomalies/{id}` | Retrieve detailed anomaly record & root-cause diagnostic | `AUTHENTICATED` |
| `GET` | `/api/anomalies/summary` | Anomaly KPI metrics (critical count, potential excess spend) | `AUTHENTICATED` |
| `GET` | `/api/anomalies/trends` | Daily anomaly detection counts & excess cost time series | `AUTHENTICATED` |
| `POST` | `/api/anomalies/detect` | Execute multi-algorithm statistical anomaly detection scan | `AUTHENTICATED` |
| `PATCH`| `/api/anomalies/{id}/status` | Transition lifecycle status (`OPEN`, `ACK`, `RESOLVED`, `DISMISSED`) | `AUTHENTICATED` |
| `GET` | `/api/anomalies/{id}/history`| Retrieve audit history trail of status transitions | `AUTHENTICATED` |
| `GET` | `/api/anomalies/config` | Inspect tenant anomaly detection parameters | `AUTHENTICATED` |
| `PUT` | `/api/anomalies/config` | Update statistical thresholds & automated scanning toggle | `AUTHENTICATED` |
| `GET` | `/api/notifications` | List in-app notifications (optional unread-only filter) | `AUTHENTICATED` |
| `GET` | `/api/notifications/unread-count`| Get unread alert notification count badge | `AUTHENTICATED` |
| `PATCH`| `/api/notifications/{id}/read` | Mark single notification as read | `AUTHENTICATED` |
| `POST` | `/api/notifications/read-all`| Mark all notifications as read for current organization | `AUTHENTICATED` |
| `GET` | `/swagger-ui.html` | Interactive Swagger UI API documentation | Public |
| `GET` | `/v3/api-docs` | OpenAPI 3.0 raw specification JSON | Public |

---

## 9. Security & Encryption Configuration

### Environment Variables
Configure security settings in environment variables or `application.properties`:
- `JWT_SECRET`: Minimum 256-bit hexadecimal secret key for HMAC-SHA256 token signing.
- `JWT_EXPIRATION_MS`: Access token validity duration in milliseconds (default: `86400000` / 24 hours).
- `CLOUD_CREDENTIAL_ENCRYPTION_KEY`: 256-bit (32 bytes / 64 hex characters) key for AES-256-GCM credential encryption. Validated at startup with an automatic cryptographic self-test.
- `CORS_ALLOWED_ORIGINS`: Permitted frontend origins (default: `http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173`).

---

## 10. Development Roadmap

- [x] **Milestone 1 — Complete Project Foundation & Architecture**
  - [x] Spring Boot 3 + Java 21 REST API Engine
  - [x] React 19 + Vite + Tailwind CSS Frontend Shell
  - [x] MySQL 8+ Database Schema & HikariCP Integration
  - [x] Clean Architecture with Standardized `ApiResponse` and `@RestControllerAdvice`
- [x] **Milestone 2 — Professional SaaS Dashboard Development**
  - [x] Enterprise Dark Navy SaaS UI with Professional Blue Accent Palette
  - [x] Responsive Collapsible Sidebar with Route Highlighting
  - [x] 4 Primary FinOps KPI Cards & Chart.js Visualizations
  - [x] Interactive Billing Table, Cloud Accounts Grid, Budget Guardrails, & AI Anomaly Feed
- [x] **Milestone 3 — Spring Boot Backend REST APIs & Persistence**
  - [x] Complete REST Controllers for Cloud Accounts, Billing CSV Processing, Budgets, and Anomalies
  - [x] Standardized Envelope Responses & Centralized Exception Handling
  - [x] Automated Postman & JUnit Test Suites
- [x] **Milestone 4 — JWT Authentication & Role-Based Authorization**
  - [x] User Registration (`/api/auth/register`) with BCrypt Password Hashing
  - [x] Secure Login (`/api/auth/login`) with HMAC-SHA256 Signed JWT Generation
  - [x] User Profile Inspection (`/api/auth/me`) & Stateless Logout Denylist (`/api/auth/logout`)
  - [x] Spring Security 6 Stateless Filter Chain with `JwtAuthenticationFilter`
  - [x] Multi-Tenant Data Isolation and IDOR Prevention Across All Domain Services
  - [x] Role-Based Access Control (`ROLE_ADMIN` vs `ROLE_USER`)
  - [x] React 19 SaaS Login & Registration Pages, `AuthContext`, and `ProtectedRoute`
  - [x] 100% Automated Backend Tests (50 JUnit tests passed) & 51 Postman Newman tests passed
- [x] **Milestone 5 — Multi-Cloud Provider Ingestion & Real-Time Billing**
  - [x] Modular `CloudBillingProvider` SPI (`AwsBillingProvider`, `AzureBillingProvider`, `GcpBillingProvider`)
  - [x] Authenticated AES-256-GCM Credential Encryption with Startup Key Validation & `v1` Key Versioning
  - [x] Deterministic SHA-256 Fingerprint Generator honoring stable provider-native record IDs
  - [x] MySQL Database-Level Duplicate Prevention (`uq_billing_record_dedup`) with Concurrent Collision Safety
  - [x] Concurrency Lock Management (`SyncLockManager`) & Swappable Distributed Lock Abstraction
  - [x] Full Ingestion Pipeline (`BillingIngestionService`) with Transactional Persistence and `SyncHistory` Auditing
  - [x] Scheduled Ingestion (`BillingSyncScheduler`) & Manual Real-Time Sync Triggers
  - [x] Strict Currency Consistency Check across all multi-cloud expenditure analytics
  - [x] Explicit Provenance Tracking (`REAL_PROVIDER`, `MOCK_PROVIDER`, `CSV_UPLOAD`, `MANUAL`)
  - [x] React 19 UI: Provider Capabilities Cards, Connect Credentials Modal, Validation, Sync Now, & Audit Logs
  - [x] 100% Automated Verification: 83 JUnit 5 & Mockito Tests Passed, 66 Postman Newman Requests (89 Assertions) Passed, Frontend Production Build Succeeded
- [x] **Milestone 6 — Advanced FinOps Analytics & Cost Forecasting**
  - [x] Advanced cloud spending metrics (Total spend, MTD, MoM, daily velocity, peak/trough days)
  - [x] Multi-cloud cost comparison with real provider distribution percentages (AWS, Azure, GCP)
  - [x] Temporal spending trends across Daily, Weekly, Monthly, and Quarterly granularities
  - [x] Month-over-Month (MoM) financial comparison with provider, service breakdowns, and explainable insights
  - [x] Native Java 21 statistical forecasting engine (Moving Average & Simple Exponential Smoothing)
  - [x] Forecast horizons (7D, 30D, 90D) with mathematically derived 95% confidence intervals
  - [x] Budget utilization analytics (allocated, spend, burn rate, projected EOP, and health statuses: `HEALTHY`, `WARNING`, `CRITICAL`, `EXCEEDED`)
  - [x] Multi-dimensional cost allocation (Cloud Provider, Account, Service, Region)
  - [x] Rule-based FinOps insight engine evaluating growth spikes, concentration risk, and budget breaches
  - [x] Strict multi-currency isolation and BigDecimal arithmetic precision
  - [x] High-performance streaming CSV export endpoints for all analytics views
  - [x] React 19 FinOps Analytics Dashboard (`AnalyticsPage.jsx`) with interactive Chart.js visualizations
  - [x] Database query optimization with composite B-Tree indexes (`idx_billing_org_period`, etc.)
  - [x] 100% Automated Verification: 105 JUnit 5 & Mockito Tests Passed, 20-Request Postman Test Suite Added, Frontend Production Build Succeeded
- [x] **Milestone 7 — Real-Time Cost Anomaly Detection Engine**
  - [x] Native Java 21 statistical anomaly detection engine with 4 distinct detector strategies:
    - [x] Z-Score Outlier Detection (mean, sample standard deviation, safe zero-variance handling, configurable $\sigma$ thresholds)
    - [x] Moving Average Deviation (rolling $k$-day window baseline comparison, percentage deviation)
    - [x] Spending Spike Detection (sudden overnight and daily cost surge evaluation against historical baselines)
    - [x] Budget-Based Detection (near-capacity warning at 90% and critical limit breach at 100% utilization)
  - [x] Anomaly Lifecycle Management with state transitions (`OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `DISMISSED`)
  - [x] Transition History Audit Trail (`CostAnomalyHistory`) capturing operator email, timestamps, and notes
  - [x] Tenant-Level Detection Configuration (`AnomalyDetectionConfig`) with customizable thresholds
  - [x] Automated Scheduled Detection (`AnomalyDetectionScheduler`) with independent multi-tenant iteration
  - [x] Concurrency Lock Management (`AnomalyDetectionLockManager`) with atomic in-memory locking
  - [x] In-App Notification Subsystem (`NotificationService`) with real-time unread counter badge in header
  - [x] Deduplication Guards: prevents spamming duplicate open anomalies or repeat notifications for the same resource
  - [x] React 19 Anomaly Dashboard (`AnomalyDetectionPage.jsx`) with KPI ribbon, dual Chart.js visualizations, search, filter toolbar, pagination, detailed inspection drawer, and settings modal
  - [x] 100% Automated Verification: 129 JUnit 5 & Mockito Tests Passed (0 failures), 10-Request Postman Test Suite Added (Folder 11), Frontend Production Build Succeeded
- [ ] **Milestone 8 — Automated Cost Optimization & Rightsizing Engine (Upcoming)**
  - [ ] Cloud rightsizing recommendations, idle resource detection, and reserved capacity planning


