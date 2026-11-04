-- ===================================================================
-- CloudCost AI - Milestone 5 Schema Migration
-- Multi-Cloud Provider Ingestion & Real-Time Billing
-- MySQL 8.0+ Compatibility
-- ===================================================================

USE cloudcost_ai;

-- 1. Augment cloud_accounts table with credential & connection state
SET @dbname = DATABASE();

-- Add encrypted_credentials column if not exists
SET @tablename = "cloud_accounts";
SET @columnname = "encrypted_credentials";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " TEXT NULL AFTER arn_or_client_id;")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add connection_status column if not exists
SET @columnname = "connection_status";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(50) NOT NULL DEFAULT 'DISCONNECTED' AFTER sync_status;")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add key_version column if not exists
SET @columnname = "key_version";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(20) NOT NULL DEFAULT 'v1' AFTER connection_status;")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2. Augment billing_records table with source_type and external_record_id
SET @tablename = "billing_records";

-- Add source_type column if not exists
SET @columnname = "source_type";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(30) NOT NULL DEFAULT 'MANUAL' AFTER tags;")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add external_record_id column if not exists
SET @columnname = "external_record_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(255) NULL AFTER source_type;")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add unique constraint for duplicate prevention at the database level if not exists
SET @indexname = "uq_billing_record_dedup";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD CONSTRAINT ", @indexname, " UNIQUE KEY (organization_id, cloud_account_id, external_record_id);")
));
PREPARE addIndexIfNotExists FROM @preparedStatement;
EXECUTE addIndexIfNotExists;
DEALLOCATE PREPARE addIndexIfNotExists;

-- 3. Create sync_history table for auditing and tracking provider synchronizations
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
