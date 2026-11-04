package com.cloudcostai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sync_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cloud_account_id", nullable = false)
    private CloudAccount cloudAccount;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "sync_start_time", nullable = false)
    private Instant syncStartTime;

    @Column(name = "sync_end_time")
    private Instant syncEndTime;

    @Column(name = "sync_status", nullable = false, length = 50)
    @Builder.Default
    private String syncStatus = "IN_PROGRESS";

    @Column(name = "records_fetched", nullable = false)
    @Builder.Default
    private Integer recordsFetched = 0;

    @Column(name = "records_inserted", nullable = false)
    @Builder.Default
    private Integer recordsInserted = 0;

    @Column(name = "records_skipped", nullable = false)
    @Builder.Default
    private Integer recordsSkipped = 0;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "date_range_start")
    private LocalDate dateRangeStart;

    @Column(name = "date_range_end")
    private LocalDate dateRangeEnd;
}
