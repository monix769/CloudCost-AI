# Rule-Based FinOps Insight Engine – CloudCost AI

## 1. Overview & Architectural Philosophy

Milestone 6 introduces the **Rule-Based FinOps Insight Engine** (`com.cloudcostai.analytics.insight`), an automated financial intelligence subsystem engineered in native **Java 21**. 

Rather than relying on black-box, unpredictable LLM inferences or fragile heuristic hacks, the FinOps Insight Engine executes a deterministic battery of financial sanity checks, governance rules, and cost-efficiency audits directly against validated cloud billing records and budget models.

```
                  ┌─────────────────────────────────────┐
                  │        FinOpsInsightService         │
                  └──────────────────┬──────────────────┘
                                     │
      ┌──────────────────────────────┼──────────────────────────────┐
      ▼                              ▼                              ▼
┌───────────────┐              ┌───────────────┐              ┌───────────────┐
│ MoM Growth    │              │ Concentration │              │ Budget & Sync │
│ Rules         │              │ Rules         │              │ Guardrails    │
│ - MoM Surge   │              │ - Service >40%│              │ - Budget >90% │
│ - MoM Drop    │              │ - Provider>80%│              │ - Budget >100%│
└───────┬───────┘              └───────┬───────┘              │ - Stale Sync  │
        │                              │                      └───────┬───────┘
        └──────────────────────────────┼──────────────────────────────┘
                                       ▼
                       ┌───────────────────────────────┐
                       │       List<FinOpsInsight>     │
                       │ - Title & Description         │
                       │ - Severity (INFO/WARN/CRIT)   │
                       │ - Financial Impact ($)        │
                       │ - Actionable Recommendation   │
                       │ - Detection Timestamp (UTC)   │
                       └───────────────────────────────┘
```

---

## 2. Insight Severity Taxonomy

Insights are strictly categorized into three standardized FinOps severity tiers:

| Severity Level | Color Coding | Threshold / Context | FinOps Action Requirement |
|---|---|---|---|
| `INFO` | Blue (`#3B82F6`) | Favorable spending reductions, moderate concentration, general awareness | Informational review during regular monthly FinOps standup. |
| `WARNING` | Amber (`#F59E0B`) | Approaching budget limits (90–99%), moderate vendor lock-in (>80%), stale sync (>48h) | Active investigation required by cloud engineering team within 3 business days. |
| `CRITICAL` | Red (`#EF4444`) | Budget exceeded ($\ge 100\%$), severe MoM cost surge ($>15\%$), extreme service concentration | Immediate executive notification and engineering freeze/remediation. |

---

## 3. Catalog of Evaluated FinOps Rules

### 3.1 Rule: Month-over-Month Cost Surge (`RULE_MOM_SURGE`)
- **Trigger**: Total current month expenditure exceeds previous month expenditure by more than **15.0%** ($\% \Delta > +15.0\%$).
- **Severity**: `CRITICAL` if $\% \Delta \ge 25.0\%$, else `WARNING`.
- **Financial Impact**: Calculated as the absolute dollar difference: $\Delta C = C_{\text{curr}} - C_{\text{prev}}$.
- **Recommendation**: Conduct immediate drill-down analysis into top-growing cloud services and check for unattached resources or overprovisioned cluster capacities.

### 3.2 Rule: Month-over-Month Spending Reduction (`RULE_MOM_REDUCTION`)
- **Trigger**: Total current month expenditure is more than **15.0%** lower than previous month expenditure ($\% \Delta < -15.0\%$).
- **Severity**: `INFO`.
- **Financial Impact**: Quantified financial savings: $|C_{\text{curr}} - C_{\text{prev}}|$.
- **Recommendation**: Validate whether decrease is driven by successful rightsizing initiatives, architectural refactoring, or temporary workload reductions.

### 3.3 Rule: Service-Level Spend Concentration (`RULE_SERVICE_DOMINANCE`)
- **Trigger**: A single cloud service (e.g. AWS EC2 or RDS) accounts for **over 40.0%** of the organization's total cloud bill.
- **Severity**: `WARNING`.
- **Financial Impact**: Current total spend on the dominant service.
- **Recommendation**: Evaluate commitment discount opportunities (AWS Savings Plans, Compute Savings, or Azure Reservations) to reduce unit pricing on high-volume services.

### 3.4 Rule: Provider Spend Concentration (`RULE_PROVIDER_DOMINANCE`)
- **Trigger**: A single cloud provider accounts for **over 80.0%** of total cloud spend across multi-cloud environments.
- **Severity**: `INFO`.
- **Financial Impact**: Total spend concentrated in that provider.
- **Recommendation**: Review multi-cloud architectural redundancy and explore cross-cloud negotiation leverage during enterprise agreement renewals.

### 3.5 Rule: Cloud Account Concentration (`RULE_ACCOUNT_SPIKE`)
- **Trigger**: A single cloud account consumes **over 65.0%** of total cloud expenditure.
- **Severity**: `WARNING`.
- **Financial Impact**: Spend consumed by the high-volume account.
- **Recommendation**: Review resource tagging, cost center allocation, and verify that shared platform services are appropriately tagged with chargeback metadata.

### 3.6 Rule: Budget Breach / Exceeded (`RULE_BUDGET_EXCEEDED`)
- **Trigger**: Any active budget has reached or exceeded **100.0%** utilization.
- **Severity**: `CRITICAL`.
- **Financial Impact**: Overspending amount: $\text{Current Spend} - \text{Allocated Budget}$.
- **Recommendation**: Immediate management review required. Request emergency budget reallocation or initiate resource spin-down procedures.

### 3.7 Rule: Budget Warning Threshold (`RULE_BUDGET_WARNING`)
- **Trigger**: An active budget is between **90.0%** and **99.9%** utilization before the period ends.
- **Severity**: `WARNING`.
- **Financial Impact**: Remaining headroom before budget breach.
- **Recommendation**: Audit non-production environments and evaluate pausing non-essential batch workloads until next billing cycle.

### 3.8 Rule: Stale Billing Synchronization (`RULE_STALE_SYNC`)
- **Trigger**: No successful billing synchronization has completed for an active cloud account within the last **48 hours**.
- **Severity**: `WARNING`.
- **Financial Impact**: None directly; risk of delayed anomaly detection and unobserved runaway spend.
- **Recommendation**: Verify cloud IAM permissions and test provider API connection status from the Cloud Accounts management console.

---

## 4. Insight Data Transfer Object (DTO) Structure

Each detected insight adheres to the following standardized JSON contract:

```json
{
  "title": "Cloud Spending Surge: +28.50% MoM",
  "description": "Total cloud expenditure increased by 28.50% compared to previous month ($42,150.00 vs $32,800.00).",
  "severity": "CRITICAL",
  "category": "COST_SPIKE",
  "provider": "AWS",
  "serviceName": "Amazon EC2",
  "financialImpact": 9350.00,
  "currency": "USD",
  "recommendation": "Review EC2 instance family utilization and verify whether autoscaling policies are experiencing runaway container tasks.",
  "detectedAt": "2026-11-12T10:15:30Z"
}
```

---

## 5. Extensibility & M7 Roadmap

The insight engine is designed for high extensibility:
- **Rule Decoupling**: Rules are structured as isolated methods within `FinOpsInsightService.java`, permitting easy extraction into an evaluation pipeline or Spring bean chain.
- **Integration with M7 Anomaly Engine**: In Milestone 7, the rule-based engine will be supplemented with statistical Z-Score and Isolation Forest anomaly triggers to detect micro-spikes in real-time streaming intervals.

