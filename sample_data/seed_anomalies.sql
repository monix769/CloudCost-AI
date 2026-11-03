USE cloudcost_ai;

INSERT INTO cost_anomalies (organization_id, cloud_account_id, service_name, expected_cost, actual_cost, deviation_percentage, severity, detected_at, status, root_cause_summary)
VALUES 
(1, 2, 'BigQuery', 180.0000, 710.2000, 294.55, 'CRITICAL', NOW() - INTERVAL 1 DAY, 'OPEN', 'Unpartitioned query on raw logs scanned 22TB of analytical data in under 4 hours.'),
(1, 3, 'Virtual Machines', 950.0000, 1910.5000, 101.11, 'HIGH', NOW() - INTERVAL 3 DAY, 'OPEN', 'GPU compute node autoscaling pool failed to terminate after scheduled training pipeline.')
ON DUPLICATE KEY UPDATE deviation_percentage=VALUES(deviation_percentage);
