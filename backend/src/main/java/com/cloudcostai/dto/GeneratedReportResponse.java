package com.cloudcostai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedReportResponse {
    private Long id;
    private String reportName;
    private String reportType;
    private String fileFormat;
    private Long fileSizeBytes;
    private String generatedByUserEmail;
    private String generationStatus;
    private String errorMessage;
    private LocalDate startDate;
    private LocalDate endDate;
    private String provider;
    private String currency;
    private int downloadCount;
    private Instant createdAt;
}

