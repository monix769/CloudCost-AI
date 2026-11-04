# Billing Ingestion & Idempotency Pipeline – CloudCost AI

CloudCost AI implements an enterprise-grade FinOps data ingestion pipeline designed for **high reliability**, **strict idempotency**, **zero-duplicate persistence**, and **full multi-tenant isolation**.

---

## 1. High-Level Ingestion Pipeline

```
[ Scheduled Cron or Manual "Sync Now" ]
                   │
                   ▼
       [ 1. Concurrency Lock ]
     SyncLockManager.tryLock(id)
        ├── If Locked: Abort (409 Conflict / Skip schedule)
        └── If Acquired: Proceed
                   │
                   ▼
     [ 2. Credential Decryption ]
  CredentialEncryptionService.decrypt()
  (AES-256-GCM authenticated ciphertext with v1 tag)
                   │
                   ▼
    [ 3. Cloud Provider Query SPI ]
  CloudBillingProvider.fetchBillingData()
  (AWS Cost Explorer / Azure Cost Management / GCP BigQuery)
  Normalized into standard NormalizedBillingRecord objects
                   │
                   ▼
    [ 4. Deterministic Fingerprinting ]
  BillingFingerprintGenerator.generate()
  (Stable Provider-native ID or canonical composite SHA-256)
                   │
                   ▼
    [ 5. MySQL Database Persistence ]
  Unique index `uq_billing_record_dedup`
  Concurrent duplicate collision safety (DataIntegrityViolationException)
                   │
                   ▼
    [ 6. Audit Trail Logging ]
  Persist SyncHistory (Fetched, Inserted, Skipped, Timing)
                   │
                   ▼
     [ 7. Concurrency Lock Release ]
  SyncLockManager.unlock(id) in finally block
```

---

## 2. Idempotency & Deterministic Fingerprinting

FinOps ingestion processes the same billing intervals repeatedly to capture final invoice revisions. To prevent duplicate billing entries without data loss, CloudCost AI uses a two-tier idempotency strategy:

### Tier 1: Stable Provider-Native Identifiers
Where available (e.g., Azure Cost Management line-item resource IDs or BigQuery export row identifiers), the provider's stable record ID is sanitized and used directly as `external_record_id`.

### Tier 2: Deterministic SHA-256 Composite Fingerprint
When a single unique record ID is unavailable (such as aggregated AWS Cost Explorer dimension buckets), a deterministic canonical fingerprint is constructed using the following billing dimensions:

```
sha256(
  organizationId + ":" +
  cloudAccountId + ":" +
  provider + ":" +
  serviceName.toLowerCase() + ":" +
  resourceId.toLowerCase() + ":" +
  usageType.toLowerCase() + ":" +
  region.toLowerCase() + ":" +
  billingPeriodStart + ":" +
  billingPeriodEnd + ":" +
  usageQuantity.stripTrailingZeros() + ":" +
  costAmount.stripTrailingZeros()
)
```

The resulting 64-character hexadecimal SHA-256 digest guarantees that identical billing line items will produce identical fingerprints regardless of run frequency.

---

## 3. Database-Level Duplicate Prevention

Application-level checks (e.g. `existsByExternalRecordId`) can experience race conditions during concurrent synchronization. CloudCost AI enforces duplicate prevention directly at the MySQL InnoDB engine level:

```sql
ALTER TABLE billing_records
ADD CONSTRAINT uq_billing_record_dedup
UNIQUE (organization_id, cloud_account_id, external_record_id);
```

### Concurrent Collision Handling
During persistence, `BillingIngestionService` uses transactional isolation and catches `DataIntegrityViolationException`. Any record that triggers a duplicate key collision is safely logged, incremented in `recordsSkipped`, and does not fail the surrounding batch.

---

## 4. Concurrency Locking Architecture

To prevent simultaneous ingestion jobs from colliding on the same cloud account:
- Ingestion operations acquire an exclusive lock via the `SyncLockManager` interface.
- **Single-Instance MVP**: Implemented by `InMemorySyncLockManager` utilizing high-concurrency `ConcurrentHashMap` with atomic operations (`putIfAbsent` and `remove`).
- **Distributed Roadmap**: The `SyncLockManager` interface is fully swappable for a Redis-based distributed lock (`Redlock` or Redis `SET NX PX`) when scaling horizontally across container clusters.

---

## 5. Currency Consistency Verification

FinOps best practices mandate that costs denominated in different currencies must never be aggregated without explicit currency conversion:
- Each billing record stores its currency code (e.g., `USD`, `EUR`, `GBP`).
- Prior to calculating combined cloud spending across providers or services, `CostAnalyticsService` executes a currency consistency check (`findDistinctCurrenciesByOrganizationId`).
- If multiple distinct currencies are detected within an organization without an active exchange rate provider, the analytics service rejects silent aggregation and raises an explicit `IllegalStateException` / FinOps currency disclaimer.

---

## 6. Audit Trail & Sync History

Every synchronization run (manual or scheduled) creates a persistent audit entry in the `sync_history` table:
- `records_fetched`: Total items retrieved from the provider API.
- `records_inserted`: Newly discovered records persisted to MySQL.
- `records_skipped`: Duplicate records safely bypassed via idempotency.
- `sync_start_time` & `sync_end_time`: Execution latency tracking.
- `sync_status`: `SUCCESS` or `FAILED`.
- `error_summary`: Sanitized diagnostic message if failure occurred (secrets purged).
