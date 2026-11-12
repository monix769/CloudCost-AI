-- ===================================================================
-- CloudCost AI - Milestone 7 Database Migration Script
-- Real-Time Cost Anomaly Detection Engine & Notification Subsystem
-- ===================================================================

USE cloudcost_ai;

-- -------------------------------------------------------------------
-- 1. Extend cost_anomalies Table with Milestone 7 Dimensions
-- -------------------------------------------------------------------
-- Add excess_cost, detection_method, region, recommended_action, 
-- period bounds, and deduplication dimension key
ALTER TABLE cost_anomalies
    ADD COLUMN IF NOT EXISTS excess_cost DECIMAL(14, 4) NOT NULL DEFAULT 0.0000 AFTER actual_cost,
    ADD COLUMN IF NOT EXISTS detection_method VARCHAR(50) NOT NULL DEFAULT 'Z_SCORE' AFTER severity,
    ADD COLUMN IF NOT EXISTS region VARCHAR(50) NULL AFTER service_name,
    ADD COLUMN IF NOT EXISTS recommended_action TEXT NULL AFTER root_cause_summary,
    ADD COLUMN IF NOT EXISTS anomaly_period_start DATE NULL AFTER detected_at,
    ADD COLUMN IF NOT EXISTS anomaly_period_end DATE NULL AFTER anomaly_period_start,
    ADD COLUMN IF NOT EXISTS dimension_key VARCHAR(255) NULL AFTER anomaly_period_end;

-- Expand status enum to support DISMISSED alongside legacy IGNORED
ALTER TABLE cost_anomalies
    MODIFY COLUMN status ENUM('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'IGNORED', 'DISMISSED') NOT NULL DEFAULT 'OPEN';

-- Add composite indexes for high-throughput anomaly querying and deduplication
CREATE INDEX IF NOT EXISTS idx_anomalies_org_status 
    ON cost_anomalies (organization_id, status);

CREATE INDEX IF NOT EXISTS idx_anomalies_org_detected 
    ON cost_anomalies (organization_id, detected_at DESC);

CREATE INDEX IF NOT EXISTS idx_anomalies_dedup 
    ON cost_anomalies (organization_id, detection_method, dimension_key, anomaly_period_start);

-- -------------------------------------------------------------------
-- 2. Cost Anomaly Status Transition History Table
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
-- 3. Tenant-Level Anomaly Detection Configuration Table
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
-- 4. In-App Notifications Subsystem Table
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

