package com.cloudcostai.dto;

import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.ReportFormat;
import com.cloudcostai.entity.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportGenerateRequest {

    private String reportName;

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "File format is required (PDF or CSV)")
    private ReportFormat fileFormat;

    private LocalDate startDate;

    private LocalDate endDate;

    private CloudProvider provider;

    @Builder.Default
    private String currency = "USD";
}

