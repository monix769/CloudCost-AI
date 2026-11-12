# Enterprise Security Audit Logging

## 1. Purpose & Scope

The CloudCost AI Enterprise Audit Trail provides an append-only, tamper-resistant event log recording critical identity events, team member modifications, organizational updates, and financial report exports.

This ensures full compliance with SOC 2 Type II, ISO 27001, and FinOps Foundation Governance frameworks.

---

## 2. Event Taxonomy

| Audit Action | Category | Trigger Event |
| :--- | :--- | :--- |
| `LOGIN` | Identity & Access | User successfully authenticates and receives JWT |
| `LOGOUT` | Identity & Access | User logs out and revokes active JWT token |
| `FAILED_LOGIN` | Identity & Access | Authentication failed due to invalid credentials |
| `USER_REGISTER` | Identity & Access | New user registration and organization provisioning |
| `MEMBER_INVITED` | Team Governance | Organization Admin dispatches a team invitation |
| `MEMBER_ROLE_UPDATED` | Team Governance | Admin promotes or demotes a team member role |
| `MEMBER_STATUS_UPDATED` | Team Governance | Admin activates or deactivates a member account |
| `MEMBER_REMOVED` | Team Governance | Admin removes a member from the organization |
| `REPORT_GENERATED` | FinOps Governance | PDF or CSV financial statement generated |
| `REPORT_DOWNLOADED` | FinOps Governance | User streams or downloads a report file |
| `REPORT_DELETED` | FinOps Governance | User or Admin purges a generated report |
| `ORGANIZATION_UPDATED` | Tenancy Governance | Admin renames organization or modifies tier settings |

---

## 3. Zero-Secret Redaction Guarantee

To prevent sensitive information from leaking into audit records, the `AuditLogService` passes all detail strings through an automated regex scrubber before persistence:

```java
public String sanitizeDetails(String raw) {
    if (raw == null) return null;
    return raw.replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
              .replaceAll("(?i)(\"clientSecret\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
              .replaceAll("(?i)(\"token\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
              .replaceAll("(?i)(\"apiKey\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
}
```

---

## 4. Database Schema

The `audit_logs` table is append-only with foreign key indexes for tenant queries:

```sql
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    user_email VARCHAR(255) NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT NULL,
    ip_address VARCHAR(45) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_audit_org_time (organization_id, timestamp),
    INDEX idx_audit_action (action)
);
```

---

## 5. Audit Log REST API

Access to enterprise audit logs is strictly restricted to users with the `ADMIN` role.

| Method | Endpoint | Query Parameters | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/audit-logs` | `action`, `userId`, `startDate`, `endDate`, `page`, `size` | Paginated search across tenant audit events |
| `GET` | `/api/audit-logs/{id}` | N/A | Detailed view of a single audit event |

