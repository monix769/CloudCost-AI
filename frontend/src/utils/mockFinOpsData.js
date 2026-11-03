/**
 * CloudCost AI – FinOps Demonstration Dataset
 * 
 * NOTE: The data in this module represents high-fidelity sample FinOps metrics
 * for commercial UI/UX demonstration until multi-cloud provider billing ingest
 * APIs (AWS Cost Explorer, GCP Billing, Azure Cost Management) are linked.
 */

export const DEMO_DISCLAIMER = "Sample FinOps Demonstration Data • Demo Organization (Acme Cloud Corp)";

export const kpiMetrics = {
  totalSpend: 42850.20,
  spendChangePercent: 4.2,
  spendChangeType: 'increase', // or 'decrease'
  monthlyBudget: 50000.00,
  budgetRemaining: 7149.80,
  budgetUtilizationPercent: 85.7,
  budgetStatus: 'warning', // healthy (<80), warning (80-99), danger (>=100)
  potentialSavings: 6420.00,
  activeRecommendationsCount: 14,
  currency: 'USD',
  billingPeriod: 'Nov 2026 (Month-to-Date)',
};

export const monthlySpendTrends = {
  labels: ['Jun 2026', 'Jul 2026', 'Aug 2026', 'Sep 2026', 'Oct 2026', 'Nov 2026 (MTD)'],
  datasets: [
    {
      label: 'Amazon Web Services (AWS)',
      data: [21500, 22100, 24300, 23800, 24900, 23140],
      borderColor: '#ff9900',
      backgroundColor: 'rgba(255, 153, 0, 0.12)',
      borderWidth: 2.5,
      tension: 0.35,
      fill: true,
      pointRadius: 3,
      pointHoverRadius: 6,
    },
    {
      label: 'Google Cloud Platform (GCP)',
      data: [10200, 11400, 11800, 12600, 13100, 12410],
      borderColor: '#38bdf8',
      backgroundColor: 'rgba(56, 189, 248, 0.12)',
      borderWidth: 2.5,
      tension: 0.35,
      fill: true,
      pointRadius: 3,
      pointHoverRadius: 6,
    },
    {
      label: 'Microsoft Azure',
      data: [6800, 7100, 7400, 6900, 7600, 7300],
      borderColor: '#818cf8',
      backgroundColor: 'rgba(129, 140, 248, 0.12)',
      borderWidth: 2.5,
      tension: 0.35,
      fill: true,
      pointRadius: 3,
      pointHoverRadius: 6,
    },
  ],
};

export const providerDistribution = {
  labels: ['AWS (54.0%)', 'GCP (29.0%)', 'Azure (17.0%)'],
  datasets: [
    {
      data: [23140, 12410, 7300],
      backgroundColor: ['#ff9900', '#0ea5e9', '#6366f1'],
      borderColor: '#0b1120',
      borderWidth: 3,
      hoverOffset: 6,
    },
  ],
};

export const serviceBreakdown = {
  labels: [
    'Amazon EC2',
    'Google Kubernetes Engine (GKE)',
    'Amazon RDS',
    'Azure Virtual Machines',
    'Google BigQuery',
    'Amazon S3 Storage',
    'CloudFront CDN',
  ],
  datasets: [
    {
      label: 'Current Month Spend ($)',
      data: [11240, 8450, 6120, 4300, 3960, 2820, 1600],
      backgroundColor: [
        'rgba(255, 153, 0, 0.85)',
        'rgba(14, 165, 233, 0.85)',
        'rgba(255, 153, 0, 0.70)',
        'rgba(99, 102, 241, 0.85)',
        'rgba(14, 165, 233, 0.70)',
        'rgba(255, 153, 0, 0.55)',
        'rgba(59, 130, 246, 0.75)',
      ],
      borderRadius: 6,
      borderSkipped: false,
    },
  ],
};

