
-- ===================================================================
-- CloudCost AI - Milestone 9 Database Migration Script
-- Enterprise Reports, Organization Management, & Audit Logging
-- ===================================================================

USE cloudcost_ai;

-- -------------------------------------------------------------------
-- 1. Generated Reports Table
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (generated_by_user_id)
        REFERENCES users(id) ON DELETE SET NULL,

    INDEX idx_reports_org_created (organization_id, created_at DESC),
    INDEX idx_reports_org_type (organization_id, report_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


-- -------------------------------------------------------------------
-- 2. Organization Invitations Table
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (invited_by_user_id)
        REFERENCES users(id) ON DELETE SET NULL,

    INDEX idx_invitations_org_status (organization_id, status),
    INDEX idx_invitations_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;


-- -------------------------------------------------------------------
-- 3. Enterprise Audit Logs Table
-- Append-only audit trail
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

    FOREIGN KEY (organization_id)
        REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL,

    INDEX idx_audit_org_created (organization_id, created_at DESC),
    INDEX idx_audit_org_action (organization_id, action_type),
    INDEX idx_audit_org_resource (organization_id, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
