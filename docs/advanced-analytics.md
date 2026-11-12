# Advanced FinOps Analytics Engine – CloudCost AI

## 1. Overview & Architecture

CloudCost AI Milestone 6 elevates the platform from basic spend reporting to a comprehensive, enterprise-grade **FinOps Financial Intelligence and Analytics Engine**. Built natively in **Java 21** and **Spring Boot 3.3.5** with **React 19**, the analytics engine delivers deep visibility into multi-cloud spending dynamics across AWS, Microsoft Azure, and Google Cloud Platform (GCP).

### Core Capabilities
1. **Total Cloud Spending & Velocity Analytics**: Aggregates enterprise expenditures across custom date windows with daily run-rate, velocity indicators, and peak/trough expenditure tracking.
2. **Multi-Cloud Cost Comparison & Share**: Computes precise provider percentage distribution derived from immutable billing records.
3. **Multi-Dimensional Cost Allocation**: Analyzes spend distributions across dimensions: Cloud Provider, Account, Service, and Region with percentage contributions.
4. **Flexible Spending Trends**: Aggregates expenditures across Daily, Weekly, Monthly, and Quarterly temporal granularities with previous-period delta comparisons.
5. **Month-over-Month (MoM) Financial Comparison**: Calculates absolute and percentage variances at aggregate, provider, and service levels with explainable business narratives.
6. **Native Statistical Cost Forecasting**: Java-implemented Moving Average and Exponential Smoothing models with 7D, 30D, and 90D horizons and 95% confidence intervals.
7. **Budget Health & Utilization Tracking**: Real-time budget burn rate, projected end-of-period (EOP) spending, and health classifications (`HEALTHY`, `WARNING`, `CRITICAL`, `EXCEEDED`).
8. **Automated FinOps Insights**: Deterministic, rule-based heuristics flagging budget breaches, vendor lock-in, anomalous service growth, and synchronization delays.
9. **Streaming CSV Exports**: High-performance streaming exports for trends, provider shares, service breakdowns, forecasts, and actionable insights.

---

## 2. Mathematical Formulations & Metrics

All monetary calculations in CloudCost AI strictly use `java.math.BigDecimal` with `RoundingMode.HALF_UP` to prevent IEEE-754 floating-point inaccuracies.

### 2.1 Average Daily Spending (Velocity)
Given a date range $[D_{\text{start}}, D_{\text{end}}]$ with total days $N = \text{ChronoUnit.DAYS.between}(D_{\text{start}}, D_{\text{end}}) + 1$:
$$\text{Average Daily Spend} = \frac{\sum_{i=1}^{M} \text{costAmount}_i}{N}$$

### 2.2 Provider Cost Distribution Percentage
For a given provider $P \in \{\text{AWS}, \text{AZURE}, \text{GCP}\}$ and total cloud spend $S_{\text{total}}$:
$$\text{Share}(P) = \left( \frac{S_P}{S_{\text{total}}} \right) \times 100$$
Where $\sum_{P} \text{Share}(P) = 100.00\%$.

### 2.3 Month-over-Month (MoM) Cost Comparison
Let $C_{\text{curr}}$ be current month spending and $C_{\text{prev}}$ be previous month spending:
- **Absolute Change**:
  $$\Delta C = C_{\text{curr}} - C_{\text{prev}}$$
- **Percentage Change**:
  $$\% \Delta = \begin{cases}
  0.00\% & \text{if } C_{\text{prev}} = 0 \text{ and } C_{\text{curr}} = 0 \\
  +100.00\% & \text{if } C_{\text{prev}} = 0 \text{ and } C_{\text{curr}} > 0 \\
  -100.00\% & \text{if } C_{\text{prev}} > 0 \text{ and } C_{\text{curr}} = 0 \\
  \left(\frac{C_{\text{curr}} - C_{\text{prev}}}{C_{\text{prev}}}\right) \times 100 & \text{otherwise}
  \end{cases}$$

### 2.4 Period-over-Period Trend Variance
For any temporal window $W_t$ and its preceding adjacent window $W_{t-1}$:
$$\% \Delta_t = \left( \frac{\text{Spend}(W_t) - \text{Spend}(W_{t-1})}{\text{Spend}(W_{t-1})} \right) \times 100$$

