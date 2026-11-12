package com.cloudcostai.controller;

import com.cloudcostai.dto.OptimizationStatusUpdateRequest;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.OptimizationPriority;
import com.cloudcostai.entity.OptimizationRecommendation;
import com.cloudcostai.entity.OptimizationRecommendationType;
import com.cloudcostai.entity.OptimizationStatus;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.ResourceUtilizationSnapshot;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.OptimizationRecommendationHistoryRepository;
import com.cloudcostai.repository.OptimizationRecommendationRepository;
import com.cloudcostai.repository.OrganizationRepository;
import com.cloudcostai.repository.ResourceUtilizationSnapshotRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
class OptimizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OptimizationRecommendationRepository recommendationRepository;

    @Autowired
    private OptimizationRecommendationHistoryRepository historyRepository;

    @Autowired
    private ResourceUtilizationSnapshotRepository snapshotRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CloudAccountRepository cloudAccountRepository;

    @Test
    @DisplayName("GET /api/optimization/overview - Should return summary KPIs")
    void shouldReturnOptimizationOverview() throws Exception {
        mockMvc.perform(get("/api/optimization/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalPotentialMonthlySavings", notNullValue()))
                .andExpect(jsonPath("$.data.totalPotentialAnnualSavings", notNullValue()))
                .andExpect(jsonPath("$.data.currency", is("USD")));
    }

    @Test
    @DisplayName("GET /api/optimization/recommendations - Should return recommendations list")
    void shouldReturnRecommendationsList() throws Exception {
        mockMvc.perform(get("/api/optimization/recommendations?page=0&size=10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/optimization/savings-summary - Should return savings breakdown")
    void shouldReturnSavingsSummary() throws Exception {
        mockMvc.perform(get("/api/optimization/savings-summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.byProvider", notNullValue()))
                .andExpect(jsonPath("$.data.byRecommendationType", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/optimization/resource-utilization - Should return telemetry snapshots")
    void shouldReturnResourceUtilization() throws Exception {
        mockMvc.perform(get("/api/optimization/resource-utilization?page=0&size=10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/optimization/commitment-analysis - Should return baseline commitment projection")
    void shouldReturnCommitmentAnalysis() throws Exception {
        mockMvc.perform(get("/api/optimization/commitment-analysis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.baselineMonthlyComputeSpend", notNullValue()))
                .andExpect(jsonPath("$.data.estimated1YearSavingsPercent", is(28.00)))
                .andExpect(jsonPath("$.data.assumptions", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("POST /api/optimization/analyze - Should run optimization scan")
    void shouldTriggerOptimizationAnalysis() throws Exception {
        mockMvc.perform(post("/api/optimization/analyze")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", anyOf(is("SUCCESS"), is("ALREADY_RUNNING"))));
    }

    @Test
    @DisplayName("PATCH /api/optimization/recommendations/{id}/status and GET history")
    void shouldUpdateStatusAndTrackAuditTrail() throws Exception {
        Organization org = organizationRepository.findAll().stream().findFirst().orElseThrow();
        CloudAccount account = cloudAccountRepository.findAll().stream().findFirst().orElseThrow();

        OptimizationRecommendation rec = OptimizationRecommendation.builder()
                .organization(org)
                .cloudAccount(account)
                .resourceId("i-test-lifecycle-001")
                .resourceName("lifecycle-test-instance")
                .resourceType("c5.2xlarge")
                .cloudProvider(CloudProvider.AWS)
                .recommendationType(OptimizationRecommendationType.RIGHTSIZING)
                .currentConfig("c5.2xlarge")
                .suggestedConfig("c5.xlarge")
                .priority(OptimizationPriority.HIGH)
                .status(OptimizationStatus.NEW)
                .reason("Test sustained low CPU")
                .currentMonthlyCost(new BigDecimal("248.2000"))
                .recommendedMonthlyCost(new BigDecimal("124.1000"))
                .estimatedMonthlySavings(new BigDecimal("124.1000"))
                .estimatedAnnualSavings(new BigDecimal("1489.2000"))
                .currency("USD")
                .detectedAt(Instant.now())
                .build();
        OptimizationRecommendation saved = recommendationRepository.save(rec);

        // Transition from NEW to APPROVED
        OptimizationStatusUpdateRequest approveReq = OptimizationStatusUpdateRequest.builder()
                .status(OptimizationStatus.APPROVED)
                .notes("Approved for next maintenance window")
                .build();

        mockMvc.perform(patch("/api/optimization/recommendations/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("APPROVED")))
                .andExpect(jsonPath("$.data.implementationNotes", is("Approved for next maintenance window")));

        // Transition from APPROVED to IMPLEMENTED
        OptimizationStatusUpdateRequest implementReq = OptimizationStatusUpdateRequest.builder()
                .status(OptimizationStatus.IMPLEMENTED)
                .notes("Downsized during Tuesday sprint window")
                .realizedMonthlySavings(new BigDecimal("125.0000"))
                .build();

        mockMvc.perform(patch("/api/optimization/recommendations/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(implementReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("IMPLEMENTED")))
                .andExpect(jsonPath("$.data.realizedMonthlySavings", is(125.0000)))
                .andExpect(jsonPath("$.data.realizedAnnualSavings", is(1500.0000)));

        // Verify history endpoint
        mockMvc.perform(get("/api/optimization/recommendations/" + saved.getId() + "/history")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("GET /api/optimization/export - Should return CSV attachment")
    void shouldExportOptimizationCsv() throws Exception {
        mockMvc.perform(get("/api/optimization/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("cost-optimization-recommendations.csv")))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("ID,Cloud Provider,Cloud Account,Resource ID")));
    }
}

