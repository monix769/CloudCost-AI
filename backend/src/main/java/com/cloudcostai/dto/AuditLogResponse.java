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
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String actionType;
    private String resourceType;
    private String resourceId;
    private String ipAddress;
    private String details;
    private String status;
    private Instant createdAt;
}

