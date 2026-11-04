import React from 'react';
import { Navigate, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Shield, Loader2 } from 'lucide-react';

/**
 * Route Guard enforcing JWT authentication.
 * Redirects unauthenticated visitors to /login while preserving the attempted location.
 */
export default function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-navy-950 flex flex-col items-center justify-center p-4">
        <div className="flex flex-col items-center space-y-4 glass-panel p-8 rounded-2xl border border-white/[0.08] shadow-2xl max-w-sm w-full text-center">
          <div className="w-12 h-12 rounded-xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
            <Shield className="w-6 h-6 animate-pulse" />
          </div>
          <div className="space-y-1">
            <h3 className="text-sm font-bold text-white tracking-wide">Validating FinOps Session</h3>
            <p className="text-xs text-slate-400">Verifying JWT authentication and tenant context...</p>
          </div>
          <Loader2 className="w-5 h-5 text-blue-400 animate-spin" />
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children ? children : <Outlet />;
}
