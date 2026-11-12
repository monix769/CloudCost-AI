package com.cloudcostai.dto;

import com.cloudcostai.entity.OptimizationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OptimizationStatus status;

    private String notes;

    private BigDecimal realizedMonthlySavings;
}

