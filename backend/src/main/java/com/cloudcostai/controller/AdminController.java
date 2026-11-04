package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.UserProfileResponse;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.User;
import com.cloudcostai.repository.UserRepository;
import com.cloudcostai.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administrative REST controller protected by ROLE_ADMIN authorization rules.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Privileged endpoints restricted exclusively to ROLE_ADMIN accounts")
public class AdminController {

    private final UserRepository userRepository;
    private final OrganizationService organizationService;

    @GetMapping("/users")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @Operation(summary = "List organization users", description = "Privileged operation returning user accounts within the current organization (ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getOrganizationUsers() {
        Organization org = organizationService.getCurrentUserOrganization();
        log.info("Admin accessing user roster for organization [{}]", org.getName());

        List<UserProfileResponse> users = userRepository.findByOrganizationId(org.getId()).stream()
                .map(this::toProfile)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Organization users retrieved", users));
    }

    private UserProfileResponse toProfile(User user) {
        Organization org = user.getOrganization();
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .organizationId(org != null ? org.getId() : null)
                .organizationName(org != null ? org.getName() : null)
                .organizationSlug(org != null ? org.getSlug() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
