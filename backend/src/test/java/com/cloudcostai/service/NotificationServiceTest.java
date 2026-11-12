package com.cloudcostai.service;

import com.cloudcostai.dto.NotificationResponse;
import com.cloudcostai.dto.UnreadNotificationCountResponse;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.CostAnomaly;
import com.cloudcostai.entity.Notification;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private NotificationService notificationService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder().name("Acme Cloud Corp").build();
        testOrg.setId(1L);
    }

    @Test
    @DisplayName("Should create in-app notification for CRITICAL severity anomaly")
    void shouldCreateNotificationForCriticalAnomaly() {
        CostAnomaly anomaly = CostAnomaly.builder()
                .organization(testOrg)
                .serviceName("Amazon Redshift")
                .severity(AnomalySeverity.CRITICAL)
                .actualCost(new BigDecimal("1200.00"))
                .expectedCost(new BigDecimal("200.00"))
                .excessCost(new BigDecimal("1000.00"))
                .deviationPercentage(new BigDecimal("500.00"))
                .build();
        anomaly.setId(99L);

        when(notificationRepository.existsByOrganizationIdAndReferenceTypeAndReferenceId(1L, "COST_ANOMALY", 99L))
                .thenReturn(false);

        notificationService.createAnomalyNotification(anomaly);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should skip creating notification if alert for that anomaly already exists")
    void shouldSkipDuplicateNotification() {
        CostAnomaly anomaly = CostAnomaly.builder()
                .organization(testOrg)
                .serviceName("Amazon Redshift")
                .severity(AnomalySeverity.HIGH)
                .build();
        anomaly.setId(99L);

        when(notificationRepository.existsByOrganizationIdAndReferenceTypeAndReferenceId(1L, "COST_ANOMALY", 99L))
                .thenReturn(true);

        notificationService.createAnomalyNotification(anomaly);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should retrieve unread notification count")
    void shouldGetUnreadCount() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(notificationRepository.countByOrganizationIdAndIsReadFalse(1L)).thenReturn(4L);

        UnreadNotificationCountResponse res = notificationService.getUnreadCount();

        assertNotNull(res);
        assertEquals(4L, res.getUnreadCount());
    }

    @Test
    @DisplayName("Should mark single notification as read")
    void shouldMarkNotificationAsRead() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        Notification notif = Notification.builder()
                .organization(testOrg)
                .title("Alert")
                .message("Spike")
                .severity("HIGH")
                .isRead(false)
                .build();
        notif.setId(7L);

        when(notificationRepository.findByIdAndOrganizationId(7L, 1L)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(7L);

        assertNotNull(response);
        assertTrue(response.getIsRead());
        verify(notificationRepository, times(1)).save(notif);
    }
}

