package com.cloudcostai.config;

import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.User;
import com.cloudcostai.entity.UserRole;
import com.cloudcostai.repository.UserRepository;
import com.cloudcostai.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures seed data integrity on startup.
 * Guarantees that the default admin user has a valid BCrypt password hash.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Organization defaultOrg = organizationService.getDefaultOrganization();

        userRepository.findByEmailIgnoreCase("admin@cloudcostai.local").ifPresentOrElse(
                admin -> {
                    // Check if existing password_hash is a dummy placeholder from M1 seed
                    if (admin.getPasswordHash() == null || admin.getPasswordHash().contains("y0z1z1z1z1z1z1z1z1z") || !admin.getPasswordHash().startsWith("$2a$")) {
                        log.info("Updating default admin user with valid BCrypt hashed password (Admin@1234)");
                        admin.setPasswordHash(passwordEncoder.encode("Admin@1234"));
                        admin.setRole(UserRole.ROLE_ADMIN.name());
                        admin.setOrganization(defaultOrg);
                        admin.setIsActive(true);
                        userRepository.save(admin);
                    }
                },
                () -> {
                    log.info("Creating default administrator account: admin@cloudcostai.local");
                    User admin = User.builder()
                            .email("admin@cloudcostai.local")
                            .firstName("System")
                            .lastName("Administrator")
                            .passwordHash(passwordEncoder.encode("Admin@1234"))
                            .role(UserRole.ROLE_ADMIN.name())
                            .organization(defaultOrg)
                            .isActive(true)
                            .build();
                    userRepository.save(admin);
                }
        );
    }
}
