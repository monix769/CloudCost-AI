package com.cloudcostai.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side Token Denylist Service for stateless JWT revocation upon logout.
 * 
 * Retention &amp; Expiration Strategy:
 * When a user logs out, their active JWT is placed into the denylist with its original expiration timestamp.
 * Any subsequent requests presenting this token are immediately rejected with HTTP 401 Unauthorized.
 * To prevent unbounded memory consumption, a scheduled cleanup task automatically evicts tokens
 * whose expiration timestamp has passed, since naturally expired tokens are already rejected by the JWT parser.
 */
@Service
@Slf4j
public class TokenDenylistService {

    private final Map<String, Instant> denylistedTokens = new ConcurrentHashMap<>();

    /**
     * Revokes a token by adding it to the denylist until its expiration time.
     *
     * @param token Raw JWT token string
     * @param expiration Token's expiration timestamp
     */
    public void revokeToken(String token, Instant expiration) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        Instant exp = expiration != null ? expiration : Instant.now().plusSeconds(86400);
        denylistedTokens.put(token.trim(), exp);
        log.info("Token added to denylist, active until {}", exp);
    }

    public void revokeToken(String token, long remainingMs) {
        revokeToken(token, Instant.now().plusMillis(Math.max(1000, remainingMs)));
    }

    public boolean isTokenRevoked(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        Instant expiry = denylistedTokens.get(token.trim());
        if (expiry == null) {
            return false;
        }
        if (Instant.now().isAfter(expiry)) {
            denylistedTokens.remove(token.trim());
            return false;
        }
        return true;
    }

    public boolean isRevoked(String token) {
        return isTokenRevoked(token);
    }

    /**
     * Periodic cleanup of expired tokens every 15 minutes.
     */
    @Scheduled(fixedRate = 900000)
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        int initialSize = denylistedTokens.size();
        denylistedTokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        int purged = initialSize - denylistedTokens.size();
        if (purged > 0) {
            log.debug("Purged {} expired tokens from logout denylist cache", purged);
        }
    }

    /**
     * Helper for test environments to clear denylist state.
     */
    public void clear() {
        denylistedTokens.clear();
    }
}
