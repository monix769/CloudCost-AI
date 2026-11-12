# Statistical Cost Forecasting Engine – CloudCost AI

## 1. Overview & Architectural Philosophy

The CloudCost AI **Cost Forecasting Engine** is implemented natively in **Java 21** as part of Milestone 6. It provides forward-looking cloud expenditure projections across **7-day**, **30-day**, and **90-day** horizons without relying on heavy external Python runtimes, opaque machine-learning APIs, or unverified statistical assumptions.

The architecture adopts the **Strategy Pattern** via the `Forecaster` interface, ensuring modularity, full unit testability, and zero runtime dependencies beyond the standard Java SDK.

```
                    ┌───────────────────────────┐
                    │    CostForecastService    │
                    └─────────────┬─────────────┘
                                  │ selects model
                 ┌────────────────┴────────────────┐
                 ▼                                 ▼
   ┌───────────────────────────┐     ┌───────────────────────────┐
   │ MovingAverageForecaster   │     │ ExponentialSmoothing...   │
   │ (k-day Rolling Window)    │     │ (Single Exp Smoothing)    │
   └─────────────┬─────────────┘     └─────────────┬─────────────┘
                 │                                 │
                 └────────────────┬────────────────┘
                                  ▼
                    ┌───────────────────────────┐
                    │      ForecastResult       │
                    │ - Historical Data Points  │
                    │ - Forecast Data Points    │
                    │ - 95% Confidence Bounds   │
                    │ - Total Projected Spend   │
                    └───────────────────────────┘
```

---

## 2. Supported Forecasting Methodologies

### 2.1 Moving Average Forecasting (`MOVING_AVERAGE`)
The Moving Average model calculates projected spend using an unweighted rolling window of historical daily expenditures.

#### Mathematical Formulation
Let $y_1, y_2, \dots, y_n$ represent the sequence of historical daily cloud costs, and let $k$ denote the sliding window size (default: $k = \min(n, 14)$):
$$\hat{y}_{t+h} = \frac{1}{k} \sum_{i=0}^{k-1} y_{n - i} \quad \text{for } h = 1, 2, \dots, H$$
Where:
- $n$: Number of available historical days.
- $k$: Sliding window parameter (typically 7 or 14 days).
- $H$: Forecast horizon ($H \in \{7, 30, 90\}$ days).
- $\hat{y}_{t+h}$: Forecasted expenditure on future day $t+h$.

#### Baseline Uncertainty Estimation
The sample standard deviation $s$ over the historical window is computed as:
$$s = \sqrt{\frac{1}{k - 1} \sum_{i=0}^{k-1} (y_{n - i} - \bar{y})^2}$$
The 95% confidence interval for future step $h$ is given by:
$$\text{CI}_{95\%} = \hat{y} \pm 1.96 \cdot s \cdot \sqrt{1 + \frac{1}{k}}$$
Where negative lower bounds are strictly clamped to zero ($\max(0, \text{lowerBound})$) to reflect physical reality (cloud costs cannot be negative).

---

### 2.2 Simple Exponential Smoothing (`EXPONENTIAL_SMOOTHING`)
Single Exponential Smoothing (SES) applies exponentially decreasing weights to older observations, making it ideal for tracking baseline run-rates with recent trend sensitivity.

#### Mathematical Formulation
Given smoothing factor $\alpha \in (0.01, 1.0]$ (default: $\alpha = 0.30$):
$$\hat{s}_1 = y_1$$
$$\hat{s}_t = \alpha \cdot y_t + (1 - \alpha) \cdot \hat{s}_{t-1} \quad \text{for } t = 2, 3, \dots, n$$
For horizon step $h \in [1, H]$, the point forecast is flat at the final smoothed level:
$$\hat{y}_{n+h} = \hat{s}_n$$

#### Expanding Variance & 95% Confidence Bounds
Because uncertainty increases further into the future, the prediction variance for horizon step $h$ expands:
$$\sigma_h = s_e \cdot \sqrt{1 + (h - 1) \cdot \alpha^2}$$
Where $s_e$ is the standard error of historical one-step-ahead forecast residuals:
$$s_e = \sqrt{\frac{1}{n - 1} \sum_{t=2}^{n} (y_t - \hat{s}_{t-1})^2}$$
The two-sided 95% confidence bounds are:
$$\text{Upper Bound}_{n+h} = \hat{y}_{n+h} + 1.96 \cdot \sigma_h$$
$$\text{Lower Bound}_{n+h} = \max\left(0, \hat{y}_{n+h} - 1.96 \cdot \sigma_h\right)$$

---

## 3. Forecast Horizons & Configurations

| Horizon Code | Horizon Days | Typical Use Case | Recommended History |
|---|---|---|---|
| `NEXT_7_DAYS` | 7 Days | Operational sprint budgeting & immediate run-rate checks | $\ge 7$ days |
| `NEXT_30_DAYS` | 30 Days | Monthly financial close & budget variance forecasting | $\ge 30$ days |
| `NEXT_90_DAYS` | 90 Days | Quarterly enterprise financial planning & board reporting | $\ge 60$ days |

### Configurable Parameters
- `method`: `MOVING_AVERAGE` or `EXPONENTIAL_SMOOTHING` (defaults to `EXPONENTIAL_SMOOTHING`).
- `horizon`: `NEXT_7_DAYS`, `NEXT_30_DAYS`, or `NEXT_90_DAYS` (defaults to `NEXT_30_DAYS`).
- `alpha`: Smoothing factor for Exponential Smoothing ($0.05 \le \alpha \le 0.95$, default: $0.30$).
- `currency`: ISO-4217 currency code (defaults to organization's active currency).

---

## 4. Cold-Start Handling & Data Validation

The forecasting engine enforces strict statistical safety checks:
1. **Zero Data Points**: Returns an empty forecast result with explanatory message: *"Insufficient historical data: at least 3 daily expenditure data points required."*
2. **Minimal Data (1–2 Days)**: Uses flat projection based on available average, with wider default uncertainty bounds ($50\%$), and flags the result with an explanatory note.
3. **Missing Intermediate Days**: Aggregates records by exact calendar date (`LocalDate`), treating days with no registered cloud consumption as $\$0.00$ spend rather than skipping dates, preventing timeline skewing.
4. **Negative Value Prevention**: Lower confidence bounds are clamped to zero.

---

## 5. Statistical Limitations & Future Roadmap

> [!IMPORTANT]
> **Limitations of Milestone 6 Forecasting**:
> - **Seasonality**: Single Exponential Smoothing and Simple Moving Average do not model multi-week or annual seasonality (e.g., Black Friday traffic spikes or weekend batch drops).
> - **Sudden Architectural Changes**: Unplanned infrastructure migrations or instant terminations will take several days to reflect in moving averages.
> - **Confidence Interpretability**: Confidence intervals assume normally distributed residual errors. Extreme one-off spikes may temporarily inflate confidence bands.

### Future Evolution (Milestone 7 & Beyond)
- Implementation of **Holt-Winters Exponential Smoothing** (triple exponential smoothing) to account for weekly seasonality.
- Integration with the **M7 Anomaly Detection Engine** to filter out identified cost anomalies prior to training the baseline forecast.
- Support for event-driven adjustments (e.g., scheduled reserved instance renewals).

