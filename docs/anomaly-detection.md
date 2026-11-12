# Real-Time Cost Anomaly Detection Engine – CloudCost AI

## 1. Overview & Architectural Philosophy

Milestone 7 introduces the **Real-Time Cost Anomaly Detection Engine** (`com.cloudcostai.anomaly`), an enterprise-grade financial surveillance and workload monitoring subsystem engineered in native **Java 21** and **Spring Boot 3.3.5**.

Rather than relying on opaque third-party AI APIs or fragile ad-hoc scripts, CloudCost AI executes deterministic statistical algorithms directly against verified multi-cloud billing records stored in MySQL 8+.

```
               ┌────────────────────────────────────────────────────────┐
               │              CloudCost AI Anomaly Engine               │
               └───────────────────────────┬────────────────────────────┘
                                           │
                  ┌────────────────────────┴────────────────────────┐
                  ▼                                                 ▼
      [ Automatic Scheduler ]                            [ On-Demand REST API ]
   AnomalyDetectionScheduler                          POST /api/anomalies/detect
   (@Scheduled fixedDelay=1h)                         (FinOps Operator Trigger)
                  │                                                 │
                  └────────────────────────┬────────────────────────┘
                                           │
                                           ▼
                     ┌───────────────────────────────────────────┐
                     │       AnomalyDetectionLockManager         │
                     │  (Atomic per-organization concurrency lock)│
                     └─────────────────────┬─────────────────────┘
                                           │
                                           ▼
                     ┌───────────────────────────────────────────┐
                     │      AnomalyDetectionEngineService        │
                     └─────────────────────┬─────────────────────┘
                                           │
         ┌───────────────────┬─────────────┴─────┬───────────────────┐
         ▼                   ▼                   ▼                   ▼
 ┌───────────────┐   ┌───────────────┐   ┌───────────────┐   ┌───────────────┐
 │    Z-Score    │   │Moving Average │   │Spending Spike │   │ Budget-Based  │
 │   Detector    │   │  Deviation    │   │   Detector    │   │   Detector    │
 │ (30-day norm) │   │ (14-day MA_k) │   │ (Daily surge) │   │(Threshold 90%)│
 └───────┬───────┘   └───────┬───────┘   └───────┬───────┘   └───────┬───────┘
         │                   │                   │                   │
         └───────────────────┴─────────────┬─────┴───────────────────┘
                                           ▼
                     ┌───────────────────────────────────────────┐
                     │      Deduplication & Persistence Guard    │
                     │ - Prevents duplicate active alerts        │
                     │ - Computes Excess Cost & Deviation %      │
                     │ - Records CostAnomalyHistory Audit Event  │
                     └─────────────────────┬─────────────────────┘
                                           │
                                           ▼
                     ┌───────────────────────────────────────────┐
                     │        In-App Notification Dispatch       │
                     │ - Dispatches for CRITICAL & HIGH severities│
                     │ - Updates unread header counter badge     │
                     └───────────────────────────────────────────┘
```

---

## 2. Statistical Anomaly Detection Methodologies

### 2.1 Z-Score Statistical Detection (`Z_SCORE`)
The Z-score detector evaluates historical cost observations to identify spending points that deviate significantly from standard distribution parameters.

#### Mathematical Formulation
Let $y_1, y_2, \dots, y_n$ denote historical daily expenditures for an account workload (minimum $n \ge 4$ days):
$$\mu = \frac{1}{n} \sum_{i=1}^n y_i$$
$$\sigma = \sqrt{\frac{1}{n-1} \sum_{i=1}^n (y_i - \mu)^2}$$
For current target day spend $y_t$:
$$Z = \frac{y_t - \mu}{\sigma} \quad (\sigma > 0)$$

#### Safe Zero-Variance Handling
If $\sigma = 0$ (constant flat spending):
- If $y_t = \mu \Rightarrow Z = 0.0$ (normal baseline).
- If $y_t > \mu$ with $\mu > 0$, the detector calculates relative ratio $\frac{y_t - \mu}{\mu} \times 3.0$ to prevent `ArithmeticException` while capturing genuine surges.

#### Severity & Thresholds
- **Trigger**: $Z \ge Z_{\text{thresh}}$ (configurable, default: $2.50$) and $(y_t - \mu) \ge \$15.00$.
- **Severity Mapping**:
  - $Z \ge 4.00 \Rightarrow \text{CRITICAL}$
  - $3.00 \le Z < 4.00 \Rightarrow \text{HIGH}$
  - $2.50 \le Z < 3.00 \Rightarrow \text{MEDIUM}$
  - $2.00 \le Z < 2.50 \Rightarrow \text{LOW}$

---

### 2.2 Moving Average Deviation (`MOVING_AVERAGE`)
Compares target daily expenditure against an unweighted sliding rolling average over window $k$ (default: $k = 14$ days, minimum $n \ge 3$).

#### Mathematical Formulation
$$\text{MA}_k = \frac{1}{k} \sum_{i=1}^k y_{t-i}$$
$$\% \Delta = \left( \frac{y_t - \text{MA}_k}{\text{MA}_k} \right) \times 100$$
$$\text{Excess Cost} = y_t - \text{MA}_k$$

#### Thresholds & Severity
- **Trigger**: $\% \Delta \ge \text{threshold}$ (default: $30.00\%$) and $\text{Excess Cost} \ge \$20.00$.
- **Severity Mapping**:
  - $\% \Delta \ge 150.0\% \Rightarrow \text{CRITICAL}$
  - $75.0\% \le \% \Delta < 150.0\% \Rightarrow \text{HIGH}$
  - $40.0\% \le \% \Delta < 75.0\% \Rightarrow \text{MEDIUM}$
  - $30.0\% \le \% \Delta < 40.0\% \Rightarrow \text{LOW}$

