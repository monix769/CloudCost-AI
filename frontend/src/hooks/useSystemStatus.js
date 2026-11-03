import { useState, useEffect, useCallback } from 'react';
import { systemService } from '../services/systemService';

export function useSystemStatus() {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchStatus = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await systemService.getStatus();
      setStatus(response.data);
    } catch (err) {
      setError(err.message || 'Failed to retrieve system status diagnostics');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  return { status, loading, error, refetch: fetchStatus };
}
