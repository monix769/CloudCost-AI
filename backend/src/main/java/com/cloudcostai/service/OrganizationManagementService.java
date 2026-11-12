package com.cloudcostai.service;

import com.cloudcostai.dto.InviteMemberRequest;
import com.cloudcostai.dto.MemberUpdateRequest;
import com.cloudcostai.dto.OrganizationInvitationResponse;
import com.cloudcostai.dto.OrganizationMemberResponse;
import com.cloudcostai.dto.OrganizationProfileResponse;
import com.cloudcostai.dto.OrganizationUpdateRequest;
import com.cloudcostai.entity.AuditActionType;
import com.cloudcostai.entity.InvitationStatus;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.OrganizationInvitation;
import com.cloudcostai.entity.User;
import com.cloudcostai.entity.UserRole;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.exception.ConflictException;
import com.cloudcostai.exception.ResourceNotFoundException;
import com.cloudcostai.repository.OrganizationInvitationRepository;
import com.cloudcostai.repository.OrganizationRepository;
import com.cloudcostai.repository.UserRepository;
import com.cloudcostai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationManagementService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationInvitationRepository invitationRepository;
    private final OrganizationService organizationService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public OrganizationProfileResponse getCurrentOrganizationProfile() {
        Organization org = organizationService.getCurrentUserOrganization();
        long memberCount = userRepository.findByOrganizationId(org.getId()).size();

        return OrganizationProfileResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .slug(org.getSlug())
                .tier(org.getTier())
                .isActive(org.getIsActive())
                .memberCount(memberCount)
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }

    @Transactional
    public OrganizationProfileResponse updateCurrentOrganization(OrganizationUpdateRequest request) {
        Organization org = organizationService.getCurrentUserOrganization();

        String oldName = org.getName();
        org.setName(request.getName().trim());
        if (request.getTier() != null && !request.getTier().trim().isEmpty()) {
            org.setTier(request.getTier().trim());
        }

        Organization updated = organizationRepository.save(org);
        long memberCount = userRepository.findByOrganizationId(updated.getId()).size();

        auditLogService.logCurrent(
                AuditActionType.ORGANIZATION_UPDATED,
                "ORGANIZATION",
                updated.getId().toString(),
                String.format("Updated organization name from '%s' to '%s', tier: %s", oldName, updated.getName(), updated.getTier())
        );

        log.info("Updated organization [{}] profile by current user", updated.getId());
        return OrganizationProfileResponse.builder()
                .id(updated.getId())
                .name(updated.getName())
                .slug(updated.getSlug())
                .tier(updated.getTier())
                .isActive(updated.getIsActive())
                .memberCount(memberCount)
                .createdAt(updated.getCreatedAt())
                .updatedAt(updated.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> getOrganizationMembers() {
        Organization org = organizationService.getCurrentUserOrganization();
        List<User> members = userRepository.findByOrganizationId(org.getId());

        return members.stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public OrganizationMemberResponse updateMember(Long memberId, MemberUpdateRequest request) {
        Organization org = organizationService.getCurrentUserOrganization();
        User member = userRepository.findByIdAndOrganizationId(memberId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", memberId));

        Long currentUserId = getCurrentUserId();

        // Safety guard: cannot deactivate oneself
        if (request.getIsActive() != null && !request.getIsActive() && member.getId().equals(currentUserId)) {
            throw new BadRequestException("You cannot deactivate your own account.");
        }

        // Safety guard: cannot demote or deactivate the last remaining admin
        if ("ROLE_ADMIN".equalsIgnoreCase(member.getRole())) {
            boolean isDemoting = request.getRole() != null && !"ROLE_ADMIN".equalsIgnoreCase(request.getRole());
            boolean isDeactivating = Boolean.FALSE.equals(request.getIsActive());

            if (isDemoting || isDeactivating) {
                long adminCount = userRepository.countByOrganizationIdAndRole(org.getId(), "ROLE_ADMIN");
                if (adminCount <= 1) {
                    throw new BadRequestException("Cannot demote or deactivate the sole remaining Administrator of this organization.");
                }
            }
        }

        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            String roleVal = UserRole.fromString(request.getRole()).getValue();
            member.setRole(roleVal);
            auditLogService.logCurrent(
                    AuditActionType.MEMBER_ROLE_UPDATED,
                    "USER",
                    member.getId().toString(),
                    String.format("Changed role of member %s to %s", member.getEmail(), roleVal)
            );
        }

        if (request.getIsActive() != null) {
            member.setIsActive(request.getIsActive());
            auditLogService.logCurrent(
                    AuditActionType.MEMBER_STATUS_UPDATED,
                    "USER",
                    member.getId().toString(),
                    String.format("Set active status of member %s to %b", member.getEmail(), request.getIsActive())
            );
        }

        User updated = userRepository.save(member);
        return toMemberResponse(updated);
    }

    @Transactional
    public void removeMember(Long memberId) {
        Organization org = organizationService.getCurrentUserOrganization();
        User member = userRepository.findByIdAndOrganizationId(memberId, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", memberId));

        Long currentUserId = getCurrentUserId();
        if (member.getId().equals(currentUserId)) {
            throw new BadRequestException("You cannot remove yourself from the organization.");
        }

        if ("ROLE_ADMIN".equalsIgnoreCase(member.getRole())) {
            long adminCount = userRepository.countByOrganizationIdAndRole(org.getId(), "ROLE_ADMIN");
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot remove the sole remaining Administrator of this organization.");
            }
        }

        // Deactivate member safely to preserve historical audit references
        member.setIsActive(false);
        userRepository.save(member);

        auditLogService.logCurrent(
                AuditActionType.MEMBER_REMOVED,
                "USER",
                member.getId().toString(),
                String.format("Removed member %s from organization", member.getEmail())
        );

        log.info("Member [{}] deactivated/removed from org [{}]", memberId, org.getId());
    }

    @Transactional
    public OrganizationInvitationResponse inviteMember(InviteMemberRequest request) {
        Organization org = organizationService.getCurrentUserOrganization();
        String email = request.getEmail().trim().toLowerCase();

        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with this email address already exists.");
        }

        // Check if pending invitation exists
        if (invitationRepository.existsByOrganizationIdAndEmailAndStatus(org.getId(), email, InvitationStatus.PENDING)) {
            throw new ConflictException("A pending invitation already exists for this email address.");
        }

        String role = UserRole.fromString(request.getRole()).getValue();
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .organization(org)
                .email(email)
                .role(role)
                .invitedByUserId(getCurrentUserId())
                .invitationToken(token)
                .status(InvitationStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        OrganizationInvitation saved = invitationRepository.save(invitation);

        auditLogService.logCurrent(
                AuditActionType.MEMBER_INVITED,
                "INVITATION",
                saved.getId().toString(),
                String.format("Invited %s with role %s", email, role)
        );

        return toInvitationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrganizationInvitationResponse> getInvitations() {
        Organization org = organizationService.getCurrentUserOrganization();
        List<OrganizationInvitation> invitations = invitationRepository.findByOrganizationIdOrderByCreatedAtDesc(org.getId());
        return invitations.stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private OrganizationMemberResponse toMemberResponse(User u) {
        if (u == null) return null;
        return OrganizationMemberResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .fullName(u.getFullName())
                .role(u.getRole())
                .isActive(u.getIsActive())
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private OrganizationInvitationResponse toInvitationResponse(OrganizationInvitation i) {
        if (i == null) return null;
        return OrganizationInvitationResponse.builder()
                .id(i.getId())
                .email(i.getEmail())
                .role(i.getRole())
                .status(i.getStatus() != null ? i.getStatus().name() : null)
                .expiresAt(i.getExpiresAt())
                .createdAt(i.getCreatedAt())
                .build();
    }
}