---

### 2.3 Sudden Spending Spike Detection (`SPENDING_SPIKE`)
Focuses on sudden overnight or daily spending surges compared to immediate prior day expenditure ($y_{t-1}$).

#### Mathematical Formulation
$$\% \text{Spike} = \left( \frac{y_t - y_{t-1}}{y_{t-1}} \right) \times 100$$
$$\text{Excess Cost} = y_t - y_{t-1}$$

#### Thresholds & Severity
- **Trigger**: $\% \text{Spike} \ge 50.00\%$ and $\text{Excess Cost} \ge \$50.00$.
- **Severity Mapping**:
  - $\% \text{Spike} \ge 200.0\% \text{ or } \text{Excess} \ge \$500.00 \Rightarrow \text{CRITICAL}$
  - $\% \text{Spike} \ge 100.0\% \text{ or } \text{Excess} \ge \$150.00 \Rightarrow \text{HIGH}$
  - $\% \text{Spike} \ge 50.0\% \Rightarrow \text{MEDIUM}$

---

### 2.4 Budget-Based Detection (`BUDGET_BASED`)
Integrates directly with the Milestone 6 budget engine, monitoring utilization limits across active budgets:
$$\text{Utilization} = \left( \frac{\text{Current Spend}}{\text{Amount Limit}} \right) \times 100$$

#### Thresholds & Severity
- $\text{Utilization} \ge 100.0\% \Rightarrow \text{CRITICAL}$ ("Budget Limit Breach").
- $90.0\% \le \text{Utilization} < 100.0\% \Rightarrow \text{HIGH}$ ("Budget Near Capacity").

---

## 3. Anomaly Lifecycle & State Transitions

Anomalies transition through four well-defined lifecycle states:

```
                  ┌──────────────────────┐
                  │         OPEN         │
                  └──────────┬───────────┘
                             │
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
   ┌─────────────────┐ ┌───────────┐ ┌─────────────────┐
   │  ACKNOWLEDGED   │ │ RESOLVED  │ │    DISMISSED    │
   └────────┬────────┘ └───────────┘ └─────────────────┘
            │                ▲
            └────────────────┘
```

1. **`OPEN`**: Newly detected anomaly awaiting review.
2. **`ACKNOWLEDGED`**: FinOps engineer has noted the anomaly and is actively investigating root causes.
3. **`RESOLVED`**: Root cause mitigated (e.g. unindexed RDS queries fixed, cluster autoscaling tuned).
4. **`DISMISSED`**: Flagged spend confirmed as intentional (e.g. planned load testing or promotional campaign).

### Transition Audit Trail (`CostAnomalyHistory`)
Every status update persists an audit record capturing:
- `previous_status` $\rightarrow$ `new_status`
- `action_by_user_id` and `action_by_user_email`
- `notes` (operator's explanation)
- `created_at` timestamp

---

## 4. Automatic Scheduling & Concurrency Safety

### Background Scheduling
Configured via `AnomalyDetectionScheduler`:
- Property: `cloudcost.anomaly.schedule.enabled` (default: `true`)
- Property: `cloudcost.anomaly.schedule.fixed-delay-ms` (default: `3600000` / 1 hour)
- Iterates over all active organizations independently to guarantee tenant isolation.

### Concurrency Lock Strategy
`AnomalyDetectionLockManager` guarantees that manual on-demand triggers (`POST /api/anomalies/detect`) and scheduled runs never collide on the same tenant:
- **Single-Instance Deployment**: `InMemoryAnomalyDetectionLockManager` using high-concurrency `ConcurrentHashMap` with atomic `putIfAbsent` and `remove`.
- **Distributed Roadmap**: Swappable for Redis (`Redlock`) or MySQL advisory locks for multi-instance container orchestration.

---

## 5. In-App Notifications Subsystem

1. **Trigger Condition**: When `AnomalyDetectionEngineService` identifies a `CRITICAL` or `HIGH` anomaly, it calls `notificationService.createAnomalyNotification()`.
2. **Deduplication**: Enforced via unique constraint `(organization_id, reference_type, reference_id)`.
3. **Reactive Header Bell**: The frontend queries `GET /api/notifications/unread-count` and displays an animated red counter badge.
4. **Management**: Supports single-item `PATCH /api/notifications/{id}/read` and bulk `POST /api/notifications/read-all`.

---

## 6. REST API Endpoint Specifications

All endpoints require JWT Bearer authentication and enforce tenant isolation:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/anomalies` | List anomalies with filters (severity, status, provider, service) & pagination |
| `GET` | `/api/anomalies/{id}` | Retrieve single anomaly details with root cause & recommendation |
| `GET` | `/api/anomalies/summary` | Anomaly KPI metrics (critical count, open count, total excess spend) |
| `GET` | `/api/anomalies/trends` | Time-series daily anomaly counts and excess cost metrics |
| `POST` | `/api/anomalies/detect` | Manually trigger statistical anomaly scan |
| `PATCH`| `/api/anomalies/{id}/status` | Transition lifecycle state with operator notes |
| `GET` | `/api/anomalies/{id}/history`| Retrieve audit history log of state transitions |
| `GET` | `/api/anomalies/config` | Inspect tenant detection parameters |
| `PUT` | `/api/anomalies/config` | Update Z-score, moving average, and spike thresholds |
| `GET` | `/api/notifications` | List in-app notifications (optional unread-only filter) |
| `GET` | `/api/notifications/unread-count`| Get unread notification count badge |
| `PATCH`| `/api/notifications/{id}/read` | Mark single notification as read |
| `POST` | `/api/notifications/read-all`| Mark all notifications as read for current organization |

