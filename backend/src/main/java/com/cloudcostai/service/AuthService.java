package com.cloudcostai.service;

import com.cloudcostai.dto.AuthResponse;
import com.cloudcostai.dto.LoginRequest;
import com.cloudcostai.dto.RegisterRequest;
import com.cloudcostai.dto.UserProfileResponse;
import com.cloudcostai.entity.AuditActionType;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.User;
import com.cloudcostai.entity.UserRole;
import com.cloudcostai.exception.ApiException;
import com.cloudcostai.repository.UserRepository;
import com.cloudcostai.security.JwtProperties;
import com.cloudcostai.security.JwtTokenProvider;
import com.cloudcostai.security.TokenDenylistService;
import com.cloudcostai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Locale;

/**
 * Service orchestrating user registration, authentication, JWT issuing,
 * profile retrieval, and stateless logout denylisting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenDenylistService tokenDenylistService;
    private final AuditLogService auditLogService;

    /**
     * Registers a new user, hashes password with BCrypt, links to organization,
     * assigns initial role, and returns an AuthResponse with signed JWT.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Registration rejected: duplicate email [{}]", normalizedEmail);
            throw new ApiException(HttpStatus.CONFLICT, "A user with email '" + normalizedEmail + "' already exists.");
        }

        // Resolve or create tenant organization
        Organization organization = organizationService.getOrCreateOrganization(request.getOrganizationName());

        // Assign initial role (admin if explicitly named or email contains admin, otherwise USER)
        String initialRole = (normalizedEmail.contains("admin") || "ROLE_ADMIN".equalsIgnoreCase(request.getRole()))
                ? UserRole.ROLE_ADMIN.name()
                : UserRole.ROLE_USER.name();

        String trimmedFullName = request.getFullName().trim();
        String firstName = trimmedFullName;
        String lastName = "";
        int spaceIdx = trimmedFullName.indexOf(' ');
        if (spaceIdx > 0) {
            firstName = trimmedFullName.substring(0, spaceIdx).trim();
            lastName = trimmedFullName.substring(spaceIdx + 1).trim();
        }

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(initialRole)
                .organization(organization)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user ID [{}] with email [{}] under organization [{}]",
                savedUser.getId(), savedUser.getEmail(), organization.getName());

        try {
            auditLogService.logEvent(organization, savedUser.getId(), savedUser.getEmail(),
                    AuditActionType.USER_REGISTER, "USER", savedUser.getId().toString(), null, "User registered successfully", "SUCCESS");
        } catch (Exception ex) {
            log.debug("Audit log skipped: {}", ex.getMessage());
        }

        String jwt = jwtTokenProvider.generateTokenFromUser(savedUser);
        UserProfileResponse profile = mapToProfileResponse(savedUser);

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.getExpirationMs())
                .user(profile)
                .build();
    }

    /**
     * Authenticates user credentials via Spring Security and issues a signed JWT.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String jwt = jwtTokenProvider.generateToken(authentication);

            // Record login timestamp
            userRepository.findById(principal.getId()).ifPresent(user -> {
                user.setLastLoginAt(Instant.now());
                userRepository.save(user);
            });

            log.info("User [{}] successfully logged in", normalizedEmail);

            try {
                auditLogService.logEvent(organizationService.getCurrentUserOrganization(), principal.getId(), principal.getUsername(),
                        AuditActionType.LOGIN, "USER", principal.getId().toString(), null, "User logged in successfully", "SUCCESS");
            } catch (Exception ex) {
                log.debug("Audit log skipped: {}", ex.getMessage());
            }

            UserProfileResponse profile = UserProfileResponse.builder()
                    .id(principal.getId())
                    .email(principal.getUsername())
                    .fullName(principal.getFullName())
                    .role(principal.getRole())
                    .organizationId(principal.getOrganizationId())
                    .organizationName(principal.getOrganizationName())
                    .organizationSlug(principal.getOrganizationSlug())
                    .isActive(principal.isEnabled())
                    .build();

            return AuthResponse.builder()
                    .token(jwt)
                    .tokenType("Bearer")
                    .expiresInMs(jwtProperties.getExpirationMs())
                    .user(profile)
                    .build();

        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for email: {}", normalizedEmail);
            try {
                auditLogService.logEvent(organizationService.getDefaultOrganization(), null, normalizedEmail,
                        AuditActionType.FAILED_LOGIN, "AUTH", normalizedEmail, null, "Failed login attempt (bad credentials)", "FAILURE");
            } catch (Exception e) {
                log.debug("Audit log skipped: {}", e.getMessage());
            }
            throw new BadCredentialsException("Invalid email or password.");
        }
    }

    /**
     * Retrieves currently authenticated user profile from SecurityContext.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User profile not found."));

        return mapToProfileResponse(user);
    }

    /**
     * Performs stateless logout by recording token on server-side denylist until natural expiration.
     */
    public void logout(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7).trim();
            long remainingMs = jwtTokenProvider.getRemainingValidityMs(token);

            if (remainingMs > 0) {
                tokenDenylistService.revokeToken(token, remainingMs);
                log.info("Revoked JWT token with remaining validity: {} ms", remainingMs);
            }
        }
        try {
            auditLogService.logCurrent(AuditActionType.LOGOUT, "AUTH", "SESSION", "User logged out");
        } catch (Exception ex) {
            log.debug("Audit log skipped: {}", ex.getMessage());
        }
        SecurityContextHolder.clearContext();
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        Organization org = user.getOrganization();
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole() : UserRole.ROLE_USER.name())
                .organizationId(org != null ? org.getId() : null)
                .organizationName(org != null ? org.getName() : null)
                .organizationSlug(org != null ? org.getSlug() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
