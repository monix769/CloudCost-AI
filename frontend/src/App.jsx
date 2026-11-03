import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { SystemProvider } from './context/SystemContext';
import RootLayout from './layouts/RootLayout';
import HomePage from './pages/HomePage';
import HealthPage from './pages/HealthPage';
import StatusPage from './pages/StatusPage';
import DocsPage from './pages/DocsPage';
import NotFoundPage from './pages/NotFoundPage';

export default function App() {
  return (
    <SystemProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<RootLayout />}>
            <Route index element={<HomePage />} />
            <Route path="health" element={<HealthPage />} />
            <Route path="status" element={<StatusPage />} />
            <Route path="docs" element={<DocsPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </SystemProvider>
  );
}
