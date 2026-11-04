package com.cloudcostai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "cloud_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_org_provider_account", columnNames = {"organization_id", "provider", "account_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private CloudProvider provider;

    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "arn_or_client_id", length = 255)
    private String arnOrClientId;

    @Column(name = "encrypted_credentials", columnDefinition = "TEXT")
    private String encryptedCredentials;

    @Column(name = "sync_status", nullable = false, length = 50)
    @Builder.Default
    private String syncStatus = "PENDING";

    @Column(name = "connection_status", nullable = false, length = 50)
    @Builder.Default
    private String connectionStatus = "DISCONNECTED";

    @Column(name = "key_version", nullable = false, length = 20)
    @Builder.Default
    private String keyVersion = "v1";

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
