package com.cloudcostai.security;

import com.cloudcostai.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Service for generating, parsing, and validating JSON Web Tokens (JWT) using JJWT 0.12.x.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate token from Spring Security Authentication object.
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpirationMs());

        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userPrincipal.getUsername()) // email
                .claim("userId", userPrincipal.getId())
                .claim("role", userPrincipal.getRole())
                .claim("roles", roles)
                .claim("organizationId", userPrincipal.getOrganizationId())
                .claim("organizationName", userPrincipal.getOrganizationName())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Generate token directly from User entity.
     */
    public String generateTokenFromUser(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getExpirationMs());

        String roleStr = user.getRole() != null ? user.getRole() : "ROLE_USER";
        Long orgId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        String orgName = user.getOrganization() != null ? user.getOrganization().getName() : null;

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", roleStr)
                .claim("roles", roleStr)
                .claim("organizationId", orgId)
                .claim("organizationName", orgName)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extract subject (email) from token.
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract user ID from token claims.
     */
    public Long getUserIdFromToken(String token) {
        Number userId = (Number) getClaims(token).get("userId");
        return userId != null ? userId.longValue() : null;
    }

    /**
     * Extract organization ID from token claims.
     */
    public Long getOrganizationIdFromToken(String token) {
        Number orgId = (Number) getClaims(token).get("organizationId");
        return orgId != null ? orgId.longValue() : null;
    }

    /**
     * Extract expiration date from token.
     */
    public Date getExpirationFromToken(String token) {
        return getClaims(token).getExpiration();
    }

    /**
     * Validate JWT token signature and expiration.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature or malformed token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or invalid: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT processing exception: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Parse token claims.
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get remaining validity in milliseconds for denylist TTL calculation.
     */
    public long getRemainingValidityMs(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            long diff = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, diff);
        } catch (Exception e) {
            return 0;
        }
    }
}
