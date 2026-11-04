# Cloud Provider Integration Guide – CloudCost AI

CloudCost AI integrates directly with major cloud service providers via a modular **Service Provider Interface (SPI)** architecture, enabling automated, accurate, and multi-tenant ingestion of line-item expenditure data.

---

## 1. Architecture Overview

Each cloud provider integration is implemented as an independent provider component implementing the `CloudBillingProvider` SPI:

```
                      ┌───────────────────────────────────────────────┐
                      │            CloudCost AI Backend               │
                      └───────────────────────┬───────────────────────┘
                                              │
                              ┌───────────────┴───────────────┐
                              │    CloudBillingProvider SPI   │
                              └───┬───────────┬───────────┬───┘
                                  │           │           │
            ┌─────────────────────┘           │           └─────────────────────┐
            ▼                                 ▼                                 ▼
┌────────────────────────┐       ┌────────────────────────┐       ┌────────────────────────┐
│   AwsBillingProvider   │       │  AzureBillingProvider  │       │   GcpBillingProvider   │
├────────────────────────┤       ├────────────────────────┤       ├────────────────────────┤
│ AWS Cost Explorer API  │       │ Azure Cost Management  │       │ GCP Cloud Billing Exp. │
│ ce:GetCostAndUsage     │       │ Query API (v2023-11-01)│       │ BigQuery SQL Query     │
│ 24–48h Latency         │       │ 8–24h Latency          │       │ 4–12h Latency          │
└────────────────────────┘       └────────────────────────┘       └────────────────────────┘
```

---

## 2. Amazon Web Services (AWS) Integration

### Supported API
- **API**: AWS Cost Explorer API (`ce:GetCostAndUsage`, `ce:GetDimensionValues`)
- **Granularity**: `DAILY`
- **Group Definitions**: `DIMENSION = SERVICE`
- **Metrics**: `UnblendedCost`, `UsageQuantity`
- **Reporting Lag**: 24 to 48 hours (standard AWS billing computation delay)

### IAM Least-Privilege Policy
Create a dedicated IAM Policy in your AWS Management or Linked Account:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CloudCostAICostExplorerReadOnly",
      "Effect": "Allow",
      "Action": [
        "ce:GetCostAndUsage",
        "ce:GetDimensionValues",
        "ce:GetCostForecast"
      ],
      "Resource": "*"
    }
  ]
}
```

### Authentication Methods
1. **IAM User Access Keys**:
   - `awsAccessKeyId`: `AKIA...`
   - `awsSecretAccessKey`: Encrypted at rest via AES-256-GCM.
   - `awsRegion`: `us-east-1` (Cost Explorer endpoint is hosted in us-east-1).
2. **Cross-Account IAM Role Assumption**:
   - `awsRoleArn`: `arn:aws:iam::123456789012:role/CloudCostAI-FinOpsRole`
   - Supports STS `AssumeRole` with optional External ID for secure cross-account delegated access.

---

## 3. Microsoft Azure Integration

### Supported API
- **API**: Azure Cost Management Query API (`2023-11-01`)
- **Scope**: `/subscriptions/{subscriptionId}`
- **Type**: `ActualCost`
- **Aggregation Dimensions**: `ServiceName`, `ResourceGroupName`, `ResourceType`, `Location`
- **Granularity**: `Daily`
- **Reporting Lag**: 8 to 24 hours (Azure Cost Management data latency)

### Entra ID (Azure AD) Setup
1. Register an Application in Microsoft Entra ID (Azure Portal -> App Registrations).
2. Create a Client Secret under Certificates & secrets.
3. Grant RBAC Role:
   - Navigate to **Subscriptions -> [Your Subscription] -> Access Control (IAM)**.
   - Add Role Assignment: **Cost Management Reader** assigned to the App Registration Service Principal.

### Credential Configuration
- `azureTenantId`: Azure AD Tenant Directory ID (GUID).
- `azureClientId`: Application (client) ID (GUID).
- `azureClientSecret`: Application secret string (AES-256-GCM encrypted).
- `azureSubscriptionId`: Azure Subscription ID (GUID).

---

## 4. Google Cloud Platform (GCP) Integration

### Supported Ingestion Model
- **Mechanism**: Google Cloud Billing export to BigQuery.
- **Table Support**: Standard Cloud Billing export and Detailed export tables.
- **Query Model**: Parameterized BigQuery SQL with interval filtering on `export_time`.
- **Dimensions**: `service.description`, `sku.description`, `project.id`, `location.location`, `usage.amount_in_pricing_units`.
- **Reporting Lag**: 4 to 12 hours (BigQuery export interval).

### BigQuery Export Setup
1. In Google Cloud Console, navigate to **Billing -> Billing Export**.
2. Select or create a BigQuery dataset (e.g. `billing_export`).
3. Enable Standard or Detailed cost data export.

### IAM Prerequisites
Create a Service Account with the following roles on the billing export project:
- `roles/bigquery.dataViewer` (Read access to export dataset and tables).
- `roles/bigquery.jobUser` (Permission to run query jobs in the project).

### Credential Configuration
- `gcpProjectId`: Project hosting the BigQuery billing export.
- `gcpDatasetId`: Dataset name (e.g. `billing_export`).
- `gcpBillingTable`: Full export table name.
- `gcpServiceAccountJson`: Service Account JSON key content (AES-256-GCM encrypted).

---

## 5. Sandbox / Mock Provider Mode

For development, automated CI/CD testing, and staging environments where production cloud credentials are not linked:
- Set `useMockAdapter: true` (or `useMock: true`).
- The system generates realistic, provider-specific, normalized billing line items.
- Ingestion, database persistence, deduplication, sync history auditing, and currency verification execute through the identical production pipeline.
- All records generated in mock mode are explicitly tagged with `source_type = 'MOCK_PROVIDER'` for 100% audit transparency.
