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
public class OrganizationProfileResponse {
    private Long id;
    private String name;
    private String slug;
    private String tier;
    private Boolean isActive;
    private long memberCount;
    private Instant createdAt;
    private Instant updatedAt;
}

