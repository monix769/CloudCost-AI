import { useSystem } from '../context/SystemContext';

export function useHealth() {
  const { health, loading, error, isConnected, isDatabaseConnected, refreshSystemData } = useSystem();
  return {
    health,
    loading,
    error,
    isConnected,
    isDatabaseConnected,
    refetch: refreshSystemData,
  };
}
