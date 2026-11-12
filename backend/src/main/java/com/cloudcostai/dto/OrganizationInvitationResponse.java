package com.cloudcostai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationInvitationResponse {
    private Long id;
    private String email;
    private String role;
    private String status;
    private Instant expiresAt;
    private Instant createdAt;
}

