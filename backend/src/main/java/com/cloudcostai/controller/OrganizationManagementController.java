package com.cloudcostai.controller;

import com.cloudcostai.dto.ApiResponse;
import com.cloudcostai.dto.InviteMemberRequest;
import com.cloudcostai.dto.MemberUpdateRequest;
import com.cloudcostai.dto.OrganizationInvitationResponse;
import com.cloudcostai.dto.OrganizationMemberResponse;
import com.cloudcostai.dto.OrganizationProfileResponse;
import com.cloudcostai.dto.OrganizationUpdateRequest;
import com.cloudcostai.service.OrganizationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organization Management", description = "Endpoints for managing organization profile, team members, roles, and invitation workflows")
public class OrganizationManagementController {

    private final OrganizationManagementService organizationManagementService;

    @GetMapping("/current")
    @Operation(summary = "Get current organization profile", description = "Retrieve profile details, tier, and member statistics for the authenticated user's organization")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>> getCurrentOrganization() {
        log.debug("GET /api/organizations/current");
        OrganizationProfileResponse profile = organizationManagementService.getCurrentOrganizationProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update current organization profile", description = "Update organization name and tier settings (Administrator only)")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>> updateCurrentOrganization(
            @Valid @RequestBody OrganizationUpdateRequest request) {
        log.info("PUT /api/organizations/current - name: {}", request.getName());
        OrganizationProfileResponse updated = organizationManagementService.updateCurrentOrganization(request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @GetMapping("/current/members")
    @Operation(summary = "List organization team members", description = "Retrieve all registered users and their roles within the current organization")
    public ResponseEntity<ApiResponse<List<OrganizationMemberResponse>>> getMembers() {
        log.debug("GET /api/organizations/current/members");
        List<OrganizationMemberResponse> members = organizationManagementService.getOrganizationMembers();
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PatchMapping("/current/members/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update member role or status", description = "Modify role permissions (ADMIN, USER) or activate/deactivate a team member (Administrator only)")
    public ResponseEntity<ApiResponse<OrganizationMemberResponse>> updateMember(
            @Parameter(description = "Member User ID") @PathVariable Long id,
            @RequestBody MemberUpdateRequest request) {
        log.info("PATCH /api/organizations/current/members/{} - role: {}, active: {}", id, request.getRole(), request.getIsActive());
        OrganizationMemberResponse updated = organizationManagementService.updateMember(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/current/members/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove organization member", description = "Deactivate and detach a member from the organization (Administrator only)")
    public ResponseEntity<ApiResponse<String>> removeMember(
            @Parameter(description = "Member User ID") @PathVariable Long id) {
        log.info("DELETE /api/organizations/current/members/{}", id);
        organizationManagementService.removeMember(id);
        return ResponseEntity.ok(ApiResponse.success("Member removed successfully"));
    }

    @PostMapping("/current/invitations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Invite team member", description = "Generate a secure invitation token for onboarding a new user to the organization (Administrator only)")
    public ResponseEntity<ApiResponse<OrganizationInvitationResponse>> inviteMember(
            @Valid @RequestBody InviteMemberRequest request) {
        log.info("POST /api/organizations/current/invitations - email: {}", request.getEmail());
        OrganizationInvitationResponse invitation = organizationManagementService.inviteMember(request);
        return ResponseEntity.ok(ApiResponse.success(invitation));
    }

    @GetMapping("/current/invitations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List pending invitations", description = "Retrieve all active invitations issued for the organization (Administrator only)")
    public ResponseEntity<ApiResponse<List<OrganizationInvitationResponse>>> getInvitations() {
        log.debug("GET /api/organizations/current/invitations");
        List<OrganizationInvitationResponse> invitations = organizationManagementService.getInvitations();
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }
}

