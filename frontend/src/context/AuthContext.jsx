import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => sessionStorage.getItem('cloudcost_token') || null);
  const [user, setUser] = useState(() => {
    try {
      const stored = sessionStorage.getItem('cloudcost_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [isLoading, setIsLoading] = useState(true);

  // Restore or verify active session on initial page load / refresh
  const verifySession = useCallback(async () => {
    const activeToken = sessionStorage.getItem('cloudcost_token') || localStorage.getItem('cloudcost_token');
    if (!activeToken) {
      setUser(null);
      setToken(null);
      setIsLoading(false);
      return;
    }

    try {
      const response = await authService.getProfile();
      if (response && response.success && response.data) {
        setUser(response.data);
        setToken(activeToken);
        sessionStorage.setItem('cloudcost_user', JSON.stringify(response.data));
      } else {
        throw new Error('Invalid profile payload');
      }
    } catch (err) {
      console.warn('Failed to restore active user session:', err.message);
      sessionStorage.removeItem('cloudcost_token');
      sessionStorage.removeItem('cloudcost_user');
      localStorage.removeItem('cloudcost_token');
      localStorage.removeItem('cloudcost_user');
      setUser(null);
      setToken(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    verifySession();

    // Listen for unauthorized/expired token events from Axios interceptor
    const handleAuthExpired = () => {
      setUser(null);
      setToken(null);
      setIsLoading(false);
    };

    window.addEventListener('cloudcost_auth_expired', handleAuthExpired);
    return () => window.removeEventListener('cloudcost_auth_expired', handleAuthExpired);
  }, [verifySession]);

  /**
   * Login with email and password
   */
  const login = async (email, password) => {
    const response = await authService.login({ email, password });
    if (response && response.success && response.data) {
      const { token: jwtToken, user: userProfile } = response.data;
      sessionStorage.setItem('cloudcost_token', jwtToken);
      sessionStorage.setItem('cloudcost_user', JSON.stringify(userProfile));
      setToken(jwtToken);
      setUser(userProfile);
      return userProfile;
    }
    throw new Error(response.message || 'Authentication failed');
  };

  /**
   * Register a new user and organization
   */
  const register = async (fullName, email, password, organizationName) => {
    const response = await authService.register({
      fullName,
      email,
      password,
      organizationName,
    });
    if (response && response.success && response.data) {
      const { token: jwtToken, user: userProfile } = response.data;
      sessionStorage.setItem('cloudcost_token', jwtToken);
      sessionStorage.setItem('cloudcost_user', JSON.stringify(userProfile));
      setToken(jwtToken);
      setUser(userProfile);
      return userProfile;
    }
    throw new Error(response.message || 'Registration failed');
  };

  /**
   * Terminate active session and invoke server-side token revocation
   */
  const logout = async () => {
    try {
      await authService.logout();
    } finally {
      sessionStorage.removeItem('cloudcost_token');
      sessionStorage.removeItem('cloudcost_user');
      localStorage.removeItem('cloudcost_token');
      localStorage.removeItem('cloudcost_user');
      setToken(null);
      setUser(null);
    }
  };

  const value = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    isLoading,
    login,
    register,
    logout,
    refreshProfile: verifySession,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
