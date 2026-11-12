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
    encrypted_credentials TEXT NULL,
    sync_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    connection_status VARCHAR(50) NOT NULL DEFAULT 'DISCONNECTED',
    key_version VARCHAR(20) NOT NULL DEFAULT 'v1',
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
    source_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    external_record_id VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (cloud_account_id) REFERENCES cloud_accounts(id) ON DELETE CASCADE,
    UNIQUE KEY uq_billing_record_dedup (organization_id, cloud_account_id, external_record_id),
    INDEX idx_billing_period (billing_period_start, billing_period_end),
    INDEX idx_billing_service (service_name),
    INDEX idx_billing_org_account (organization_id, cloud_account_id),
    INDEX idx_billing_org_period_curr (organization_id, billing_period_start, billing_period_end, currency),
    INDEX idx_billing_org_service (organization_id, service_name),
    INDEX idx_billing_org_region (organization_id, region),
    INDEX idx_billing_org_currency (organization_id, currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 4b. Sync History (Multi-Cloud Ingestion Auditing)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sync_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    cloud_account_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    sync_start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sync_end_time TIMESTAMP NULL,
    sync_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    records_fetched INT NOT NULL DEFAULT 0,
    records_inserted INT NOT NULL DEFAULT 0,
    records_skipped INT NOT NULL DEFAULT 0,
    error_summary TEXT NULL,
    date_range_start DATE NULL,
    date_range_end DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (cloud_account_id) REFERENCES cloud_accounts(id) ON DELETE CASCADE,
    INDEX idx_sync_history_org (organization_id),
    INDEX idx_sync_history_account (cloud_account_id),
    INDEX idx_sync_history_status (sync_status)
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
-- 6. Cost Anomalies (Statistical & Real-Time Anomaly Engine)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cost_anomalies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    cloud_account_id BIGINT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    region VARCHAR(50) NULL,
    expected_cost DECIMAL(14, 4) NOT NULL,
    actual_cost DECIMAL(14, 4) NOT NULL,
    excess_cost DECIMAL(14, 4) NOT NULL DEFAULT 0.0000,
    deviation_percentage DECIMAL(6, 2) NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    detection_method VARCHAR(50) NOT NULL DEFAULT 'Z_SCORE',
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    anomaly_period_start DATE NULL,
    anomaly_period_end DATE NULL,
    dimension_key VARCHAR(255) NULL,
    status ENUM('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'IGNORED', 'DISMISSED') NOT NULL DEFAULT 'OPEN',
    root_cause_summary TEXT NULL,
    recommended_action TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (cloud_account_id) REFERENCES cloud_accounts(id) ON DELETE CASCADE,
    INDEX idx_anomalies_org (organization_id),
    INDEX idx_anomalies_severity (severity),
    INDEX idx_anomalies_org_status (organization_id, status),
    INDEX idx_anomalies_org_detected (organization_id, detected_at DESC),
    INDEX idx_anomalies_dedup (organization_id, detection_method, dimension_key, anomaly_period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 7. Cost Anomaly Status Transition History
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cost_anomaly_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cost_anomaly_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    action_by_user_id BIGINT NULL,
    action_by_user_email VARCHAR(120) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cost_anomaly_id) REFERENCES cost_anomalies(id) ON DELETE CASCADE,
    FOREIGN KEY (action_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_anomaly_history_anomaly (cost_anomaly_id),
    INDEX idx_anomaly_history_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 8. Tenant Anomaly Detection Configuration
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS anomaly_detection_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL UNIQUE,
    z_score_threshold DECIMAL(5, 2) NOT NULL DEFAULT 2.50,
    moving_avg_window_days INT NOT NULL DEFAULT 14,
    moving_avg_deviation_percent DECIMAL(5, 2) NOT NULL DEFAULT 30.00,
    spending_spike_percent DECIMAL(5, 2) NOT NULL DEFAULT 50.00,
    spending_spike_min_amount DECIMAL(14, 4) NOT NULL DEFAULT 50.0000,
    budget_warning_threshold_percent DECIMAL(5, 2) NOT NULL DEFAULT 90.00,
    budget_critical_threshold_percent DECIMAL(5, 2) NOT NULL DEFAULT 100.00,
    is_automated_detection_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_anomaly_config_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 9. In-App Notifications Subsystem
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    notification_type VARCHAR(50) NOT NULL DEFAULT 'ANOMALY_DETECTED',
    reference_id BIGINT NULL,
    reference_type VARCHAR(50) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_org_read (organization_id, is_read),
    INDEX idx_notifications_created (created_at DESC),
    UNIQUE KEY uq_notif_reference (organization_id, reference_type, reference_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 10. Resource Utilization Snapshots (Milestone 8)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resource_utilization_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    cloud_account_id BIGINT NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    resource_name VARCHAR(255) NULL,
    resource_type VARCHAR(100) NOT NULL,
    region VARCHAR(50) NULL,
    cpu_utilization_avg DECIMAL(5, 2) NULL,
    cpu_utilization_max DECIMAL(5, 2) NULL,
    memory_utilization_avg DECIMAL(5, 2) NULL,
    memory_utilization_max DECIMAL(5, 2) NULL,
    running_hours DECIMAL(7, 2) NOT NULL DEFAULT 730.00,
    snapshot_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (cloud_account_id) REFERENCES cloud_accounts(id) ON DELETE CASCADE,
    INDEX idx_util_org_date (organization_id, snapshot_date DESC),
    INDEX idx_util_org_resource (organization_id, resource_id),
    INDEX idx_util_org_account (organization_id, cloud_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 11. Cost Optimization Recommendations (Milestone 8)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS optimization_recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    cloud_account_id BIGINT NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    resource_name VARCHAR(255) NULL,
    resource_type VARCHAR(100) NOT NULL,
    cloud_provider VARCHAR(20) NOT NULL,
    recommendation_type ENUM('RIGHTSIZING', 'IDLE_RESOURCE', 'COMMITMENT_SAVINGS_PLAN', 'COMMITMENT_RESERVED_INSTANCE', 'MODERNIZATION') NOT NULL,
    current_config VARCHAR(255) NOT NULL,
    suggested_config VARCHAR(255) NOT NULL,
    priority ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'MEDIUM',
    status ENUM('NEW', 'REVIEWED', 'APPROVED', 'IMPLEMENTED', 'REJECTED') NOT NULL DEFAULT 'NEW',
    confidence_level ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'MEDIUM',
    reason TEXT NOT NULL,
    current_monthly_cost DECIMAL(14, 4) NOT NULL,
    recommended_monthly_cost DECIMAL(14, 4) NOT NULL,
    estimated_monthly_savings DECIMAL(14, 4) NOT NULL,
    estimated_annual_savings DECIMAL(14, 4) NOT NULL,
    realized_monthly_savings DECIMAL(14, 4) NOT NULL DEFAULT 0.0000,
    realized_annual_savings DECIMAL(14, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    implementation_notes TEXT NULL,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (cloud_account_id) REFERENCES cloud_accounts(id) ON DELETE CASCADE,
    INDEX idx_opt_org_status (organization_id, status),
    INDEX idx_opt_org_priority (organization_id, priority),
    INDEX idx_opt_org_type (organization_id, recommendation_type),
    INDEX idx_opt_org_savings (organization_id, estimated_monthly_savings DESC),
    INDEX idx_opt_dedup (organization_id, cloud_account_id, recommendation_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 12. Optimization Recommendation History (Milestone 8)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS optimization_recommendation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recommendation_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    action_by_user_id BIGINT NULL,
    action_by_user_email VARCHAR(120) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recommendation_id) REFERENCES optimization_recommendations(id) ON DELETE CASCADE,
    FOREIGN KEY (action_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_opt_hist_rec (recommendation_id),
    INDEX idx_opt_hist_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 13. Generated Reports (Milestone 9)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS generated_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    report_type VARCHAR(60) NOT NULL,
    file_format VARCHAR(10) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    generated_by_user_id BIGINT NULL,
    generated_by_user_email VARCHAR(180) NOT NULL,
    generation_status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    error_message TEXT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    provider VARCHAR(20) NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (generated_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_reports_org_created (organization_id, created_at DESC),
    INDEX idx_reports_org_type (organization_id, report_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 14. Organization Invitations (Milestone 9)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organization_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    email VARCHAR(180) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    invited_by_user_id BIGINT NULL,
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (invited_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_invitations_org_status (organization_id, status),
    INDEX idx_invitations_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 15. Enterprise Audit Logs (Milestone 9)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    user_email VARCHAR(180) NOT NULL,
    action_type VARCHAR(60) NOT NULL,
    resource_type VARCHAR(60) NOT NULL,
    resource_id VARCHAR(255) NULL,
    ip_address VARCHAR(64) NULL,
    details TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_org_created (organization_id, created_at DESC),
    INDEX idx_audit_org_action (organization_id, action_type),
    INDEX idx_audit_org_resource (organization_id, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
