package com.cloudcostai.cloud.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of SyncLockManager using ConcurrentHashMap.
 *
 * NOTE (Architecture Limitation):
 * This implementation is scoped to a single JVM instance. In clustered or horizontally
 * scaled production deployments with multiple backend containers, replace this bean
 * with a distributed lock implementation (e.g. Redisson / Redis distributed locks).
 */
@Component
@Slf4j
public class InMemorySyncLockManager implements SyncLockManager {

    private final ConcurrentHashMap<Long, Boolean> activeLocks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(Long cloudAccountId) {
        if (cloudAccountId == null) return false;
        Boolean previous = activeLocks.putIfAbsent(cloudAccountId, Boolean.TRUE);
        boolean acquired = (previous == null);
        if (acquired) {
            log.debug("Acquired sync lock for cloud account [{}]", cloudAccountId);
        } else {
            log.warn("Sync lock collision: Account [{}] is already locked by an ongoing sync task", cloudAccountId);
        }
        return acquired;
    }

    @Override
    public void releaseLock(Long cloudAccountId) {
        if (cloudAccountId == null) return;
        activeLocks.remove(cloudAccountId);
        log.debug("Released sync lock for cloud account [{}]", cloudAccountId);
    }

    @Override
    public boolean isLocked(Long cloudAccountId) {
        if (cloudAccountId == null) return false;
        return activeLocks.containsKey(cloudAccountId);
    }
}
