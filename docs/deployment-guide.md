# CloudCost AI – Production Deployment Guide

## 1. Overview & Target Cloud Topology

This guide details the end-to-end production deployment process for **CloudCost AI** using modern cloud infrastructure:

```
[ Client Web Browsers ]
          │ (HTTPS)
          ▼
[ Frontend: Vercel Global Edge Network ]
   • React 19 + Vite 8 SPA
   • Static Assets cached via Edge CDN (1-year immutable max-age)
   • SPA Rewrites & Security Headers configured via `vercel.json`
          │ (REST API / HTTPS / JWT Bearer)
          ▼
[ Backend: Render Web Service ]
   • Java 21 LTS + Spring Boot 3.3.5
   • Multi-stage Docker Container (Distroless Temurin Alpine JRE, non-root user)
   • Health Check endpoint: `/api/health`
   • Profile: `spring.profiles.active=prod`
          │ (JDBC / TLS / SSL)
          ▼
[ Database: Managed MySQL 8.0+ ]
   • Compatible providers: Aiven, Amazon RDS, DigitalOcean, PlanetScale
   • UTF-8 MB4 character set, InnoDB engine
   • HikariCP connection pooling
```

---

## 2. Managed MySQL Database Provisioning

### 2.1 Provider Requirements
- **MySQL Engine**: Version 8.0 or higher.
- **Default Character Set**: `utf8mb4`
- **Default Collation**: `utf8mb4_unicode_ci`
- **Max Connections**: Minimum 25 connections to support HikariCP pool (`maximum-pool-size=20`).

### 2.2 Schema Initialization
Connect to your remote database instance using standard MySQL CLI or a client such as MySQL Workbench / DBeaver:

```bash
# Connect to remote managed MySQL
mysql -h <DB_HOST> -P <DB_PORT> -u <DB_USERNAME> -p <DB_NAME> --ssl-mode=REQUIRED

# Execute master schema initialization
source database/schema.sql;

# (Optional) Seed initial tenant and cloud accounts
source sample_data/seed_data.sql;
```

### 2.3 Incremental Migrations
For existing deployments upgrading across milestones:
```bash
# Apply Milestone 8 Rightsizing & Optimization migrations
source database/milestone8_migration.sql;

# Apply Milestone 9 Reports, Organizations & Audit Logs migrations
source database/milestone9_migration.sql;
```

---

## 3. Backend Deployment on Render

