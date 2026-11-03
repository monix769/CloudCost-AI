# CloudCost AI – Deployment Guide

This guide outlines deployment preparation for **CloudCost AI** using **Vercel** for the React frontend and **Render** for the Spring Boot backend and managed MySQL.

---

## 1. Frontend Deployment (Vercel)

### Architecture
- **Framework**: Vite + React
- **Output Directory**: `dist`
- **Build Command**: `npm run build`
- **Install Command**: `npm install`

### Steps
1. Push repository to GitHub: `git push origin main`.
2. Log in to [Vercel](https://vercel.com) and import the repository.
3. Configure the Project Settings:
   - **Root Directory**: `frontend`
   - **Framework Preset**: Vite
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
4. Set Environment Variables:
   - `VITE_API_BASE_URL`: `https://cloudcost-ai-backend.onrender.com` (Your Render backend URL)
5. Deploy.
6. Verify SPA Routing: A `vercel.json` rewrite configuration can be placed in `frontend/vercel.json`:
```json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

---

## 2. Backend Deployment (Render)

### Architecture
- **Runtime**: Docker (Multi-stage Eclipse Temurin 21) or Native Java
- **Root Directory**: `backend`
- **Docker Context**: `./backend`
- **Port**: `8080` (or `PORT` environment variable injected by Render)

### Database Provisioning on Render
1. Create a **Managed MySQL 8** instance on Render or connect to cloud MySQL (AWS RDS / Aiven).
2. Note the internal connection string:
   - `DB_URL`: `jdbc:mysql://<host>:<port>/cloudcost_ai?useSSL=true&serverTimezone=UTC`
   - `DB_USERNAME`: `<db_user>`
   - `DB_PASSWORD`: `<db_password>`
3. Run `database/schema.sql` against the database to initialize tables.

### Web Service Setup on Render
1. Create a new **Web Service** on [Render](https://render.com).
2. Select **Docker** environment with context `./backend` and Dockerfile `./backend/Dockerfile`.
3. Configure Environment Variables:
   - `PORT`: `8080`
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DB_URL`: `jdbc:mysql://<host>:<port>/cloudcost_ai?useSSL=true&serverTimezone=UTC`
   - `DB_USERNAME`: `<db_user>`
   - `DB_PASSWORD`: `<db_password>`
   - `JPA_DDL_AUTO`: `validate`
   - `CORS_ALLOWED_ORIGINS`: `https://your-frontend.vercel.app`
   - `JWT_SECRET`: `<64-byte-secure-hex-key>`
4. Deploy and verify health check: `https://<your-service>.onrender.com/api/health`.
