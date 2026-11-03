package com.cloudcostai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration.
 * 
 * Milestone 1 establishes the security architecture with stateless session policies,
 * password hashing readiness, and unrestricted access to public discovery &amp; health endpoints.
 * Authentication filters (JWT) will be attached in Milestone 2.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security Filter Chain for Milestone 1 architecture");

        http
                // Disable CSRF for stateless RESTful APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Session Management: Stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure endpoint authorization
                .authorizeHttpRequests(auth -> auth
                        // Milestone 3 API Endpoints, Public Discovery & Swagger Docs
                        .requestMatchers(
                                "/",
                                "/api/**",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
