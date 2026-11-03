package com.cloudcostai.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties prepared for Milestone 2 Authentication workflows.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Secret key for signing HMAC SHA-256 JWT tokens.
     */
    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    /**
     * Token validity in milliseconds (default: 24 hours).
     */
    private long expirationMs = 86400000L;

    /**
     * Header name containing token.
     */
    private String header = "Authorization";

    /**
     * Token prefix.
     */
    private String prefix = "Bearer ";
}
