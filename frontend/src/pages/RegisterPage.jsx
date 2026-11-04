import React, { useState } from 'react';
import { Link, useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Layers,
  Lock,
  Mail,
  User,
  Building,
  Eye,
  EyeOff,
  AlertCircle,
  Loader2,
  CheckCircle2,
  ArrowRight,
  Shield,
} from 'lucide-react';

export default function RegisterPage() {
  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [organizationName, setOrganizationName] = useState('Acme Cloud Corp');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [validationErrors, setValidationErrors] = useState({});

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  // Password requirements check
  const hasMinLength = password.length >= 8;
  const hasUpper = /[A-Z]/.test(password);
  const hasLower = /[a-z]/.test(password);
  const hasDigit = /\d/.test(password);

  const validate = () => {
    const errors = {};
    if (!fullName.trim()) {
      errors.fullName = 'Full name is required';
    } else if (fullName.trim().length < 2) {
      errors.fullName = 'Full name must be at least 2 characters';
    }

    if (!email.trim()) {
      errors.email = 'Email address is required';
    } else if (!/\S+@\S+\.\S+/.test(email.trim())) {
      errors.email = 'Please provide a valid email address';
    }

    if (!password) {
      errors.password = 'Password is required';
    } else if (!hasMinLength || !hasUpper || !hasLower || !hasDigit) {
      errors.password = 'Password must meet all complexity requirements';
    }

    if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
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
      await register(fullName.trim(), email.trim(), password, organizationName.trim());
      navigate('/', { replace: true });
    } catch (err) {
      setErrorMessage(err.message || 'Registration failed. Please check the entered fields.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-navy-950 flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Decorative ambient background glows */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 left-1/4 w-80 h-80 bg-indigo-600/10 rounded-full blur-3xl pointer-events-none"></div>

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
            Create your FinOps organization and analytics account
          </p>
        </div>

        {/* Register Card */}
        <div className="mt-8 glass-panel rounded-2xl border border-white/[0.08] shadow-2xl p-6 sm:p-8 backdrop-blur-xl">
          <div className="mb-6 pb-4 border-b border-white/[0.06]">
            <h3 className="text-base font-bold text-white tracking-wide">Organization Registration</h3>
            <p className="text-xs text-slate-400 mt-1">
              Join existing enterprise workspace or bootstrap a new cloud tenant
            </p>
          </div>

          {/* Backend Error Alert */}
          {errorMessage && (
            <div className="mb-5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/25 flex items-start space-x-2.5 text-rose-300 text-xs animate-fadeIn">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div className="flex-1 font-medium">{errorMessage}</div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Full Name */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 font-mono">
                Full Name
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <User className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Jane Doe"
                  className={`w-full pl-9 pr-3 py-2.5 rounded-xl bg-navy-900 border text-slate-200 text-xs sm:text-sm placeholder-slate-500 focus:outline-none transition-all ${
                    validationErrors.fullName
                      ? 'border-rose-500/60 focus:border-rose-500'
                      : 'border-slate-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/30'
                  }`}
                />
              </div>
              {validationErrors.fullName && (
                <p className="mt-1 text-[11px] text-rose-400">{validationErrors.fullName}</p>
              )}
            </div>

            {/* Email Address */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 font-mono">
                Corporate Email
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="jane.doe@company.com"
                  className={`w-full pl-9 pr-3 py-2.5 rounded-xl bg-navy-900 border text-slate-200 text-xs sm:text-sm placeholder-slate-500 focus:outline-none transition-all ${
                    validationErrors.email
                      ? 'border-rose-500/60 focus:border-rose-500'
                      : 'border-slate-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/30'
                  }`}
                  autoComplete="email"
                />
              </div>
              {validationErrors.email && (
                <p className="mt-1 text-[11px] text-rose-400">{validationErrors.email}</p>
              )}
            </div>

            {/* Organization Tenant Name */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 font-mono">
                Organization / Company
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Building className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  value={organizationName}
                  onChange={(e) => setOrganizationName(e.target.value)}
                  placeholder="Acme Cloud Corp"
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-navy-900 border border-slate-800 text-slate-200 text-xs sm:text-sm placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500/30 transition-all"
                />
              </div>
              <p className="mt-1 text-[11px] text-slate-400">
                Matches existing tenant if available or establishes a new workspace
              </p>
            </div>

            {/* Password */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 font-mono">
                Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className={`w-full pl-9 pr-10 py-2.5 rounded-xl bg-navy-900 border text-slate-200 text-xs sm:text-sm placeholder-slate-500 focus:outline-none transition-all ${
                    validationErrors.password
                      ? 'border-rose-500/60 focus:border-rose-500'
                      : 'border-slate-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/30'
                  }`}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-200"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>

              {/* Password Complexity Checklist */}
              <div className="mt-2 grid grid-cols-2 gap-1.5 text-[10px] font-mono">
                <div className={`flex items-center space-x-1 ${hasMinLength ? 'text-emerald-400' : 'text-slate-500'}`}>
                  <CheckCircle2 className="w-3 h-3" />
                  <span>8+ Characters</span>
                </div>
                <div className={`flex items-center space-x-1 ${hasUpper ? 'text-emerald-400' : 'text-slate-500'}`}>
                  <CheckCircle2 className="w-3 h-3" />
                  <span>1 Uppercase</span>
                </div>
                <div className={`flex items-center space-x-1 ${hasLower ? 'text-emerald-400' : 'text-slate-500'}`}>
                  <CheckCircle2 className="w-3 h-3" />
                  <span>1 Lowercase</span>
                </div>
                <div className={`flex items-center space-x-1 ${hasDigit ? 'text-emerald-400' : 'text-slate-500'}`}>
                  <CheckCircle2 className="w-3 h-3" />
                  <span>1 Number</span>
                </div>
              </div>
            </div>

            {/* Confirm Password */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5 font-mono">
                Confirm Password
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className={`w-full pl-9 pr-3 py-2.5 rounded-xl bg-navy-900 border text-slate-200 text-xs sm:text-sm placeholder-slate-500 focus:outline-none transition-all ${
                    validationErrors.confirmPassword
                      ? 'border-rose-500/60 focus:border-rose-500'
                      : 'border-slate-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500/30'
                  }`}
                  autoComplete="new-password"
                />
              </div>
              {validationErrors.confirmPassword && (
                <p className="mt-1 text-[11px] text-rose-400">{validationErrors.confirmPassword}</p>
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
                  <span>Creating Account...</span>
                </>
              ) : (
                <>
                  <span>Create Account &amp; Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Login Link */}
          <div className="mt-5 text-center text-xs text-slate-400">
            Already registered?{' '}
            <Link
              to="/login"
              className="font-bold text-blue-400 hover:text-blue-300 hover:underline"
            >
              Sign in to your account →
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
