package com.cloudcostai.service;

import com.cloudcostai.dto.AuditLogResponse;
import com.cloudcostai.entity.AuditActionType;
import com.cloudcostai.entity.AuditLog;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.repository.AuditLogRepository;
import com.cloudcostai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final OrganizationService organizationService;

    @Transactional
    public AuditLog logEvent(
            Organization organization,
            Long userId,
            String userEmail,
            AuditActionType actionType,
            String resourceType,
            String resourceId,
            String ipAddress,
            String details,
            String status) {

        if (organization == null) {
            log.warn("Cannot record audit log for null organization");
            return null;
        }

        // Security sanitize: Ensure no passwords, tokens, or cloud secrets ever enter the audit log
        String safeDetails = sanitizeDetails(details);

        AuditLog auditLog = AuditLog.builder()
                .organization(organization)
                .userId(userId)
                .userEmail(userEmail != null ? userEmail : "SYSTEM")
                .actionType(actionType)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .details(safeDetails)
                .status(status != null ? status : "SUCCESS")
                .createdAt(Instant.now())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("AuditLog recorded: {} on {} [{}] by {}", actionType, resourceType, resourceId, userEmail);
        return saved;
    }

    @Transactional
    public AuditLog logCurrent(
            AuditActionType actionType,
            String resourceType,
            String resourceId,
            String details) {

        Organization org = organizationService.getCurrentUserOrganization();
        Long userId = null;
        String userEmail = "ANONYMOUS";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.getId();
            userEmail = principal.getEmail();
        } else if (auth != null && auth.getName() != null) {
            userEmail = auth.getName();
        }

        return logEvent(org, userId, userEmail, actionType, resourceType, resourceId, null, details, "SUCCESS");
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            AuditActionType actionType,
            String userEmail,
            String resourceType,
            Instant startInstant,
            Instant endInstant,
            Pageable pageable) {

        Organization org = organizationService.getCurrentUserOrganization();
        String safeEmail = (userEmail != null && !userEmail.trim().isEmpty()) ? userEmail.trim() : null;
        String safeResource = (resourceType != null && !resourceType.trim().isEmpty()) ? resourceType.trim() : null;

        Page<AuditLog> page = auditLogRepository.findFilteredAuditLogs(
                org.getId(), actionType, safeEmail, safeResource, startInstant, endInstant, pageable);

        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long id) {
        Organization org = organizationService.getCurrentUserOrganization();
        AuditLog logEntity = auditLogRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));
        return toResponse(logEntity);
    }

    private AuditLogResponse toResponse(AuditLog a) {
        if (a == null) return null;
        return AuditLogResponse.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .userEmail(a.getUserEmail())
                .actionType(a.getActionType() != null ? a.getActionType().name() : null)
                .resourceType(a.getResourceType())
                .resourceId(a.getResourceId())
                .ipAddress(a.getIpAddress())
                .details(a.getDetails())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private String sanitizeDetails(String details) {
        if (details == null) return null;
        return details
                .replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]+(\")", "$1***$2")
                .replaceAll("(?i)(\"token\"\\s*:\\s*\")[^\"]+(\")", "$1***$2")
                .replaceAll("(?i)(\"clientSecret\"\\s*:\\s*\")[^\"]+(\")", "$1***$2")
                .replaceAll("(?i)(\"accessKey\"\\s*:\\s*\")[^\"]+(\")", "$1***$2")
                .replaceAll("(?i)(\"secretKey\"\\s*:\\s*\")[^\"]+(\")", "$1***$2");
    }
}

