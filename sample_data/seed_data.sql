-- ===================================================================
-- CloudCost AI - Sample Seed Data
-- ===================================================================

USE cloudcost_ai;

-- Organization
INSERT INTO organizations (id, name, slug, tier, is_active)
VALUES (1, 'Acme Cloud Corp', 'acme-corp', 'ENTERPRISE', TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Users
INSERT INTO users (id, organization_id, email, password_hash, first_name, last_name, role, is_active)
VALUES (1, 1, 'admin@cloudcostai.local', '$2a$10$wT0E0v9pG7M1U2bXvX.K0u5t0Wq8jWpS6y0z1z1z1z1z1z1z1z1z', 'Admin', 'User', 'ADMIN', TRUE)
ON DUPLICATE KEY UPDATE email=VALUES(email);

-- Cloud Accounts
INSERT INTO cloud_accounts (id, organization_id, provider, account_id, account_name, sync_status, is_active)
VALUES 
(1, 1, 'AWS', '123456789012', 'Production AWS Account', 'ACTIVE', TRUE),
(2, 1, 'GCP', 'gcp-production-9876', 'Production GCP Project', 'ACTIVE', TRUE),
(3, 1, 'AZURE', 'sub-azure-5544-22', 'Core Infrastructure Azure', 'ACTIVE', TRUE)
ON DUPLICATE KEY UPDATE account_name=VALUES(account_name);

-- Budgets
INSERT INTO budgets (id, organization_id, name, amount_limit, current_spend, currency, period, alert_threshold_percentage, status)
VALUES (1, 1, 'Q4 Total Cloud Spend', 50000.00, 34210.50, 'USD', 'QUARTERLY', 80.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE amount_limit=VALUES(amount_limit);