export const budgetForecast = {
  labels: ['Actual Spend', 'Forecast Month-End', 'Monthly Budget Limit'],
  datasets: [
    {
      label: 'Amount ($)',
      data: [42850, 48200, 50000],
      backgroundColor: [
        '#3b82f6',
        '#f59e0b',
        '#10b981',
      ],
      borderRadius: 8,
      barThickness: 36,
    },
  ],
};

export const cloudAccountsList = [
  {
    id: 'acc-1',
    name: 'Production AWS Account',
    provider: 'AWS',
    accountId: '1234-5678-9012',
    region: 'us-east-1 (Primary)',
    currentSpend: 23140.20,
    monthlyBudget: 28000.00,
    status: 'ACTIVE',
    syncStatus: 'HEALTHY',
    lastSync: '12 minutes ago',
    activeResources: 184,
  },
  {
    id: 'acc-2',
    name: 'Data & Analytics GCP Project',
    provider: 'GCP',
    accountId: 'gcp-production-9876',
    region: 'us-central1',
    currentSpend: 12410.00,
    monthlyBudget: 14000.00,
    status: 'ACTIVE',
    syncStatus: 'HEALTHY',
    lastSync: '18 minutes ago',
    activeResources: 92,
  },
  {
    id: 'acc-3',
    name: 'Core Infrastructure Azure Sub',
    provider: 'AZURE',
    accountId: 'sub-azure-5544-22',
    region: 'eastus',
    currentSpend: 7300.00,
    monthlyBudget: 8000.00,
    status: 'ACTIVE',
    syncStatus: 'HEALTHY',
    lastSync: '25 minutes ago',
    activeResources: 47,
  },
];

export const recentBillingRecords = [
  {
    id: 'bill-101',
    account: 'Production AWS Account',
    provider: 'AWS',
    service: 'Amazon Elastic Compute Cloud (EC2)',
    region: 'us-east-1',
    usage: 'm5.2xlarge Linux (744 hrs)',
    cost: 1420.50,
    date: '2026-11-03',
    status: 'PROCESSED',
  },
  {
    id: 'bill-102',
    account: 'Data & Analytics GCP Project',
    provider: 'GCP',
    service: 'Google Kubernetes Engine (GKE)',
    region: 'us-central1',
    usage: 'Standard Cluster Node Pool (24 nodes)',
    cost: 2150.00,
    date: '2026-11-03',
    status: 'PROCESSED',
  },
  {
    id: 'bill-103',
    account: 'Production AWS Account',
    provider: 'AWS',
    service: 'Amazon Relational Database Service (RDS)',
    region: 'us-east-1',
    usage: 'Multi-AZ MySQL db.r5.xlarge',
    cost: 860.25,
    date: '2026-11-02',
    status: 'PROCESSED',
  },
  {
    id: 'bill-104',
    account: 'Core Infrastructure Azure Sub',
    provider: 'AZURE',
    service: 'Azure Virtual Machines',
    region: 'eastus',
    usage: 'Standard D4s v3 Instances',
    cost: 640.80,
    date: '2026-11-02',
    status: 'PROCESSED',
  },
  {
    id: 'bill-105',
    account: 'Data & Analytics GCP Project',
    provider: 'GCP',
    service: 'Google BigQuery',
    region: 'us-central1',
    usage: 'Analysis Queries (48 TB processed)',
    cost: 412.00,
    date: '2026-11-01',
    status: 'PROCESSED',
  },
  {
    id: 'bill-106',
    account: 'Production AWS Account',
    provider: 'AWS',
    service: 'Amazon Simple Storage Service (S3)',
    region: 'us-east-1',
    usage: 'Standard Storage & Data Retrieval (18 TB)',
    cost: 395.40,
    date: '2026-11-01',
    status: 'PROCESSED',
  },
];

