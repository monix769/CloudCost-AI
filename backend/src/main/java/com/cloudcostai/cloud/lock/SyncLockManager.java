package com.cloudcostai.cloud.lock;

/**
 * Abstraction for synchronization concurrency locking per cloud account.
 * Prevents overlapping ingestion jobs for the same account.
 *
 * Single-instance MVP uses InMemorySyncLockManager (ConcurrentHashMap).
 * For horizontal scaling across multiple application instances, implement
 * this interface using Redis (Redlock) or database advisory locks.
 */
public interface SyncLockManager {

    /**
     * Attempts to acquire an exclusive synchronization lock for the given cloud account.
     *
     * @param cloudAccountId ID of the cloud account
     * @return true if the lock was acquired; false if another sync is already running
     */
    boolean tryLock(Long cloudAccountId);

    /**
     * Releases the synchronization lock for the given cloud account.
     *
     * @param cloudAccountId ID of the cloud account
     */
    void releaseLock(Long cloudAccountId);

    /**
     * Checks if a synchronization job is currently in progress for the account.
     *
     * @param cloudAccountId ID of the cloud account
     * @return true if currently locked
     */
    boolean isLocked(Long cloudAccountId);
}
