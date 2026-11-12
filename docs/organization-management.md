# Enterprise Organization & Member Management

## 1. Multi-Tenant Architecture

CloudCost AI enforces strict organization-level tenant isolation across all database operations and REST endpoints. Every billing record, cloud account, budget, anomaly, optimization recommendation, generated report, and audit log belongs exclusively to a single `Organization`.

---

## 2. Roles & Permissions (RBAC)

The platform enforces Role-Based Access Control via Spring Security `@PreAuthorize("hasRole('ADMIN')")`:

| Capability | Role: `ADMIN` | Role: `USER` |
| :--- | :---: | :---: |
| View Dashboards & FinOps Analytics | :white_check_mark: | :white_check_mark: |
| View Billing & Cost Data | :white_check_mark: | :white_check_mark: |
| Generate & Download Reports | :white_check_mark: | :white_check_mark: |
| View Anomaly Flags & Optimizations | :white_check_mark: | :white_check_mark: |
| Modify Organization Legal Name | :white_check_mark: | :x: (403 Forbidden) |
| Invite New Team Members | :white_check_mark: | :x: (403 Forbidden) |
| Change Member Roles (Promote/Demote) | :white_check_mark: | :x: (403 Forbidden) |
| Activate / Deactivate Member Accounts | :white_check_mark: | :x: (403 Forbidden) |
| Remove Member Accounts | :white_check_mark: | :x: (403 Forbidden) |
| View Enterprise Audit Trail | :white_check_mark: | :x: (403 Forbidden) |

---

## 3. Administrative Guardrails & Protections

To protect organizations from accidental administrative lockouts or orphaned tenant states, the `OrganizationManagementService` enforces critical safety invariants:

### 3.1 Sole Admin Demotion Protection
An Admin cannot demote their own role or another Admin's role to `USER` if they are the sole remaining active administrator in the organization:
```java
if (currentRole == UserRole.ADMIN && newRole == UserRole.USER) {
    long adminCount = userRepository.countByOrganizationIdAndRoleAndIsActiveTrue(orgId, UserRole.ADMIN);
    if (adminCount <= 1) {
        throw new IllegalStateException("Cannot demote the sole active administrator of this organization.");
    }
}
```

### 3.2 Self-Deactivation Protection
An Admin cannot deactivate their own active user account:
```java
if (currentUser.getId().equals(targetMember.getId()) && Boolean.FALSE.equals(request.getIsActive())) {
    throw new IllegalStateException("Users cannot deactivate their own account.");
}
```

### 3.3 Self-Removal Protection
An Admin cannot remove themselves from the organization. A second Admin must perform account removals or account ownership transfer.

---

## 4. Team Member Invitation Workflow

1. **Admin Dispatch**: An Admin enters a corporate email address and role (`ADMIN` or `USER`) via `POST /api/organizations/current/invitations`.
2. **Token Generation**: A cryptographically random UUID token is generated and persisted in `organization_invitations` with a 7-day expiration window.
3. **Invitation State**: The invitation is tracked as `PENDING`.
4. **Revocation**: The Admin can revoke a pending invitation at any time via `DELETE /api/organizations/current/invitations/{id}`.

---

## 5. REST API Reference

| Method | Endpoint | RBAC | Description |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/organizations/current` | Any User | View tenant profile & member count |
| `PUT` | `/api/organizations/current` | Admin Only | Rename tenant organization |
| `GET` | `/api/organizations/current/members` | Any User | List all organization members |
| `PUT` | `/api/organizations/current/members/{id}` | Admin Only | Modify member role or active status |
| `DELETE` | `/api/organizations/current/members/{id}` | Admin Only | Remove member from organization |
| `POST` | `/api/organizations/current/invitations` | Admin Only | Issue invitation to new member |
| `GET` | `/api/organizations/current/invitations` | Admin Only | List pending/historical invitations |
| `DELETE` | `/api/organizations/current/invitations/{id}` | Admin Only | Revoke pending invitation |

