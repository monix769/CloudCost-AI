package com.cloudcostai.service;

import com.cloudcostai.entity.AuditActionType;
import com.cloudcostai.entity.AuditLog;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    @DisplayName("Should sanitize sensitive passwords, tokens, and credentials before recording audit log")
    void shouldSanitizeSensitiveDetails() {
        Organization org = Organization.builder().name("Test Org").build();
        org.setId(1L);

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawDetails = "{\"user\":\"admin@test.com\", \"password\":\"SuperSecret123\", \"token\":\"eyJhbGciOi...\", \"clientSecret\":\"my-secret-key\"}";

        AuditLog saved = auditLogService.logEvent(
                org,
                10L,
                "admin@test.com",
                AuditActionType.LOGIN,
                "AUTH",
                "10",
                "127.0.0.1",
                rawDetails,
                "SUCCESS"
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getDetails()).doesNotContain("SuperSecret123");
        assertThat(saved.getDetails()).doesNotContain("my-secret-key");
        assertThat(saved.getDetails()).contains("\"password\":\"***\"");
        assertThat(saved.getDetails()).contains("\"token\":\"***\"");
        assertThat(saved.getDetails()).contains("\"clientSecret\":\"***\"");
    }

    @Test
    @DisplayName("Should record append-only audit event with actor and timestamp")
    void shouldRecordAuditEvent() {
        Organization org = Organization.builder().name("Test Org").build();
        org.setId(1L);

        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog saved = auditLogService.logEvent(
                org,
                5L,
                "finops@test.com",
                AuditActionType.REPORT_GENERATED,
                "REPORT",
                "42",
                "192.168.1.10",
                "Generated Executive PDF Report",
                "SUCCESS"
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getActionType()).isEqualTo(AuditActionType.REPORT_GENERATED);
        assertThat(saved.getResourceType()).isEqualTo("REPORT");
        assertThat(saved.getResourceId()).isEqualTo("42");
        assertThat(saved.getUserEmail()).isEqualTo("finops@test.com");
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
    }
}

