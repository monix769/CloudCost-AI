package com.cloudcostai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenDenylistServiceTest {

    private TokenDenylistService denylistService;

    @BeforeEach
    void setUp() {
        denylistService = new TokenDenylistService();
        denylistService.clear();
    }

    @Test
    @DisplayName("Should report unrevoked token as not revoked")
    void shouldReportUnrevokedTokenAsNotRevoked() {
        assertThat(denylistService.isRevoked("valid.jwt.token")).isFalse();
    }

    @Test
    @DisplayName("Should revoke token and recognize it as revoked")
    void shouldRevokeTokenAndRecognizeAsRevoked() {
        String token = "sample.jwt.token";
        denylistService.revokeToken(token, Instant.now().plusSeconds(300));

        assertThat(denylistService.isRevoked(token)).isTrue();
        assertThat(denylistService.isTokenRevoked(token)).isTrue();
    }

    @Test
    @DisplayName("Should purge expired tokens automatically")
    void shouldPurgeExpiredTokens() {
        String expiredToken = "expired.jwt.token";
        denylistService.revokeToken(expiredToken, Instant.now().minusSeconds(10));

        denylistService.purgeExpiredTokens();

        assertThat(denylistService.isRevoked(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should handle null or empty tokens gracefully")
    void shouldHandleNullOrEmptyTokensGracefully() {
        assertThat(denylistService.isRevoked(null)).isFalse();
        assertThat(denylistService.isRevoked("")).isFalse();
        denylistService.revokeToken(null, Instant.now());
        denylistService.revokeToken("", Instant.now());
    }
}
