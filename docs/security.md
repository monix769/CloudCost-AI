# CloudCost AI – Security Architecture & JWT Authentication Guide

## 1. Security Architecture Overview

CloudCost AI implements an enterprise-grade, stateless security architecture powered by **Spring Security 6**, **JJWT 0.12.6**, and **BCryptPasswordEncoder**. The application strictly adheres to the principle of least privilege, zero-trust tenant isolation, and defense-in-depth API protection.

```
                    ┌────────────────────────────────────────────────────────┐
                    │               Client Request (React 19)                │
                    │         Header: Authorization: Bearer <JWT>            │
                    └───────────────────────────┬────────────────────────────┘
                                                │
                                                ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ Spring Security Filter Chain                                                           │
│                                                                                        │
│  1. CorsFilter ────────────► Enforces whitelist (localhost:5173, etc.)                 │
│  2. CsrfFilter ────────────► Disabled for stateless Bearer REST APIs                   │
│  3. JwtAuthenticationFilter:                                                           │
│       ├─ Extract Bearer token from 'Authorization' header                             │
│       ├─ Validate token not revoked in TokenDenylistService                            │
│       ├─ Verify HMAC-SHA256 signature & expiration date                                │
│       ├─ Extract claims: email (sub), userId, role, organizationId                     │
│       └─ Populate SecurityContextHolder with UserPrincipal & GrantedAuthority          │
│  4. AuthorizationFilter:                                                               │
│       ├─ Public paths (/api/auth/register, /api/auth/login, /api/health) ──► PERMIT    │
│       ├─ Admin paths (/api/admin/**) ──────────────────────────────► hasRole('ADMIN')   │
│       └─ Business REST APIs (/api/cloud-accounts/**, /api/billing/**) ──► AUTHENTICATED│
│  5. Exception Handling:                                                                │
│       ├─ AuthenticationEntryPoint ─────────────────────────────────► 401 Unauthorized │
│       └─ AccessDeniedHandler ──────────────────────────────────────► 403 Forbidden    │
└───────────────────────────────────────────────┬────────────────────────────────────────┘
                                                │
                                                ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ Domain Services & Multi-Tenant Data Scoping                                            │
│                                                                                        │
│  OrganizationService.getCurrentUserOrganization()                                      │
│    └─ Scopes all queries by organization_id = principal.getOrganizationId()             │
│    └─ IDOR Prevention: findByIdAndOrganizationId(id, userOrg.getId())                  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Authentication Flow

### A. User Registration (`POST /api/auth/register`)
1. Validates all input fields via Jakarta Bean Validation (`@Valid`).
2. Validates email format and uniqueness: returns `409 Conflict` if duplicate.
3. Enforces password strength regex: minimum 8 characters, at least 1 uppercase letter, 1 lowercase letter, and 1 numeric digit.
4. Salting and hashing: Uses `BCryptPasswordEncoder` (10 rounds). Passwords are never stored or logged in plain text.
5. Organization association: Matches existing organization by name or slug, or creates a new tenant without creating duplicate entries.
6. Role assignment: Assigns `ROLE_USER` by default, or `ROLE_ADMIN` if designated or if email belongs to system administrators.
7. Generates signed JWT access token and returns sanitized `UserProfileResponse` (password hash is strictly omitted).

### B. User Login (`POST /api/auth/login`)
1. Accepts normalized email and password.
2. Authenticates through Spring Security's `AuthenticationManager.authenticate(...)`.
3. Handles invalid credentials securely with uniform `401 Unauthorized` ("Invalid email or password.") without exposing whether an email exists.
4. Generates HMAC-SHA256 signed JWT token containing subject, user ID, role authorities, and tenant organization details.
5. Updates the user's `last_login_at` timestamp.

### C. Current User Profile (`GET /api/auth/me`)
1. Requires a valid, unrevoked JWT Bearer token.
2. Extracts authenticated `UserPrincipal` from `SecurityContextHolder`.
3. Returns current user details, role, and organization tenant metadata.

### D. Logout & Token Revocation (`POST /api/auth/logout`)
1. Extracts Bearer token from the incoming `Authorization` header.
2. Calculates remaining token time-to-live (TTL) from JWT claims.
3. Inserts token into `TokenDenylistService` backed by thread-safe `ConcurrentHashMap`.
4. Clears `SecurityContextHolder`.
5. Subsequent requests presenting the revoked token are immediately rejected with `401 Unauthorized`.
6. An automated background scheduled task (`@Scheduled`) purges expired tokens from the cache every 15 minutes to guarantee bounded memory consumption.

---

## 3. JWT Token Specification

| Property | Details |
| :--- | :--- |
| **Algorithm** | HMAC-SHA256 (`HS256`) via JJWT 0.12.6 |
| **Signing Key** | Minimum 256-bit secret key read from environment variable `JWT_SECRET` |
| **Default Validity** | 24 Hours (86,400,000 ms), configurable via `JWT_EXPIRATION_MS` |
| **Standard Header** | `Authorization: Bearer <TOKEN>` |

### Claims Payload
```json
{
  "sub": "jane.doe@example.com",
  "userId": 5,
  "role": "ROLE_USER",
  "roles": "ROLE_USER",
  "organizationId": 2,
  "organizationName": "Acme Cloud Corp",
  "iat": 1730728800,
  "exp": 1730815200
}
```

---

## 4. Multi-Tenant Data Security & IDOR Prevention

CloudCost AI enforces strict organization-level tenant isolation:
- Every authenticated user belongs to an `Organization`.
- All domain operations (`CloudAccountService`, `BillingService`, `BudgetService`, `AnomalyService`, `CostAnalyticsService`) resolve the caller's organization context dynamically via `OrganizationService.getCurrentUserOrganization()`.
- **Read Isolation**: Queries strictly filter by `organization_id = :orgId`. A user from Organization A can never see accounts, budgets, billing records, or anomalies from Organization B.
- **Write / Delete Isolation**: Entity operations use `findByIdAndOrganizationId(id, org.getId())`. Attempting to access an entity belonging to another tenant produces an immediate `404 Not Found` (`ResourceNotFoundException`), preventing Insecure Direct Object Reference (IDOR) exploits.
- **CSV Ingestion Isolation**: Referenced or newly registered cloud accounts during billing CSV uploads are strictly bounded to the uploading user's organization.

---

## 5. Role-Based Access Control (RBAC)

CloudCost AI defines two initial authorization roles:
- `ROLE_USER`: Standard FinOps team member. Accesses dashboards, cloud accounts, billing records, budgets, cost analytics, and anomalies within their organization.
- `ROLE_ADMIN`: Administrative user. Possesses all `ROLE_USER` permissions plus organization user governance via `/api/admin/users` and elevated tenant settings.

Enforcement mechanisms:
- URL-level rule: `.requestMatchers("/api/admin/**").hasRole("ADMIN")` in `SecurityConfig`.
- Method-level annotation: `@PreAuthorize("hasRole('ADMIN')")` on sensitive controller operations.
- Unauthorized users receive HTTP `403 Forbidden` with standardized `ErrorResponse`.

---

## 6. Public vs. Protected Endpoints

| Endpoint | Method | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `/api/auth/register` | `POST` | Public | Register new user and organization |
| `/api/auth/login` | `POST` | Public | Authenticate user credentials and receive JWT |
| `/api/health` | `GET` | Public | Operational health check (Spring + MySQL) |
| `/api/status` | `GET` | Public | System runtime diagnostics |
| `/swagger-ui/**` | `GET` | Public | Interactive OpenAPI documentation |
| `/v3/api-docs/**` | `GET` | Public | OpenAPI v3 specification JSON |
| `/api/auth/me` | `GET` | `AUTHENTICATED` | Fetch currently authenticated user profile |
| `/api/auth/logout` | `POST` | `AUTHENTICATED` | Revoke active JWT token |
| `/api/cloud-accounts/**` | ALL | `AUTHENTICATED` | Multi-tenant cloud account operations |
| `/api/billing/**` | ALL | `AUTHENTICATED` | Multi-tenant billing record & CSV operations |
| `/api/costs/**` | ALL | `AUTHENTICATED` | Multi-tenant cost analytics & aggregates |
| `/api/budgets/**` | ALL | `AUTHENTICATED` | Multi-tenant budget guardrails |
| `/api/admin/**` | ALL | `ROLE_ADMIN` | Organization administration & user roster |

---

## 7. Cloud Credential Encryption & Key Management (Milestone 5)

CloudCost AI enforces strict zero-knowledge security for external cloud provider credentials (AWS Access Keys, Azure Client Secrets, GCP Service Account JSON keys).

### A. Authenticated Encryption Algorithm (AES-256-GCM)
- **Algorithm**: `AES/GCM/NoPadding`
- **Key Length**: 256 bits (32 bytes)
- **Initialization Vector (IV)**: 96-bit (12 bytes) cryptographically secure random IV generated per encryption event using `SecureRandom`. IVs are never reused.
- **Authentication Tag**: 128-bit authentication tag ensuring both confidentiality and cryptographic integrity against tampering or bit-flipping attacks.
- **Ciphertext Storage Format**:
  ```
  v1:<base64-encoded-96bit-iv>:<base64-encoded-ciphertext-and-tag>
  ```

### B. Startup Key Validation & Self-Test
During application startup (`@PostConstruct`), `CredentialEncryptionService`:
1. Validates that `CLOUD_CREDENTIAL_ENCRYPTION_KEY` is configured and decodes to exactly 256 bits (32 bytes).
2. Executes an internal cryptographic round-trip self-test (encrypting and decrypting a synthetic canary payload).
3. If the key is invalid or the self-test fails, the Spring ApplicationContext terminates immediately with a fatal exit code, preventing the application from running in an insecure state.

### C. Zero-Exposure Policy
- **Logs**: `ProviderCredentials.toString()` automatically masks all sensitive fields (`AKIA****12`).
- **REST DTOs**: `CloudAccountResponse` exposes only metadata (`credentialsConfigured: true`, `keyVersion: "v1"`). Raw secrets and encrypted blobs are never serialized into API responses.
- **Error Handlers**: Cryptographic exceptions are caught centrally; internal cipher details or stack traces are never surfaced to client responses.

### D. Key Rotation Strategy
- All encrypted records store their active key version in `cloud_accounts.key_version` (default: `v1`).
- When rotating keys:
  1. The new key version (e.g. `v2`) is introduced in the key management vault.
  2. Legacy records continue decrypting using the key corresponding to their stored `key_version`.
  3. A background migration job or on-access re-encryption reads with the legacy key and immediately re-encrypts using `v2`, updating `key_version` in MySQL without service disruption.

