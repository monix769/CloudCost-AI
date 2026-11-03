# CloudCost AI – Architecture & System Design Document

## 1. System Overview
**CloudCost AI** is an intelligent, multi-cloud FinOps SaaS platform designed to monitor expenditures, aggregate billing records, track budgets, detect spending anomalies, and provide AI-driven cost optimization recommendations across AWS, GCP, and Microsoft Azure.

---

## 2. Architectural Principles

### 2.1 Clean & Layered Architecture
CloudCost AI strictly enforces separation of concerns across four key architectural tiers:
1. **Presentation Tier (React 19 + Vite)**:
   - Component-driven UI styled with custom Tailwind CSS tokens.
   - Centralized Axios client (`services/api.js`) encapsulating authentication headers and error handling.
   - Reactive global system state via React Context (`SystemContext`).
2. **Security & Gateway Tier (Spring Security 6)**:
   - Stateless session management (`SessionCreationPolicy.STATELESS`).
   - Declarative CORS filter integrated for cross-origin SPA requests.
   - Architectural foundation prepared for JWT authentication filters in Milestone 2.
3. **Application & Service Tier (Spring Boot 3 + Java 21)**:
   - Decoupled `Controller -> Service -> Repository` flow.
   - Centralized exception interception with `@RestControllerAdvice`.
   - Unified API response envelope (`ApiResponse<T>`).
4. **Data Access & Persistence Tier (Spring Data JPA + MySQL 8)**:
   - Object-relational mapping via Hibernate 6.5.
   - Enterprise connection pooling via HikariCP.
   - Non-destructive schema evolution policy (`update` in development, `validate` in production).

---

## 3. Technology Stack Reference

| Layer | Technology | Version | Rationale |
|---|---|---|---|
| Language (Backend) | Java | 21 (LTS) | Modern virtual threads, pattern matching, record types |
| Language (Frontend)| JavaScript | ES6+ / JSX | Standard modern React web ecosystem |
| Backend Framework | Spring Boot | 3.3.5 | Industry standard for robust, high-performance enterprise APIs |
| ORM / Persistence | Spring Data JPA / Hibernate | 6.5.x | Automated query derivation and schema mapping |
| Database | MySQL | 8.0+ | ACID-compliant relational storage with JSON support |
| Security | Spring Security | 6.3.x | Enterprise authentication and authorization framework |
| Documentation | Springdoc OpenAPI | 2.6.0 | Automated OpenAPI 3 and interactive Swagger UI generation |
| Frontend Framework | React.js | 19.x | Component lifecycle, hooks, and reactive state |
| Build Tool (FE) | Vite | 8.x | High-speed ESM dev server and optimized Rollup bundler |
| Styling | Tailwind CSS | 3.4.x | Utility-first design system with dark mode |
| Testing (BE) | JUnit 5 + Mockito | 5.10.x | Unit and controller-level integration testing |
| API Testing | Postman | v2.1.0 | Automated collection verification with test assertions |
| Containerization | Docker & Docker Compose | 3.8 | Multi-container portability across environments |

---

## 4. REST API Standard Response Format

All REST endpoints return a standardized JSON envelope:

```json
{
  "success": true,
  "message": "Human-readable status message",
  "timestamp": "2026-11-03T15:00:00.000Z",
  "data": { ... }
}
```

In the event of an exception or invalid parameter:
```json
{
  "timestamp": "2026-11-03T15:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Input validation failed",
  "path": "/api/status",
  "validationErrors": {
    "cloudProvider": "Invalid cloud provider specified"
  }
}
```
