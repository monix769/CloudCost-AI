package com.cloudcostai.security;

import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.User;
import com.cloudcostai.entity.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(secret);
        jwtProperties.setExpirationMs(3600000L); // 1 hour

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate valid JWT token from Authentication")
    void shouldGenerateValidTokenFromAuthentication() {
        Organization org = Organization.builder().name("Test Org").slug("test-org").build();
        org.setId(10L);

        User user = User.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .passwordHash("hashed")
                .role("ROLE_USER")
                .organization(org)
                .isActive(true)
                .build();
        user.setId(5L);

        UserPrincipal principal = UserPrincipal.create(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String token = jwtTokenProvider.generateToken(auth);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("alice@example.com");
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(5L);
        assertThat(jwtTokenProvider.getOrganizationIdFromToken(token)).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should generate valid JWT token directly from User entity")
    void shouldGenerateValidTokenFromUserEntity() {
        Organization org = Organization.builder().name("Acme Corp").slug("acme").build();
        org.setId(1L);

        User user = User.builder()
                .firstName("Bob")
                .lastName("Jones")
                .email("bob@acme.com")
                .role("ROLE_ADMIN")
                .organization(org)
                .isActive(true)
                .build();
        user.setId(2L);

        String token = jwtTokenProvider.generateTokenFromUser(user);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("bob@acme.com");
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(2L);
        assertThat(jwtTokenProvider.getOrganizationIdFromToken(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should reject tampered token with invalid signature")
    void shouldRejectTamperedToken() {
        Organization org = Organization.builder().name("Org").build();
        org.setId(1L);
        User user = User.builder().email("tamper@test.com").role("ROLE_USER").organization(org).build();
        user.setId(1L);

        String validToken = jwtTokenProvider.generateTokenFromUser(user);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "abcde";

        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Build token with past expiration
        Date past = new Date(System.currentTimeMillis() - 60000); // 1 min ago
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        String expiredToken = Jwts.builder()
                .subject("expired@test.com")
                .issuedAt(new Date(past.getTime() - 60000))
                .expiration(past)
                .signWith(Keys.hmacShaKeyFor(keyBytes))
                .compact();

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed token strings")
    void shouldRejectMalformedToken() {
        assertThat(jwtTokenProvider.validateToken("not-a-valid-jwt-token")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }
}