### 2.5 Budget Burn Rate & Projected End-of-Period (EOP)
Let $B_{\text{spend}}$ be current spend in budget period, $d_{\text{elapsed}}$ be elapsed days, and $d_{\text{total}}$ be total days in the budget lifecycle:
$$\text{Daily Burn Rate} = \frac{B_{\text{spend}}}{d_{\text{elapsed}}}$$
$$\text{Projected EOP Spend} = B_{\text{spend}} + (\text{Daily Burn Rate} \times (d_{\text{total}} - d_{\text{elapsed}}))$$
$$\text{Utilization Percentage} = \left( \frac{B_{\text{spend}}}{B_{\text{allocated}}} \right) \times 100$$

---

## 3. FinOps Multi-Currency Policy

1. **Strict Currency Isolation**: CloudCost AI never combines records denominated in different currencies (e.g., USD, EUR, INR) without explicit exchange conversion.
2. **Explicit Currency Resolution**: Every request allows an optional `?currency=XXX` filter. If omitted, the system resolves the organization's predominant currency.
3. **Discrepancy Rejection**: When multiple currencies exist within an organization and no currency parameter is specified, cross-currency aggregations return clear warning metadata prompting currency isolation.
4. **Precision**: All outputs format monetary quantities with 2 decimal places and ISO 4217 currency identifiers.

---

## 4. REST API Endpoint Specifications

All endpoints reside under `/api/analytics/*` and require standard JWT Bearer token authentication. Tenant isolation is enforced using the authenticated user's `organizationId`.

### 4.1 Analytics Overview
- **Endpoint**: `GET /api/analytics/overview`
- **Query Parameters**:
  - `startDate` (optional, `YYYY-MM-DD`): Start date filter (defaults to current month start).
  - `endDate` (optional, `YYYY-MM-DD`): End date filter (defaults to today).
  - `currency` (optional, string): ISO currency code (e.g., `USD`).
- **Response**: `AnalyticsOverviewResponse` containing `summary`, `providerComparison`, `topServices`, and `insights`.

### 4.2 Spending Trends
- **Endpoint**: `GET /api/analytics/spending-trends`
- **Query Parameters**:
  - `granularity` (optional): `DAILY` (default), `WEEKLY`, `MONTHLY`, `QUARTERLY`.
  - `startDate`, `endDate`, `currency`.
- **Response**: `SpendingTrendsSeriesResponse` containing time-series data points with spend, period boundaries, and percentage change.

### 4.3 Month-over-Month Comparison
- **Endpoint**: `GET /api/analytics/month-over-month`
- **Query Parameters**:
  - `month` (optional, `YYYY-MM`): Target month (defaults to current month).
  - `currency` (optional).
- **Response**: `MonthOverMonthResponse` with overall, provider, and service breakdown deltas and natural language summary.

### 4.4 Multi-Cloud Cost Allocation
- **Endpoint**: `GET /api/analytics/cost-allocation`
- **Query Parameters**:
  - `dimension`: `PROVIDER`, `ACCOUNT`, `SERVICE`, `REGION`.
  - `startDate`, `endDate`, `currency`.
- **Response**: `CostAllocationResponse` containing ranked items, amounts, record counts, and percentage shares.

### 4.5 Budget Utilization Analytics
- **Endpoint**: `GET /api/analytics/budget-utilization`
- **Query Parameters**:
  - `currency` (optional).
- **Response**: `BudgetUtilizationResponse` detailing total allocated, total spent, overall health status, and granular per-budget burn metrics.

### 4.6 Streaming CSV Export Endpoints
- `GET /api/analytics/export/spending-trends` (CSV attachment)
- `GET /api/analytics/export/provider-comparison` (CSV attachment)
- `GET /api/analytics/export/service-breakdown` (CSV attachment)
- `GET /api/analytics/export/forecast` (CSV attachment)
- `GET /api/analytics/export/insights` (CSV attachment)

---

## 5. Database Indexing & Query Optimization

To sustain sub-100ms response times on millions of billing records, Milestone 6 adds composite B-Tree indexes:

```sql
-- Composite index for organization + date range aggregations
CREATE INDEX idx_billing_org_period 
ON billing_records (organization_id, billing_period_start, billing_period_end);

-- Composite index for provider-level cost distribution
CREATE INDEX idx_billing_org_provider 
ON billing_records (organization_id, provider, billing_period_start);

-- Composite index for service-level cost ranking
CREATE INDEX idx_billing_org_service 
ON billing_records (organization_id, service_name, cost_amount);

-- Composite index for account-level spending breakdown
CREATE INDEX idx_billing_org_account 
ON billing_records (organization_id, cloud_account_id, billing_period_start);

-- Composite index for regional cost allocation
CREATE INDEX idx_billing_org_region 
ON billing_records (organization_id, region, cost_amount);
```
These indexes eliminate full table scans during daily projection aggregations and group-by operations.

