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
│   └── README.md                             # Database design and migration guide
│
├── sample_data/                              # Mock and seed test datasets
│   ├── seed_data.sql                         # Initial organization and user seed records
│   └── sample_billing_records.json           # Sample cloud spend records (AWS, GCP)
│
├── postman/                                  # API Testing Suite
│   ├── CloudCost AI – API Testing.postman_collection.json
│   └── CloudCost AI – Local Development.postman_environment.json
│
└── docs/                                     # In-depth architectural & API documents
    ├── architecture.md                       # Comprehensive design specifications
    ├── api-documentation.md                  # REST endpoint definitions & payloads
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

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/` | Root API platform metadata and endpoint discovery | Public |
| `GET` | `/api/health` | Application health and live MySQL `SELECT 1` ping | Public |
| `GET` | `/api/status` | Runtime diagnostics, JVM memory meters, and modules | Public |
| `GET` | `/swagger-ui.html` | Interactive Swagger UI API documentation | Public |
| `GET` | `/v3/api-docs` | OpenAPI 3.0 raw specification JSON | Public |

---

## 9. Development Roadmap

- [x] **Milestone 1 — Complete Project Foundation & Architecture**
  - [x] Spring Boot 3 + Java 21 REST API Engine
  - [x] React 19 + Vite + Tailwind CSS Frontend Application Shell
  - [x] MySQL 8+ Database Schema & HikariCP Integration
  - [x] Clean Architecture with Standardized `ApiResponse` and `@RestControllerAdvice`
  - [x] Interactive Endpoint Tester & Health Probes
  - [x] Postman Collection & JUnit 5 Testing Suite
  - [x] Docker & Multi-Environment Configuration
- [x] **Milestone 2 — Professional SaaS Dashboard Development**
  - [x] Enterprise Dark Navy SaaS UI with Professional Blue Accent Palette
  - [x] Responsive Collapsible Sidebar with Active Route Highlighting & Mobile Drawer
  - [x] Dynamic Header with Backend Live Status, Notifications, & User Profile Dropdown
  - [x] 4 Primary FinOps KPI Cards (Total Spend, Budget, Utilization %, Potential Savings)
  - [x] Chart.js Visualizations (Multi-Month Trends, Provider Distribution, Top Services, Budget Forecast)
  - [x] Interactive Recent Billing Activity Table with Search & Multi-Cloud Provider Filters
  - [x] Connected Cloud Environments Overview (AWS, GCP, Azure tenant cards)
  - [x] Cost Center Budget Guardrails & Threshold Warning Tracking
  - [x] Machine Learning Anomaly Detection Feed with Severity Pills & Resolution Workflows
  - [x] Actionable Cost Optimization Engine with Estimated Monthly Savings
  - [x] Executive Reports Center & Tenant Governance Settings
  - [x] Preserved Milestone 1 System Architecture, Health Diagnostics, and OpenAPI Specifications
- [ ] **Milestone 3 — Cloud Provider Ingestion & Real-Time Billing (Upcoming)**
  - AWS Cost Explorer, Azure Cost Management, and GCP Billing IAM connectors
  - Automated daily & hourly cloud spend aggregation
- [ ] **Milestone 4 — Advanced Unit Economics & Custom Dashboards (Upcoming)**
  - Dynamic user-configurable FinOps widgets & custom KPI formulas
- [ ] **Milestone 5 — Automated Rightsizing & Cloud Guardrail Enforcement (Upcoming)**
  - Scheduled automated resource shutdown and rightsizing approval workflows
