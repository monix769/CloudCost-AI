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
public class OptimizationRecommendationHistoryResponse {
    private Long id;
    private Long recommendationId;
    private String previousStatus;
    private String newStatus;
    private Long actionByUserId;
    private String actionByUserEmail;
    private String notes;
    private Instant createdAt;
}