export const departmentBudgets = [
  {
    department: 'Engineering & Core Platform',
    lead: 'Alex Rivera',
    allocated: 26000,
    spent: 23400,
    percentage: 90.0,
    status: 'warning',
  },
  {
    department: 'AI & Machine Learning Infrastructure',
    lead: 'Dr. Sarah Chen',
    allocated: 14000,
    spent: 11200,
    percentage: 80.0,
    status: 'warning',
  },
  {
    department: 'Data Analytics & BI',
    lead: 'Marcus Vance',
    allocated: 6500,
    spent: 5100,
    percentage: 78.5,
    status: 'healthy',
  },
  {
    department: 'Security & DevOps Tools',
    lead: 'Elena Rostova',
    allocated: 3500,
    spent: 3150,
    percentage: 90.0,
    status: 'warning',
  },
];

export const recentAlerts = [
  {
    id: 'alert-1',
    title: 'AWS EC2 Spend Spike Detected',
    description: 'EC2 instance spend in us-east-1 increased by 38% over the 48-hour baseline.',
    severity: 'HIGH',
    service: 'EC2',
    provider: 'AWS',
    time: '2 hours ago',
    resolved: false,
  },
  {
    id: 'alert-2',
    title: 'Department Budget Threshold (90%) Reached',
    description: 'Engineering & Core Platform budget has exceeded the 90% soft alert threshold.',
    severity: 'MEDIUM',
    service: 'Budget',
    provider: 'ALL',
    time: '5 hours ago',
    resolved: false,
  },
  {
    id: 'alert-3',
    title: 'Unattached EBS Volume Idle for 14 Days',
    description: '6 unattached gp3 volumes (total 1.2 TB) detected generating unnecessary charges.',
    severity: 'LOW',
    service: 'EBS',
    provider: 'AWS',
    time: '1 day ago',
    resolved: true,
  },
];

export const optimizationOpportunities = [
  {
    id: 'opt-1',
    title: 'Right-size Underutilized EC2 Instances',
    category: 'RIGHTSIZING',
    provider: 'AWS',
    resource: 'i-0ab1289cf4920 (m5.4xlarge)',
    recommendation: 'Downgrade to m5.xlarge based on average 8% CPU utilization over 30 days.',
    estimatedSavingsMonthly: 418.00,
    effort: 'LOW',
    status: 'RECOMMENDED',
  },
  {
    id: 'opt-2',
    title: 'Purchase 1-Year Compute Savings Plan',
    category: 'COMMITMENT',
    provider: 'AWS',
    resource: 'Baseline EC2 & Fargate Fleet',
    recommendation: 'Commit to $18/hr baseline compute commitment for 32% blended rate reduction.',
    estimatedSavingsMonthly: 3450.00,
    effort: 'MEDIUM',
    status: 'RECOMMENDED',
  },
  {
    id: 'opt-3',
    title: 'Clean Up Idle Google Cloud NAT Gateway',
    category: 'IDLE_RESOURCE',
    provider: 'GCP',
    resource: 'gateway-us-central1-stg',
    recommendation: 'Staging NAT gateway has zero ingress/egress packets for the past 21 days.',
    estimatedSavingsMonthly: 145.00,
    effort: 'VERY_LOW',
    status: 'RECOMMENDED',
  },
  {
    id: 'opt-4',
    title: 'Delete Unattached Azure Managed Disks',
    category: 'STORAGE',
    provider: 'AZURE',
    resource: '4x Premium SSD (512 GB each)',
    recommendation: 'Unattached OS disks from deleted test virtual machines.',
    estimatedSavingsMonthly: 280.00,
    effort: 'VERY_LOW',
    status: 'RECOMMENDED',
  },
  {
    id: 'opt-5',
    title: 'Adopt GKE Autopilot for Development Workloads',
    category: 'ARCHITECTURE',
    provider: 'GCP',
    resource: 'dev-cluster-primary',
    recommendation: 'Migrate standard dev node pool to GKE Autopilot to eliminate paying for unused node capacity.',
    estimatedSavingsMonthly: 890.00,
    effort: 'HIGH',
    status: 'IN_REVIEW',
  },
];
