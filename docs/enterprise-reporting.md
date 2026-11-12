# Enterprise Reporting Engine Architecture

## 1. Overview & Objectives

The CloudCost AI Enterprise Reporting Engine generates boardroom-grade financial statements, executive briefs, and granular audit-ready exports directly from real multi-cloud billing telemetry, anomaly detection logs, budget utilization tables, and rightsizing optimization models.

Every report is securely generated, cataloged with immutable metadata in MySQL, stored in isolated tenant filesystem directories, and streamed through authenticated REST endpoints with download auditing and directory traversal defenses.

---

## 2. Supported Formats & Technology Stack

| Format | Technology / Library | Standard Compliance | Primary Target Audience |
| :--- | :--- | :--- | :--- |
| **PDF** | OpenPDF (`com.github.librepdf:openpdf:1.3.40`) | PDF/A-compliant vector tables, headers, footers | CFOs, CTOs, Finance Committees, Board of Directors |
| **CSV** | Apache Commons CSV (`org.apache.commons:commons-csv:1.10.0`) | RFC-4180 strict comma-separated values | Data Scientists, FinOps Analysts, Excel / PowerBI Modeling |

---

## 3. Supported Report Categories

The reporting engine supports 8 specialized FinOps categories:

1. **Executive FinOps Summary (`EXECUTIVE_SUMMARY`)**:
   - Executive header, total spending, active provider distribution, month-over-month trajectory, active anomaly count, and potential annual rightsizing savings.
2. **Cloud Spending Report (`CLOUD_SPENDING`)**:
   - Detailed cost breakdown aggregated across services, cloud providers, and billing accounts for the selected reporting period.
3. **Monthly Cost Analysis (`MONTHLY_COST_ANALYSIS`)**:
   - Historical monthly spending velocity, MoM variance commentary, and highest-growth cost vectors.
4. **Provider Comparison Report (`PROVIDER_COMPARISON`)**:
   - Multi-cloud spending share across AWS, Microsoft Azure, and Google Cloud Platform, cost efficiency index, and workload concentration.
5. **Budget Utilization Report (`BUDGET_UTILIZATION`)**:
   - Threshold tracking, current period consumption, burn rates, and financial threshold breach alerts.
6. **Anomaly Detection Report (`ANOMALY_DETECTION`)**:
   - Statistical cost anomalies (Z-Score flags, Moving Average deviations, sudden daily spending spikes) and historical resolution actions.
7. **Cost Optimization Report (`COST_OPTIMIZATION`)**:
   - Idle resource inventory, oversized instance rightsizing recommendations, and actionable monthly savings.
8. **Potential and Realized Savings Report (`SAVINGS_SUMMARY`)**:
   - Implemented FinOps optimizations, realized savings tally, and ongoing optimization pipeline projection.

---

## 4. File Storage & Security Architecture

### 4.1 Tenant-Partitioned Storage
Reports are persisted on disk using organization-scoped directory structures:
```
reports_storage/
└── org_{organizationId}/
    ├── 4a8b1c0e-9d2a-4c91-95fe-a1b2c3d4e5f6.pdf
    └── 7b3c2d1e-8e3b-4d92-86ff-f6e5d4c3b2a1.csv
```

### 4.2 Path Traversal Protection
All file paths are normalized and strictly validated against the base storage root before any filesystem read, stream, or delete operation:
```java
Path baseDir = Paths.get(storageDirectory).toAbsolutePath().normalize();
Path targetFile = Paths.get(report.getFilePath()).toAbsolutePath().normalize();

if (!targetFile.startsWith(baseDir)) {
    throw new SecurityException("Illegal file path traversal detected");
}
```

### 4.3 Download Streaming & Audit Counter
- Files are streamed via Spring's `InputStreamResource` with `MediaType.APPLICATION_PDF` or `text/csv`.
- Every download increments `download_count` in the database and emits an append-only `REPORT_DOWNLOADED` event to the `audit_logs` table.

---

## 5. REST API Specifications

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/reports/generate` | All authenticated users | Requests report generation (PDF or CSV) |
| `GET` | `/api/reports` | All authenticated users | Lists report history for current tenant |
| `GET` | `/api/reports/{id}` | All authenticated users | Retrieves report metadata |
| `GET` | `/api/reports/{id}/download` | All authenticated users | Streams and downloads report file |
| `DELETE` | `/api/reports/{id}` | Admin / Report Author | Deletes report file and metadata |

### Sample Generation Request
```json
POST /api/reports/generate
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "reportType": "EXECUTIVE_SUMMARY",
  "format": "PDF",
  "startDate": "2026-10-15",
  "endDate": "2026-11-12",
  "currency": "USD"
}
```

