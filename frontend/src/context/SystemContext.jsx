import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { systemService } from '../services/systemService';

const SystemContext = createContext(null);

export function SystemProvider({ children }) {
  const [health, setHealth] = useState(null);
  const [rootInfo, setRootInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  const refreshSystemData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [rootData, healthData] = await Promise.allSettled([
        systemService.getRootInfo(),
        systemService.getHealth()
      ]);

      if (rootData.status === 'fulfilled') {
        setRootInfo(rootData.value.data);
      }
      if (healthData.status === 'fulfilled') {
        setHealth(healthData.value.data);
      } else {
        setError('Backend is unreachable or responding with an error');
      }
      setLastUpdated(new Date());
    } catch (err) {
      setError(err.message || 'Failed to communicate with backend');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshSystemData();
    // Auto-poll health every 30 seconds
    const interval = setInterval(refreshSystemData, 30000);
    return () => clearInterval(interval);
  }, [refreshSystemData]);

  const isConnected = !!health && health.status === 'UP';
  const isDatabaseConnected = !!health && health.database === 'CONNECTED';

  return (
    <SystemContext.Provider
      value={{
        health,
        rootInfo,
        loading,
        error,
        lastUpdated,
        isConnected,
        isDatabaseConnected,
        refreshSystemData,
      }}
    >
      {children}
    </SystemContext.Provider>
  );
}

export function useSystem() {
  const context = useContext(SystemContext);
  if (!context) {
    throw new Error('useSystem must be used within a SystemProvider');
  }
  return context;
}
