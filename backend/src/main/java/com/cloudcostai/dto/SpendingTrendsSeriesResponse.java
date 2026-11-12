package com.cloudcostai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete time-series spending trend dataset with summary statistical aggregations")
public class SpendingTrendsSeriesResponse {

    @Schema(description = "Time granularity bucket resolution", example = "DAILY")
    private TimeGranularity granularity;

    @Schema(description = "Currency of aggregated figures", example = "USD")
    private String currency;

    @Schema(description = "Query window start date")
    private LocalDate startDate;

    @Schema(description = "Query window end date")
    private LocalDate endDate;

    @Schema(description = "Grand total spend across entire series", example = "35400.00")
    private BigDecimal totalSpend;

    @Schema(description = "Average bucket spend across entire series", example = "1180.00")
    private BigDecimal averageSpend;

    @Schema(description = "Lowest spending period total", example = "820.00")
    private BigDecimal minSpend;

    @Schema(description = "Highest spending period total", example = "1950.00")
    private BigDecimal maxSpend;

    @Schema(description = "Chronological list of data points")
    @Builder.Default
    private List<SpendingTrendPointResponse> points = new ArrayList<>();
}

