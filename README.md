
# CloudCost AI – Intelligent Cloud Cost Monitoring & FinOps Analytics Platform

CloudCost AI is a portfolio project that demonstrates a full-stack cloud cost monitoring and FinOps analytics platform. It provides a local dashboard for managing cloud accounts, reviewing billing data, monitoring budgets, identifying unusual spending, and exploring cost-optimization recommendations.

> **Project status:** Local development and Docker-based demonstration. This project has not been deployed to a public production environment. Live AWS, Azure, and GCP credentials are not required for the demo.

## 1. Features

- SaaS-style dashboard with responsive navigation.
- User registration, login, JWT authentication, and role-based access.
- Organization-aware data access.
- Cloud account management for AWS, Azure, and Google Cloud.
- Billing record management and CSV import.
- Cost analytics, trends, forecasts, and cost allocation.
- Budgets and budget monitoring.
- Spending anomaly detection and anomaly lifecycle management.
- Cost-optimization recommendations, including rightsizing and commitment analysis.
- PDF and CSV report generation.
- Organization profile, team invitations, and audit logs.
- Docker-based local environment.

## 2. Technology Stack

### Frontend
- React 19
- Vite
- JavaScript (ES6+)
- Tailwind CSS
- React Router
- Axios
- Chart.js
- Lucide React

### Backend
- Java 21
- Spring Boot 3.3.5
- Spring Security
- Spring Data JPA and Hibernate
- Maven
- REST APIs
- JWT authentication
- BCrypt password hashing

### Database and Tools
- MySQL 8+
- Docker and Docker Compose
- Git and GitHub
- Postman
- JUnit 5 and Mockito

## 3. Architecture

The React frontend communicates with the Spring Boot backend through REST APIs. The backend applies authentication and authorization, runs business logic, and accesses MySQL through Spring Data JPA/Hibernate.

```text
React + Vite Frontend
        |
        | HTTP / JSON (REST API)
        v
Spring Boot Backend
        |
        | Spring Data JPA / Hibernate
        v
MySQL Database
```

## 4. Project Structure

```text
CloudCost-AI/
│
├── frontend/              # React application
├── backend/               # Spring Boot application
├── database/              # SQL schema and migration scripts
├── docs/                  # API, architecture, testing and setup documentation
├── postman/               # Postman collections and environments
├── docker-compose.yml     # Local multi-container configuration
└── README.md
```

Check the repository for the exact current file list.

## 5. Run Locally on Windows

### Prerequisites

Install:
- Java 21
- Node.js and npm
- MySQL 8+
- Git

### Backend Setup

1. Open PowerShell in the project root.
2. Configure the backend database connection using your local environment configuration.
3. Create the database in MySQL if it does not already exist:

```sql
CREATE DATABASE cloudcost_ai;
```

4. Open the backend folder:

```powershell
cd backend
```

5. Run the Spring Boot application using the Maven wrapper, if included:

```powershell
.\mvnw.cmd spring-boot:run
```

If the Maven wrapper is not included, use:

```powershell
mvn spring-boot:run
```

### Frontend Setup

Open another PowerShell window in the project root, then run:

```powershell
cd frontend
npm install
npm run dev
```

Use the local URL printed by Vite in the terminal.

## 6. Run with Docker Compose

From the project root:

```powershell
docker compose up -d --build
docker compose ps
```

Expected local endpoints:

| Service | URL / Port |
|---|---|
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080 |
| Health endpoint | http://localhost:8080/api/health |
| MySQL host port | 3307 |

The backend connects to MySQL using the Docker service hostname and the internal MySQL port. The host port mapping is for access from the Windows machine.

### View Logs

```powershell
docker compose logs -f
```

### Stop Containers

```powershell
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete the persisted database volume and its data.

## 7. API Overview

The backend exposes REST endpoints for the main application modules, including:

- Authentication and current-user information.
- Cloud account management.
- Billing records and CSV import.
- Cost analytics and forecasts.
- Budgets.
- Anomaly detection and management.
- Cost optimization recommendations.
- Reports and downloads.
- Organization settings, invitations, and audit logs.
- Application health and status.

For exact paths, request methods, parameters, and response examples, refer to the API documentation and Postman collection included in the repository.

## 8. API Documentation

Swagger UI availability depends on the active Spring profile and configuration.

In development, the documented Swagger UI path is:

http://localhost:8080/swagger-ui.html

Swagger may be disabled by default in the production profile. If it is disabled, enable it only in the local environment using the project's supported configuration.

## 9. Testing

The project includes backend unit/integration tests and frontend build verification. The project milestone report records **159 backend tests passing**.

Postman collections are provided for API testing. A collection being present does not mean every request has been executed successfully in Newman. Run the relevant collection in your environment to verify current behavior.

### Frontend Production Build

```powershell
cd frontend
npm install
npm run build
```

### Backend Tests

From the backend folder:

```powershell
.\mvnw.cmd test
```

Use the Maven wrapper command only if `mvnw.cmd` exists in the backend folder.

## 10. Security

- Passwords are hashed using BCrypt.
- API authentication uses JWT.
- Role-based access control is implemented.
- Organization-level data access is enforced in application services.
- Cloud credentials are designed to be encrypted using AES-256-GCM.
- Secrets and environment files must not be committed to Git.
- Do not use demo credentials or development secrets in a public deployment.

Before sharing the repository, check that `.env` files, API keys, database passwords, JWT secrets, and encryption keys are excluded from Git.

## 11. Development Milestones

1. **M1** – Project foundation and application setup.
2. **M2** – Professional dashboard and navigation.
3. **M3** – REST APIs and database persistence.
4. **M4** – JWT authentication and authorization.
5. **M5** – Multi-cloud provider ingestion architecture.
6. **M6** – FinOps analytics and cost forecasting.
7. **M7** – Cost anomaly detection.
8. **M8** – Cost optimization recommendations.
9. **M9** – Reports and organization features.
10. **M10** – Final testing and local Docker readiness.

## 12. Known Limitations

- The application is intended for local portfolio demonstration; no public production deployment is claimed.
- Live cloud-provider ingestion requires valid provider credentials and configuration. The local demo does not prove live provider connectivity.
- Rightsizing recommendations require resource telemetry. Billing records alone are not sufficient to determine safe resource sizes.
- Commitment-plan savings are estimates based on assumptions and are not guaranteed provider discounts.
- In-memory synchronization locks and token-denylist behavior are suitable for a single-instance demo, not a horizontally scaled deployment.
- Local filesystem report storage may not persist across some hosted deployment environments.
- Forecasts and anomaly detection are analytical estimates; they should not be treated as guaranteed predictions.

## 13. Future Enhancements

- Verify live ingestion against real AWS, Azure, and GCP accounts.
- Add durable distributed locks and shared token revocation storage.
- Add object storage for generated reports.
- Add automated end-to-end testing for all Postman collections.
- Improve forecasting with seasonality-aware models and backtesting.
- Add deployment-specific monitoring and operational alerting.

## 14. Local Demo

For the local demonstration, start the application with Docker Compose and open:

**http://localhost:3000**

Use only the credentials configured for your local environment. Do not publish default or personal passwords in this README.

## 15. License

Add a `LICENSE` file and update this section if you choose to distribute the project under a specific open-source license.
