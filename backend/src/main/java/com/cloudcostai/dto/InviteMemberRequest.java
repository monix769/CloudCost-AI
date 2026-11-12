package com.cloudcostai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteMemberRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email address is required")
    private String email;

    @Builder.Default
    private String role = "ROLE_USER";
}

