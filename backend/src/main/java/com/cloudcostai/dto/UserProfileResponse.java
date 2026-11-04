package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sanitized user profile information")
public class UserProfileResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Email address", example = "jane.doe@example.com")
    private String email;

    @Schema(description = "First name", example = "Jane")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Full combined name", example = "Jane Doe")
    private String fullName;

    @Schema(description = "Assigned system role", example = "ROLE_USER")
    private String role;

    @Schema(description = "Tenant Organization ID", example = "1")
    private Long organizationId;

    @Schema(description = "Tenant Organization Name", example = "Acme Cloud Corp")
    private String organizationName;

    @Schema(description = "Tenant Organization Slug", example = "acme-corp")
    private String organizationSlug;

    @Schema(description = "Account active state indicator", example = "true")
    private Boolean isActive;

    @Schema(description = "Last login timestamp")
    private Instant lastLoginAt;

    @Schema(description = "Account creation timestamp")
    private Instant createdAt;
}
