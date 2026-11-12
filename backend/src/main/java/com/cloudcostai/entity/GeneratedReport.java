package com.cloudcostai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "generated_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 60)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", nullable = false, length = 10)
    private ReportFormat fileFormat;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size_bytes", nullable = false)
    @Builder.Default
    private Long fileSizeBytes = 0L;

    @Column(name = "generated_by_user_id")
    private Long generatedByUserId;

    @Column(name = "generated_by_user_email", nullable = false, length = 180)
    private String generatedByUserEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus generationStatus = ReportStatus.COMPLETED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20)
    private CloudProvider provider;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private int downloadCount = 0;
}

