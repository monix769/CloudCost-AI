package com.cloudcostai.service;

import com.cloudcostai.dto.InviteMemberRequest;
import com.cloudcostai.dto.MemberUpdateRequest;
import com.cloudcostai.dto.OrganizationInvitationResponse;
import com.cloudcostai.dto.OrganizationMemberResponse;
import com.cloudcostai.dto.OrganizationProfileResponse;
import com.cloudcostai.dto.OrganizationUpdateRequest;
import com.cloudcostai.entity.InvitationStatus;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.OrganizationInvitation;
import com.cloudcostai.entity.User;
import com.cloudcostai.entity.UserRole;
import com.cloudcostai.exception.BadRequestException;
import com.cloudcostai.exception.ConflictException;
import com.cloudcostai.repository.OrganizationInvitationRepository;
import com.cloudcostai.repository.OrganizationRepository;
import com.cloudcostai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationManagementServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationInvitationRepository invitationRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private OrganizationManagementService orgManagementService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder()
                .name("Acme Cloud Corp")
                .slug("acme-corp")
                .tier("ENTERPRISE")
                .isActive(true)
                .build();
        testOrg.setId(1L);
    }

    @Test
    @DisplayName("Should retrieve organization profile with member count")
    void shouldRetrieveOrgProfile() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(userRepository.findByOrganizationId(1L)).thenReturn(List.of(
                User.builder().email("admin@acme.com").role("ROLE_ADMIN").build(),
                User.builder().email("user@acme.com").role("ROLE_USER").build()
        ));

        OrganizationProfileResponse profile = orgManagementService.getCurrentOrganizationProfile();

        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("Acme Cloud Corp");
        assertThat(profile.getMemberCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should prevent demoting the sole remaining administrator")
    void shouldPreventDemotingSoleAdmin() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);

        User soleAdmin = User.builder()
                .email("admin@acme.com")
                .role("ROLE_ADMIN")
                .organization(testOrg)
                .isActive(true)
                .build();
        soleAdmin.setId(10L);

        when(userRepository.findByIdAndOrganizationId(eq(10L), eq(1L))).thenReturn(Optional.of(soleAdmin));
        when(userRepository.countByOrganizationIdAndRole(eq(1L), eq("ROLE_ADMIN"))).thenReturn(1L);

        MemberUpdateRequest req = MemberUpdateRequest.builder()
                .role("ROLE_USER")
                .build();

        assertThatThrownBy(() -> orgManagementService.updateMember(10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot demote or deactivate the sole remaining Administrator");
    }

    @Test
    @DisplayName("Should create invitation token for new email")
    void shouldCreateInvitationSuccessfully() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(userRepository.existsByEmailIgnoreCase("newuser@acme.com")).thenReturn(false);
        when(invitationRepository.existsByOrganizationIdAndEmailAndStatus(eq(1L), eq("newuser@acme.com"), eq(InvitationStatus.PENDING)))
                .thenReturn(false);

        when(invitationRepository.save(any(OrganizationInvitation.class))).thenAnswer(inv -> {
            OrganizationInvitation i = inv.getArgument(0);
            i.setId(100L);
            return i;
        });

        InviteMemberRequest req = InviteMemberRequest.builder()
                .email("newuser@acme.com")
                .role("ROLE_USER")
                .build();

        OrganizationInvitationResponse resp = orgManagementService.inviteMember(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getEmail()).isEqualTo("newuser@acme.com");
        assertThat(resp.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Should reject invitation if user already exists")
    void shouldRejectInvitationForExistingUser() {
        when(organizationService.getCurrentUserOrganization()).thenReturn(testOrg);
        when(userRepository.existsByEmailIgnoreCase("existing@acme.com")).thenReturn(true);

        InviteMemberRequest req = InviteMemberRequest.builder()
                .email("existing@acme.com")
                .role("ROLE_USER")
                .build();

        assertThatThrownBy(() -> orgManagementService.inviteMember(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }
}

