package com.cloudcostai.analytics.forecast;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Historical daily expenditure data point")
public class HistoricalDataPoint {

    @Schema(description = "Date of recorded cloud usage", example = "2026-10-15")
    private LocalDate date;

    @Schema(description = "Actual cost incurred", example = "1245.50")
    private BigDecimal cost;
}

