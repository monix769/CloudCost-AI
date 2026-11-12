# Automated Cost Optimization & Rightsizing Engine

## 1. Overview
The **CloudCost AI Cost Optimization & Rightsizing Engine** (Milestone 8) analyzes cloud infrastructure spending and telemetry metrics across AWS, Microsoft Azure, and Google Cloud Platform (GCP). It identifies actionable savings opportunities, quantifies financial impact, and guides engineering and FinOps teams through the recommendation lifecycle.

---

## 2. Core Principles & Safety Guarantees

### Advisory and Non-Destructive
> [!IMPORTANT]
> The engine is strictly advisory. It **never** resizes, shuts down, or terminates cloud resources automatically. All actions require explicit human review and approval through the platform or integrated change-management workflows.

### Telemetry Integrity
- **No Fabricated Metrics**: Billing records alone do not prove CPU or memory utilization. The engine evaluates genuine utilization telemetry captured from provider monitoring APIs (AWS CloudWatch, Azure Monitor, GCP Cloud Monitoring).
- **Monitoring Required Flag**: If utilization metrics are unavailable for an instance, the system explicitly marks the resource as requiring additional telemetry (`MONITORING_REQUIRED`) rather than making an unsupported rightsizing recommendation.

---

## 3. Optimization Engines

### A. Instance Rightsizing Engine
Evaluates monitored compute resources against sustained utilization thresholds:
- **Downsizing Rule**: An instance with 30-day average CPU utilization $< 15.00\%$ and peak CPU $< 40.00\%$ is marked as oversized.
- **Sizing Matrix**: Recommends the immediate next lower instance tier (e.g., `c5.2xlarge` $\to$ `c5.xlarge`, `Standard_D4s_v5` $\to$ `Standard_D2s_v5`, `n2-standard-4` $\to$ `n2-standard-2`).
- **Cost Differential**: Calculates exact monthly savings:
  $$\text{Savings}_{\text{monthly}} = \text{Cost}_{\text{current}} - \text{Cost}_{\text{recommended}}$$
  $$\text{Savings}_{\text{annual}} = \text{Savings}_{\text{monthly}} \times 12$$

### B. Idle Resource Detection Engine
Identifies resources incurring ongoing hourly charges without contributing to active workloads:
1. **Unattached Storage Volumes**: EBS, Azure Managed Disks, and GCP Persistent Disks with $0$ attached compute hours. Recommends taking a recovery snapshot and releasing the volume ($100\%$ savings).
2. **Idle Load Balancers**: Application/Network Load Balancers with zero registered active targets or negligible throughput. Recommends decommissioning ($100\%$ savings).
3. **Idle Compute Instances**: Always-on virtual machines with average CPU $< 2.50\%$ and peak $< 5.00\%$. Recommends scheduled shutdown or instance stop ($\sim 90\%$ savings, retaining root disk).

### C. Commitment Analysis Engine (Savings Plans & RIs)
Models steady-state compute baseline spending across the previous 30 days:
- **Baseline Isolation**: Computes stable continuous daily compute expenditure.
- **Conservative Coverage Target**: Recommends covering $75\%$ of baseline spend to eliminate over-commitment risks.
- **Commitment Terms**:
  - **1-Year Commitment**: Yields $\sim 28\%$ discount against on-demand retail rates.
  - **3-Year Commitment**: Yields $\sim 42\%$ discount against on-demand retail rates.
- **Explicit Assumptions**: All commitment projections state the baseline period, coverage percentage, and no-upfront payment assumptions.

---

## 4. Savings Calculation & Precision

All monetary arithmetic is performed using Java `BigDecimal` with `RoundingMode.HALF_UP` to prevent financial discrepancies:
- **Potential Monthly Savings**: $\sum \text{Estimated Monthly Savings}$ for all open recommendations (`NEW`, `REVIEWED`, `APPROVED`).
- **Projected Annual Savings**: $\text{Potential Monthly Savings} \times 12$.
- **Realized Monthly Savings**: $\sum \text{Realized Monthly Savings}$ for `IMPLEMENTED` recommendations.
- **Savings Percentage**: $\frac{\text{Monthly Savings}}{\text{Current Monthly Cost}} \times 100\%$.

---

## 5. Recommendation Lifecycle Management

Recommendations progress through a formal audit-logged state machine:

```
[ NEW ] ──> [ REVIEWED ] ──> [ APPROVED ] ──> [ IMPLEMENTED ]
   │             │                 │
   └─────────────┴─────────────────┴─────────> [ REJECTED ]
```

- **NEW**: Detected by the optimization scan.
- **REVIEWED**: Inspected by a FinOps analyst or cloud architect.
- **APPROVED**: Queued for execution in the next maintenance window.
- **IMPLEMENTED**: Changes applied in cloud provider; actual realized savings tracked.
- **REJECTED**: Dismissed with mandatory or optional reasoning notes.

Every transition logs an immutable entry in `optimization_recommendation_history` recording the operator, previous state, new state, timestamp, and notes.

---

## 6. Notification Subsystem Integration
When high-impact opportunities are identified:
- Any recommendation classified as **CRITICAL** ($\ge \$250/\text{mo}$ savings) or **HIGH** ($\ge \$75/\text{mo}$ savings) triggers an in-app notification via `NotificationService`.
- Notifications are deduplicated by `(organization_id, 'OPTIMIZATION_RECOMMENDATION', reference_id)` to prevent redundant alerts.

