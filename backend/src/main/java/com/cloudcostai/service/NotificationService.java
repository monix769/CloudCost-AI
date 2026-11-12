package com.cloudcostai.service;

import com.cloudcostai.dto.NotificationResponse;
import com.cloudcostai.dto.UnreadNotificationCountResponse;
import com.cloudcostai.entity.CostAnomaly;
import com.cloudcostai.entity.Notification;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Boolean unreadOnly, int limit) {
        Organization org = organizationService.getCurrentUserOrganization();
        int safeLimit = Math.max(1, Math.min(100, limit));
        Pageable pageable = PageRequest.of(0, safeLimit);

        List<Notification> items;
        if (Boolean.TRUE.equals(unreadOnly)) {
            items = notificationRepository.findByOrganizationIdAndIsReadFalseOrderByCreatedAtDesc(org.getId());
            if (items.size() > safeLimit) {
                items = items.subList(0, safeLimit);
            }
        } else {
            items = notificationRepository.findByOrganizationIdOrderByCreatedAtDesc(org.getId(), pageable);
        }

        return items.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount() {
        Organization org = organizationService.getCurrentUserOrganization();
        long count = notificationRepository.countByOrganizationIdAndIsReadFalse(org.getId());
        return UnreadNotificationCountResponse.builder()
                .unreadCount(count)
                .build();
    }

    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Organization org = organizationService.getCurrentUserOrganization();
        Notification notification = notificationRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
            log.info("Marked notification [ID: {}] as read for org [{}]", id, org.getId());
        }

        return toResponse(notification);
    }

    @Transactional
    public int markAllAsRead() {
        Organization org = organizationService.getCurrentUserOrganization();
        int count = notificationRepository.markAllAsReadForOrganization(org.getId(), Instant.now());
        log.info("Marked all notifications ({} updated) as read for org [{}]", count, org.getId());
        return count;
    }

    @Transactional
    public void createAnomalyNotification(CostAnomaly anomaly) {
        if (anomaly == null || anomaly.getOrganization() == null) return;

        // In-app notifications only target CRITICAL and HIGH severity anomalies
        if (anomaly.getSeverity() == null ||
                (!"CRITICAL".equalsIgnoreCase(anomaly.getSeverity().name()) &&
                 !"HIGH".equalsIgnoreCase(anomaly.getSeverity().name()))) {
            return;
        }

        Long orgId = anomaly.getOrganization().getId();
        Long anomalyId = anomaly.getId();

        // Safe deduplication: never generate duplicate notification for the same anomaly record
        if (anomalyId != null && notificationRepository.existsByOrganizationIdAndReferenceTypeAndReferenceId(orgId, "COST_ANOMALY", anomalyId)) {
            log.debug("Notification for anomaly [ID: {}] already exists for org [{}]. Skipping duplicate.", anomalyId, orgId);
            return;
        }

        String title = String.format("%s: %s Cost Anomaly Detected", anomaly.getSeverity(), anomaly.getServiceName());
        String message = String.format("Cost surged by +%s%% over baseline ($%s vs expected $%s). Excess impact: $%s.",
                anomaly.getDeviationPercentage() != null ? anomaly.getDeviationPercentage().toPlainString() : "0.0",
                anomaly.getActualCost() != null ? anomaly.getActualCost().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00",
                anomaly.getExpectedCost() != null ? anomaly.getExpectedCost().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00",
                anomaly.getExcessCost() != null ? anomaly.getExcessCost().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00");

        Notification notification = Notification.builder()
                .organization(anomaly.getOrganization())
                .title(title)
                .message(message)
                .severity(anomaly.getSeverity().name())
                .notificationType("ANOMALY_DETECTED")
                .referenceId(anomalyId)
                .referenceType("COST_ANOMALY")
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Created in-app {} notification for anomaly [ID: {}] in org [{}]", anomaly.getSeverity(), anomalyId, orgId);
    }

    private NotificationResponse toResponse(Notification n) {
        if (n == null) return null;
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .severity(n.getSeverity())
                .notificationType(n.getNotificationType())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .isRead(n.getIsRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

