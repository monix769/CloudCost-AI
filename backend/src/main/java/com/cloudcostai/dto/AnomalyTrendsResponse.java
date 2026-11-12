package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Time-series trend dataset of anomalies over time")
public class AnomalyTrendsResponse {

    @Schema(description = "Chronological list of anomaly trend data points")
    private List<AnomalyTrendPointResponse> trends;

    @Schema(description = "Reporting ISO currency code", example = "USD")
    private String currency;
}

