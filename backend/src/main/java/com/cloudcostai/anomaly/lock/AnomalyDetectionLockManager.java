package com.cloudcostai.anomaly.lock;

/**
 * Concurrency lock manager abstraction for anomaly detection runs per organization.
 * Prevents overlapping detection runs for the same tenant.
 */
public interface AnomalyDetectionLockManager {

    /**
     * Attempts to acquire an exclusive anomaly detection lock for an organization.
     *
     * @param organizationId ID of the tenant organization
     * @return true if lock acquired, false if a scan is already running
     */
    boolean tryLock(Long organizationId);

    /**
     * Releases the lock for the given organization.
     */
    void releaseLock(Long organizationId);

    /**
     * Checks if a scan is currently active for the given organization.
     */
    boolean isLocked(Long organizationId);
}

