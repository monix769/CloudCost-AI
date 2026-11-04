import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { SystemProvider } from './context/SystemContext';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './layouts/AppLayout';

// Auth Pages (Milestone 4)
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

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
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public Authentication Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* Protected Enterprise SaaS Application Routes */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
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
      </AuthProvider>
    </SystemProvider>
  );
}
