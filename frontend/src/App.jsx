import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { SystemProvider } from './context/SystemContext';
import AppLayout from './layouts/AppLayout';

// SaaS FinOps Pages
import OverviewPage from './pages/OverviewPage';
import CloudAccountsPage from './pages/CloudAccountsPage';
import CostAnalyticsPage from './pages/CostAnalyticsPage';
import BillingManagementPage from './pages/BillingManagementPage';
import BudgetManagementPage from './pages/BudgetManagementPage';
import AnomalyDetectionPage from './pages/AnomalyDetectionPage';
import CostOptimizationPage from './pages/CostOptimizationPage';
import ReportsPage from './pages/ReportsPage';
import SettingsPage from './pages/SettingsPage';

// System Architecture & Diagnostics (Preserving Milestone 1)
import ArchitecturePage from './pages/ArchitecturePage';
import HealthPage from './pages/HealthPage';
import StatusPage from './pages/StatusPage';
import DocsPage from './pages/DocsPage';
import NotFoundPage from './pages/NotFoundPage';

export default function App() {
  return (
    <SystemProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<AppLayout />}>
            <Route index element={<OverviewPage />} />
            <Route path="overview" element={<Navigate to="/" replace />} />
            <Route path="cloud-accounts" element={<CloudAccountsPage />} />
            <Route path="analytics" element={<CostAnalyticsPage />} />
            <Route path="billing" element={<BillingManagementPage />} />
            <Route path="budgets" element={<BudgetManagementPage />} />
            <Route path="anomalies" element={<AnomalyDetectionPage />} />
            <Route path="optimization" element={<CostOptimizationPage />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="settings" element={<SettingsPage />} />

            {/* Diagnostics & Architecture */}
            <Route path="architecture" element={<ArchitecturePage />} />
            <Route path="health" element={<HealthPage />} />
            <Route path="status" element={<StatusPage />} />
            <Route path="docs" element={<DocsPage />} />

            {/* 404 Fallback */}
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </SystemProvider>
  );
}
