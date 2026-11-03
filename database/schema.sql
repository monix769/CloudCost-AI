-- ===================================================================
-- CloudCost AI - Database Schema Definition
-- Intelligent Cloud Cost Monitoring & FinOps Analytics Platform
-- MySQL 8.0+ Compatibility
-- ===================================================================

CREATE DATABASE IF NOT EXISTS cloudcost_ai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE cloudcost_ai;

-- -------------------------------------------------------------------
-- 1. Organizations (Multi-Tenancy Foundation)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    tier VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_organizations_slug (slug),
    INDEX idx_organizations_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 2. Users & Roles (Authentication & RBAC Foundation)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'FINOPS_ANALYST',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    INDEX idx_users_org (organization_id),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 3. Cloud Accounts (AWS, Azure, GCP Provider Connections)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cloud_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    provider ENUM('AWS', 'AZURE', 'GCP') NOT NULL,
    account_id VARCHAR(100) NOT NULL,
    account_name VARCHAR(150) NOT NULL,
    arn_or_client_id VARCHAR(255) NULL,
    sync_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    last_synced_at TIMESTAMP NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    UNIQUE KEY uq_org_provider_account (organization_id, provider, account_id),
    INDEX idx_cloud_accounts_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 4. Billing Records (Granular Cost Line Items)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS billing_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    cloud_account_id BIGINT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    region VARCHAR(60) NOT NULL,
    cost_amount DECIMAL(14, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    usage_type VARCHAR(120) NULL,
    usage_quantity DECIMAL(16, 4) NULL,
    usage_unit VARCHAR(50) NULL,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    resource_id VARCHAR(255) NULL,
    tags JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (cloud_account_id) REFERENCES cloud_accounts(id) ON DELETE CASCADE,
    INDEX idx_billing_period (billing_period_start, billing_period_end),
    INDEX idx_billing_service (service_name),
    INDEX idx_billing_org_account (organization_id, cloud_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 5. Budgets (Financial Thresholds & Alerting)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    amount_limit DECIMAL(14, 2) NOT NULL,
    current_spend DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    period ENUM('MONTHLY', 'QUARTERLY', 'ANNUAL') NOT NULL DEFAULT 'MONTHLY',
    alert_threshold_percentage DECIMAL(5, 2) NOT NULL DEFAULT 80.00,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_budgets_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 6. Cost Anomalies (AI / Heuristic Anomaly Records)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cost_anomalies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    cloud_account_id BIGINT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    expected_cost DECIMAL(14, 4) NOT NULL,
    actual_cost DECIMAL(14, 4) NOT NULL,
    deviation_percentage DECIMAL(6, 2) NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'IGNORED') NOT NULL DEFAULT 'OPEN',
    root_cause_summary TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (cloud_account_id) REFERENCES cloud_accounts(id) ON DELETE CASCADE,
    INDEX idx_anomalies_org (organization_id),
    INDEX idx_anomalies_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
