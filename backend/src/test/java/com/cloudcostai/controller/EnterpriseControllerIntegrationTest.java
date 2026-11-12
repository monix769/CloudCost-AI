package com.cloudcostai.controller;

import com.cloudcostai.dto.InviteMemberRequest;
import com.cloudcostai.dto.MemberUpdateRequest;
import com.cloudcostai.dto.OrganizationUpdateRequest;
import com.cloudcostai.dto.ReportGenerateRequest;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.ReportFormat;
import com.cloudcostai.entity.ReportType;
import com.cloudcostai.entity.User;
import com.cloudcostai.repository.OrganizationRepository;
import com.cloudcostai.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
class EnterpriseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /api/reports/generate (PDF) and GET /api/reports - Should generate and list reports")
    void shouldGenerateAndListReports() throws Exception {
        ReportGenerateRequest req = ReportGenerateRequest.builder()
                .reportName("Q4_FinOps_Summary")
                .reportType(ReportType.EXECUTIVE_SUMMARY)
                .fileFormat(ReportFormat.PDF)
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now())
                .provider(CloudProvider.AWS)
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.reportName", containsString("Q4_FinOps_Summary")))
                .andExpect(jsonPath("$.data.fileFormat", is("PDF")))
                .andExpect(jsonPath("$.data.generationStatus", is("COMPLETED")));

        mockMvc.perform(get("/api/reports?page=0&size=10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST /api/reports/generate (CSV) - Should generate CSV report")
    void shouldGenerateCsvReport() throws Exception {
        ReportGenerateRequest req = ReportGenerateRequest.builder()
                .reportName("Provider_Breakdown_CSV")
                .reportType(ReportType.PROVIDER_COMPARISON)
                .fileFormat(ReportFormat.CSV)
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fileFormat", is("CSV")))
                .andExpect(jsonPath("$.data.generationStatus", is("COMPLETED")));
    }

    @Test
    @DisplayName("GET & PUT /api/organizations/current - Organization Profile")
    void shouldGetAndUpdateOrganization() throws Exception {
        mockMvc.perform(get("/api/organizations/current")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", notNullValue()))
                .andExpect(jsonPath("$.data.memberCount", greaterThanOrEqualTo(1)));

        OrganizationUpdateRequest updateReq = OrganizationUpdateRequest.builder()
                .name("Acme Cloud Corp Enterprise")
                .tier("ENTERPRISE")
                .build();

        mockMvc.perform(put("/api/organizations/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Acme Cloud Corp Enterprise")));
    }

    @Test
    @DisplayName("GET /api/organizations/current/members & POST invitation")
    void shouldManageMembersAndInvitations() throws Exception {
        mockMvc.perform(get("/api/organizations/current/members")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        String uniqueInviteEmail = "finops.invite." + System.currentTimeMillis() + "@acme.com";
        InviteMemberRequest inviteReq = InviteMemberRequest.builder()
                .email(uniqueInviteEmail)
                .role("ROLE_USER")
                .build();

        mockMvc.perform(post("/api/organizations/current/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(uniqueInviteEmail)))
                .andExpect(jsonPath("$.data.status", is("PENDING")));

        mockMvc.perform(get("/api/organizations/current/invitations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/audit-logs - Should list append-only audit trail")
    void shouldListAuditLogs() throws Exception {
        mockMvc.perform(get("/api/audit-logs?page=0&size=10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @WithMockUser(username = "regular.user@cloudcostai.local", roles = {"USER"})
    @DisplayName("RBAC: Regular USER should be forbidden from accessing admin audit logs and organization update")
    void shouldForbidRegularUserFromAdminEndpoints() throws Exception {
        // Audit logs endpoint is ADMIN only
        mockMvc.perform(get("/api/audit-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Organization update is ADMIN only
        OrganizationUpdateRequest updateReq = OrganizationUpdateRequest.builder()
                .name("Unauthorized Change")
                .build();

        mockMvc.perform(put("/api/organizations/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }
}

