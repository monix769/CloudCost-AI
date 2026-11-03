# CloudCost AI - Database Architecture & Guidelines

## 1. Overview
The **CloudCost AI** persistence tier is built on **MySQL 8.0+**, managed through **Spring Data JPA** and **Hibernate**. The database model is designed for high-throughput FinOps analytical queries, multi-tenancy, and cost tracking.

---

## 2. Database Information
- **Database Name**: `cloudcost_ai`
- **Default Port**: `3306`
- **Character Set**: `utf8mb4`
- **Collation**: `utf8mb4_unicode_ci`
- **Storage Engine**: `InnoDB`

---

## 3. Entity-Relationship Overview

| Table | Purpose | Primary Key | Foreign Keys / References |
|---|---|---|---|
| `organizations` | Multi-tenant organization boundaries | `id` (BIGINT AUTO_INCREMENT) | None |
| `users` | User credentials, roles, and profiles | `id` (BIGINT AUTO_INCREMENT) | `organization_id` -> `organizations(id)` |
| `cloud_accounts` | Linked AWS, Azure, GCP accounts | `id` (BIGINT AUTO_INCREMENT) | `organization_id` -> `organizations(id)` |
| `billing_records` | Granular cloud spend line items | `id` (BIGINT AUTO_INCREMENT) | `organization_id`, `cloud_account_id` |
| `budgets` | Cost threshold and budget tracking | `id` (BIGINT AUTO_INCREMENT) | `organization_id` -> `organizations(id)` |
| `cost_anomalies` | Detected cost spikes and deviations | `id` (BIGINT AUTO_INCREMENT) | `organization_id`, `cloud_account_id` |

---

## 4. Configuration & Environment Variables

| Variable | Default (Local Dev) | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/cloudcost_ai?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | JDBC Connection URL |
| `DB_USERNAME` | `root` | MySQL Database User |
| `DB_PASSWORD` | `root` | MySQL Database Password |
| `JPA_DDL_AUTO` | `update` (dev) / `validate` (prod) | Hibernate Schema DDL Policy |
| `JPA_SHOW_SQL` | `false` | Log formatted SQL queries |

> [!IMPORTANT]
> **Production Safety Policy**: Never use `create` or `create-drop` in production. For production environments, `spring.jpa.hibernate.ddl-auto` is strictly set to `validate` with version-controlled SQL migrations.

---

## 5. Setup & Verification Instructions

### Manual Setup via MySQL CLI:
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS cloudcost_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p cloudcost_ai < database/schema.sql
```

### Verification Query:
```sql
USE cloudcost_ai;
SHOW TABLES;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'cloudcost_ai';
```