### 3.1 Service Setup
1. Create an account at [Render](https://render.com).
2. Click **New +** → **Web Service**.
3. Connect your GitHub repository: `https://github.com/monix769/CloudCost-AI`.
4. Configure the service settings:
   - **Name**: `cloudcost-ai-backend`
   - **Region**: Choose the region closest to your MySQL database instance.
   - **Branch**: `main`
   - **Root Directory**: `backend`
   - **Runtime**: `Docker`
   - **Dockerfile Path**: `Dockerfile`
   - **Instance Type**: Starter (512 MB - 1 GB RAM recommended for JVM).

### 3.2 Health Check Configuration
- In the Render service dashboard under **Settings** → **Health Check Path**:
  - Enter: `/api/health`
  - Render will automatically perform zero-downtime rolling deploys and health validation.

### 3.3 Required Production Environment Variables
Under the **Environment** tab on Render, add the following key-value pairs:

| Variable Name | Example / Expected Format | Description |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production configuration profile |
| `PORT` | `8080` | Server listening port |
| `DB_URL` | `jdbc:mysql://<HOST>:<PORT>/<DB>?useSSL=true&requireSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | Full JDBC database connection string |
| `DB_USERNAME` | `<db_username>` | Managed database user username |
| `DB_PASSWORD` | `<db_password>` | Managed database user password |
| `JWT_SECRET` | `5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437` | Secure 256-bit (64 hex characters) cryptographic key |
| `JWT_EXPIRATION_MS` | `86400000` | Access token lifespan (24 hours) |
| `CLOUD_CREDENTIAL_ENCRYPTION_KEY` | `0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef` | 256-bit (32 bytes / 64 hex characters) AES-GCM key |
| `CORS_ALLOWED_ORIGINS` | `https://cloudcost-ai.vercel.app,http://localhost:5173` | Comma-delimited permitted frontend domains |
| `JPA_DDL_AUTO` | `validate` | Production safety: validates schema without altering |
| `HIKARI_MAX_POOL_SIZE` | `20` | HikariCP maximum connection pool size |
| `HIKARI_MIN_IDLE` | `5` | HikariCP minimum idle connection count |
| `SWAGGER_ENABLED` | `false` | Disable public Swagger UI in sensitive environments |

---

## 4. Frontend Deployment on Vercel

### 4.1 Project Import & Build Settings
1. Create an account at [Vercel](https://vercel.com).
2. Click **Add New...** → **Project**.
3. Import your GitHub repository: `https://github.com/monix769/CloudCost-AI`.
4. Configure project settings:
   - **Framework Preset**: `Vite`
   - **Root Directory**: `frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
   - **Install Command**: `npm install`

### 4.2 Required Environment Variables
Under the **Environment Variables** tab in Vercel, define:

| Variable Name | Example Value | Description |
| :--- | :--- | :--- |
| `VITE_API_BASE_URL` | `https://cloudcost-ai-backend.onrender.com` | Base URL of deployed Render backend |

### 4.3 Client-Side Routing & Security (`vercel.json`)
CloudCost AI includes `frontend/vercel.json`, which automatically configures:
- SPA client-side route fallback to `/index.html` (resolves 404s on deep links such as `/analytics`, `/anomalies`, `/reports`).
- Security response headers (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`).
- Immutable 1-year browser cache headers for `/assets/*` bundles.

---

## 5. Local Production-Grade Deployment with Docker Compose

To test the entire containerized production environment locally before deploying to cloud providers:

```bash
# 1. Ensure Docker Desktop is running
docker-compose version

# 2. Start all services in detached mode
docker-compose up --build -d

# 3. Monitor container health status
docker-compose ps

# 4. Tail real-time service logs
docker-compose logs -f backend

# 5. Verify backend health check endpoint
curl -i http://localhost:8080/api/health

# 6. Open frontend in browser
# Navigate to: http://localhost:3000
```

---

## 6. Post-Deployment Verification & Smoke Testing Checklist

After deploying to Render and Vercel:

- [ ] **1. Public Health Check**: Visit `https://<your-render-url>/api/health` and verify HTTP 200 with `status: "UP"`.
- [ ] **2. Root Discovery**: Visit `https://<your-render-url>/` and verify platform metadata JSON response.
- [ ] **3. Frontend HTTPS & Assets**: Visit `https://<your-vercel-domain>.vercel.app/` and verify that stylesheets, icons, and hero section render cleanly.
- [ ] **4. User Registration**: Register a new tenant organization and verify JWT generation.
- [ ] **5. User Login & Session**: Sign out and log back in to verify session restoration and JWT Bearer headers.
- [ ] **6. Multi-Cloud Dashboard**: Verify dashboard renders telemetry cards, charts, and provider filters.
- [ ] **7. Billing Ingestion**: Upload a sample billing CSV and verify idempotency and balance calculations.
- [ ] **8. Anomaly Detection**: Trigger an evaluation scan and verify statistical anomaly flags.
- [ ] **9. Rightsizing & Optimization**: Inspect rightsizing recommendations and commitment plans.
- [ ] **10. PDF & CSV Reporting**: Generate an Executive Summary PDF and Cloud Spending CSV, then download the resulting files.
- [ ] **11. Member Management**: Invite a team member, copy the invitation token, and test role updates.
- [ ] **12. Audit Log Explorer**: Verify that identity, report, and management events appear in the immutable audit log table.
