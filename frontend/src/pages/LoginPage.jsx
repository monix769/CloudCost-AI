import React, { useState } from 'react';
import { Link, useNavigate, useLocation, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Shield,
  Layers,
  Lock,
  Mail,
  Eye,
  EyeOff,
  AlertCircle,
  Loader2,
  CheckCircle2,
  ArrowRight,
  Sparkles,
} from 'lucide-react';

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [validationErrors, setValidationErrors] = useState({});

  const fromLocation = location.state?.from?.pathname || '/';

  // Redirect if already authenticated
  if (isAuthenticated) {
    return <Navigate to={fromLocation} replace />;
  }

  const validate = () => {
    const errors = {};
    if (!email.trim()) {
      errors.email = 'Email address is required';
    } else if (!/\S+@\S+\.\S+/.test(email.trim())) {
      errors.email = 'Please provide a valid corporate or personal email';
    }

    if (!password) {
      errors.password = 'Password is required';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);
    try {
      await login(email.trim(), password);
      navigate(fromLocation, { replace: true });
    } catch (err) {
      setErrorMessage(err.message || 'Authentication failed. Please verify your credentials.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleFillDemoAdmin = () => {
    setEmail('admin@cloudcostai.local');
    setPassword('Admin@1234');
    setValidationErrors({});
    setErrorMessage('');
  };

  return (
    <div className="min-h-screen bg-navy-950 flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Background ambient decorative glows */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-indigo-600/10 rounded-full blur-3xl pointer-events-none"></div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md z-10">
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 shadow-xl shadow-blue-500/20 border border-blue-400/30">
            <Layers className="w-7 h-7 text-white" />
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            CloudCost <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-300">AI</span>
          </h2>
          <p className="text-xs sm:text-sm text-slate-400">
            Intelligent Cloud Cost Monitoring &amp; FinOps Analytics
          </p>
        </div>

        {/* Login Card */}
        <div className="mt-8 glass-panel rounded-2xl border border-white/[0.08] shadow-2xl p-6 sm:p-8 backdrop-blur-xl">
          <div className="mb-6 pb-4 border-b border-white/[0.06]">
            <h3 className="text-base font-bold text-white tracking-wide">Account Sign In</h3>
            <p className="text-xs text-slate-400 mt-1">
              Enter your credentials to access your tenant cost dashboard
            </p>
          </div>

          {/* Backend Error Banner */}
          {errorMessage && (
            <div className="mb-5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/25 flex items-start space-x-2.5 text-rose-300 text-xs animate-fadeIn">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div className="flex-1 font-medium">{errorMessage}</div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Email Field */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 font-mono">
                Email Address
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (validationErrors.email) {
                      setValidationErrors((prev) => ({ ...prev, email: null }));
                    }
                  }}
                  placeholder="admin@cloudcostai.local"
                  className={`w-full pl-9 pr-3 py-2.5 rounded-xl bg-navy-900 border text-slate-200 text-xs sm:text-sm placeholder-slate-500 focus:outline-none transition-all ${
                    validationErrors.email
                      ? 'border-rose-500/60 focus:border-rose-500 focus:ring-1 focus:ring-rose-500/30'
                      : 'border-slate-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/30'
                  }`}
                  autoComplete="email"
                />
              </div>
              {validationErrors.email && (
                <p className="mt-1 text-[11px] text-rose-400 flex items-center space-x-1">
                  <span>{validationErrors.email}</span>
                </p>
              )}
            </div>

            {/* Password Field */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider font-mono">
                  Password
                </label>
                <span className="text-[11px] text-slate-500">Min 8 characters</span>
              </div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (validationErrors.password) {
                      setValidationErrors((prev) => ({ ...prev, password: null }));
                    }
                  }}
                  placeholder="••••••••••••"
                  className={`w-full pl-9 pr-10 py-2.5 rounded-xl bg-navy-900 border text-slate-200 text-xs sm:text-sm placeholder-slate-500 focus:outline-none transition-all ${
                    validationErrors.password
                      ? 'border-rose-500/60 focus:border-rose-500 focus:ring-1 focus:ring-rose-500/30'
                      : 'border-slate-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/30'
                  }`}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-200"
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {validationErrors.password && (
                <p className="mt-1 text-[11px] text-rose-400">
                  {validationErrors.password}
                </p>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full mt-2 py-2.5 px-4 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-bold text-xs sm:text-sm shadow-lg shadow-blue-500/25 border border-blue-400/30 flex items-center justify-center space-x-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Authenticating...</span>
                </>
              ) : (
                <>
                  <span>Sign In to Dashboard</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Demo Credentials Quick-Fill helper */}
          <div className="mt-6 pt-4 border-t border-white/[0.06] text-center">
            <button
              type="button"
              onClick={handleFillDemoAdmin}
              className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-blue-500/10 hover:bg-blue-500/20 text-blue-300 text-xs border border-blue-500/20 transition-all font-mono cursor-pointer"
            >
              <Sparkles className="w-3.5 h-3.5 text-blue-400" />
              <span>Fill Default Admin Credentials (Admin@1234)</span>
            </button>
          </div>

          {/* Registration Navigation Link */}
          <div className="mt-5 text-center text-xs text-slate-400">
            Don't have an account yet?{' '}
            <Link
              to="/register"
              className="font-bold text-blue-400 hover:text-blue-300 hover:underline"
            >
              Create an organization account →
            </Link>
          </div>
        </div>

        {/* Security Footer Notice */}
        <div className="mt-6 text-center text-[11px] text-slate-500 flex items-center justify-center space-x-1.5 font-mono">
          <Shield className="w-3.5 h-3.5 text-emerald-400" />
          <span>Secured by Spring Security 6 &amp; HMAC-SHA256 JWT</span>
        </div>
      </div>
    </div>
  );
}
