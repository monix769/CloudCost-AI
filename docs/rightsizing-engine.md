# Rightsizing & Idle Resource Engine Technical Specification

## 1. Architectural Scope
The Rightsizing & Idle Resource Engine operates as a subcomponent of CloudCost AI's FinOps suite, designed to analyze compute infrastructure and telemetry data, applying deterministic rules to downsize oversized instances and decommission idle resources.

---

## 2. Rightsizing Downsizing Matrix

The engine maintains a verified mapping of compute instance families across cloud providers:

### AWS Compute & Database Families
| Current Instance Tier | Target Recommended Tier | vCPU Delta | Memory Delta | Est. Savings Ratio |
| :--- | :--- | :--- | :--- | :--- |
| `c5.4xlarge` | `c5.2xlarge` | $16 \to 8$ | $32 \text{ GiB} \to 16 \text{ GiB}$ | $50.0\%$ |
| `c5.2xlarge` | `c5.xlarge` | $8 \to 4$ | $16 \text{ GiB} \to 8 \text{ GiB}$ | $50.0\%$ |
| `c5.xlarge` | `c5.large` | $4 \to 2$ | $8 \text{ GiB} \to 4 \text{ GiB}$ | $50.0\%$ |
| `m5.4xlarge` | `m5.2xlarge` | $16 \to 8$ | $64 \text{ GiB} \to 32 \text{ GiB}$ | $50.0\%$ |
| `m5.2xlarge` | `m5.xlarge` | $8 \to 4$ | $32 \text{ GiB} \to 16 \text{ GiB}$ | $50.0\%$ |
| `m5.xlarge` | `m5.large` | $4 \to 2$ | $16 \text{ GiB} \to 8 \text{ GiB}$ | $50.0\%$ |
| `t3.2xlarge` | `t3.xlarge` | $8 \to 4$ | $32 \text{ GiB} \to 16 \text{ GiB}$ | $50.0\%$ |
| `t3.xlarge` | `t3.large` | $4 \to 2$ | $16 \text{ GiB} \to 8 \text{ GiB}$ | $50.0\%$ |
| `t3.large` | `t3.medium` | $2 \to 2$ | $8 \text{ GiB} \to 4 \text{ GiB}$ | $50.0\%$ |
| `db.m5.2xlarge` | `db.m5.xlarge` | $8 \to 4$ | $32 \text{ GiB} \to 16 \text{ GiB}$ | $50.0\%$ |

### Azure Virtual Machine Families
| Current VM Tier | Target Recommended Tier | vCPU Delta | Memory Delta | Est. Savings Ratio |
| :--- | :--- | :--- | :--- | :--- |
| `Standard_D8s_v5` | `Standard_D4s_v5` | $8 \to 4$ | $32 \text{ GiB} \to 16 \text{ GiB}$ | $50.0\%$ |
| `Standard_D4s_v5` | `Standard_D2s_v5` | $4 \to 2$ | $16 \text{ GiB} \to 8 \text{ GiB}$ | $50.0\%$ |
| `Standard_E4s_v5` | `Standard_E2s_v5` | $4 \to 2$ | $32 \text{ GiB} \to 16 \text{ GiB}$ | $50.0\%$ |
| `Standard_B4ms` | `Standard_B2ms` | $4 \to 2$ | $16 \text{ GiB} \to 8 \text{ GiB}$ | $50.0\%$ |

### GCP Compute Engine Families
| Current Machine Type | Target Recommended Tier | vCPU Delta | Memory Delta | Est. Savings Ratio |
| :--- | :--- | :--- | :--- | :--- |
| `n2-standard-8` | `n2-standard-4` | $8 \to 4$ | $32 \text{ GiB} \to 16 \text{ GiB}$ | $50.0\%$ |
| `n2-standard-4` | `n2-standard-2` | $4 \to 2$ | $16 \text{ GiB} \to 8 \text{ GiB}$ | $50.0\%$ |
| `e2-standard-8` | `e2-standard-4` | $8 \to 4$ | $32 \text{ GiB} \to 16 \text{ GiB}$ | $50.0\%$ |
| `e2-standard-4` | `e2-standard-2` | $4 \to 2$ | $16 \text{ GiB} \to 8 \text{ GiB}$ | $50.0\%$ |

---

## 3. Idle Resource Rules

1. **Unattached Storage Disks**:
   - Condition: Resource type matches `VOLUME`, `DISK`, or `EBS` AND `runningHours == 0.00`.
   - Action: Take a recovery snapshot and terminate the volume.
   - Cost Recovery: $100\%$ of allocated monthly storage cost.

2. **Idle Load Balancers**:
   - Condition: Resource type matches `LOAD_BALANCER`, `ELB`, `ALB` AND average requests/connections is zero.
   - Action: Decommission idle load balancer.
   - Cost Recovery: $100\%$ of base hourly balancer charge ($\sim \$22.50/\text{mo}$).

3. **Abandoned Compute Instances**:
   - Condition: $\text{CPU}_{\text{avg}} < 2.50\%$ AND $\text{CPU}_{\text{max}} < 5.00\%$ over evaluated period.
   - Action: Stop instance or schedule automated cron shutdown.
   - Cost Recovery: $\sim 90\%$ of compute costs (preserving attached boot volume).

---

## 4. Telemetry Verification & Safety
When reviewing resources:
- Resources without monitored CPU/memory telemetry are never subjected to blind downsizing guesses.
- The UI and API clearly label these instances with `telemetryStatus: MONITORING_REQUIRED`.

