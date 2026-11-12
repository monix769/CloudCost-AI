-- ===================================================================
-- CloudCost AI - Milestone 8 Database Migration Script
-- Automated Cost Optimization & Rightsizing Engine
-- ===================================================================

USE cloudcost_ai;

-- -------------------------------------------------------------------
-- 1. Resource Utilization Snapshots Table
-- Telemetry cache for CPU, memory, running hours from cloud providers
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
-- 2. Optimization Recommendations Table
-- Stores rightsizing, idle resources, commitment recommendations
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
-- 3. Optimization Recommendation History Table
-- Audit log of status transitions and implementation notes
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

