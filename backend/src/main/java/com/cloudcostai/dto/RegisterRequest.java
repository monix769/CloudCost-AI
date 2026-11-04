package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to register a new user account")
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    @Schema(description = "User's full name", example = "Jane Doe")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 180, message = "Email cannot exceed 180 characters")
    @Schema(description = "Corporate or personal email", example = "jane.doe@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one number"
    )
    @Schema(description = "Secure password (min 8 chars, uppercase, lowercase, digit)", example = "P@ssw0rd123")
    private String password;

    @Size(max = 150, message = "Organization name cannot exceed 150 characters")
    @Schema(description = "Tenant organization name", example = "Acme Cloud Corp")
    private String organizationName;

    @Schema(description = "Optional user role assignment", example = "ROLE_USER")
    private String role;
}
