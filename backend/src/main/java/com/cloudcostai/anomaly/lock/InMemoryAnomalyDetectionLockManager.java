package com.cloudcostai.anomaly.lock;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryAnomalyDetectionLockManager implements AnomalyDetectionLockManager {

    private final ConcurrentMap<Long, Boolean> activeLocks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(Long organizationId) {
        if (organizationId == null) return false;
        return activeLocks.putIfAbsent(organizationId, Boolean.TRUE) == null;
    }

    @Override
    public void releaseLock(Long organizationId) {
        if (organizationId != null) {
            activeLocks.remove(organizationId);
        }
    }

    @Override
    public boolean isLocked(Long organizationId) {
        if (organizationId == null) return false;
        return activeLocks.containsKey(organizationId);
    }
}

