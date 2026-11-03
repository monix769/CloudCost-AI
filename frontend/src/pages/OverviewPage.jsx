import React from 'react';
import { DollarSign, Wallet, Percent, PiggyBank, Cloud, Sparkles } from 'lucide-react';
import KpiCard from '../components/KpiCard';
import SpendingTrendChart from '../components/SpendingTrendChart';
import ProviderDistributionChart from '../components/ProviderDistributionChart';
import ServiceBreakdownChart from '../components/ServiceBreakdownChart';
import BudgetForecastChart from '../components/BudgetForecastChart';
import CloudProviderSummary from '../components/CloudProviderSummary';
import RecentBillingTable from '../components/RecentBillingTable';
import BudgetStatusSection from '../components/BudgetStatusSection';
import RecentAlertsSection from '../components/RecentAlertsSection';
import QuickActions from '../components/QuickActions';
import DemoDataDisclaimer from '../components/DemoDataDisclaimer';
import { kpiMetrics } from '../utils/mockFinOpsData';

export default function OverviewPage() {
  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Disclaimer Banner for Transparency */}
      <DemoDataDisclaimer />

      {/* Quick Actions Shortcuts */}
      <QuickActions />

      {/* 4 Primary KPI Cards */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Total Cloud Spending */}
        <KpiCard
          title="Total Cloud Spending"
          value={`$${kpiMetrics.totalSpend.toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
          subtitle="Aggregated across AWS, GCP & Azure"
          changePercent={kpiMetrics.spendChangePercent}
          changeType={kpiMetrics.spendChangeType}
          icon={DollarSign}
          accentColor="blue"
          badgeText="MTD"
        />

        {/* KPI 2: Monthly Budget */}
        <KpiCard
          title="Monthly Budget Limit"
          value={`$${kpiMetrics.monthlyBudget.toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
          subtitle={`Remaining: $${kpiMetrics.budgetRemaining.toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
          icon={Wallet}
          accentColor="emerald"
          badgeText="Q4 2026"
        />

        {/* KPI 3: Budget Utilization */}
        <KpiCard
          title="Budget Utilization"
          value={`${kpiMetrics.budgetUtilizationPercent}%`}
          subtitle="Alert Threshold set at 80.0%"
          progress={kpiMetrics.budgetUtilizationPercent}
          progressColor={kpiMetrics.budgetUtilizationPercent >= 85 ? 'bg-amber-500' : 'bg-blue-500'}
          icon={Percent}
          accentColor="amber"
          badgeText="Soft Alert"
        />

        {/* KPI 4: Potential Cost Savings */}
        <KpiCard
          title="Potential Monthly Savings"
          value={`$${kpiMetrics.potentialSavings.toLocaleString(undefined, { minimumFractionDigits: 2 })}`}
          subtitle={`${kpiMetrics.activeRecommendationsCount} actionable recommendations`}
          icon={PiggyBank}
          accentColor="purple"
          badgeText="AI FinOps"
        />
      </section>

      {/* 2x2 Analytics Charts Grid */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <SpendingTrendChart />
        <ProviderDistributionChart />
        <ServiceBreakdownChart />
        <BudgetForecastChart />
      </section>

      {/* Connected Cloud Environments */}
      <section>
        <CloudProviderSummary />
      </section>

      {/* Budgets & Alerts Grid */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <BudgetStatusSection />
        <RecentAlertsSection />
      </section>

      {/* Recent Cloud Billing Records Table */}
      <section>
        <RecentBillingTable />
      </section>
    </div>
  );
}
