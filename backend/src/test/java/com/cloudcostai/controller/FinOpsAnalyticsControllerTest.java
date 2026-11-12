package com.cloudcostai.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class FinOpsAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Unauthenticated request to /api/analytics/overview should be rejected with 401")
    @WithAnonymousUser
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/analytics/overview - Should return executive FinOps summary")
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    void shouldReturnAnalyticsOverview() throws Exception {
        mockMvc.perform(get("/api/analytics/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.summary", notNullValue()))
                .andExpect(jsonPath("$.data.monthOverMonth", notNullValue()))
                .andExpect(jsonPath("$.data.providerDistribution", notNullValue()))
                .andExpect(jsonPath("$.data.budgetHealth", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/analytics/spending-trends - Should return flexible trend series")
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    void shouldReturnSpendingTrends() throws Exception {
        mockMvc.perform(get("/api/analytics/spending-trends?granularity=MONTHLY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.granularity", is("MONTHLY")))
                .andExpect(jsonPath("$.data.points", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/analytics/forecast - Should generate statistical projection")
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    void shouldReturnCostForecast() throws Exception {
        mockMvc.perform(get("/api/analytics/forecast?horizon=30D&method=EXPONENTIAL_SMOOTHING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.method", is("EXPONENTIAL_SMOOTHING")))
                .andExpect(jsonPath("$.data.horizon", is("30 Days")))
                .andExpect(jsonPath("$.data.estimatedTotalForecastCost", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/analytics/budget-utilization - Should return consumption health")
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    void shouldReturnBudgetUtilization() throws Exception {
        mockMvc.perform(get("/api/analytics/budget-utilization")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalAllocatedBudget", notNullValue()))
                .andExpect(jsonPath("$.data.overallHealthStatus", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/analytics/cost-allocation - Should allocate spend by dimension")
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    void shouldReturnCostAllocation() throws Exception {
        mockMvc.perform(get("/api/analytics/cost-allocation?dimension=PROVIDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.dimension", is("PROVIDER")))
                .andExpect(jsonPath("$.data.items", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/analytics/insights - Should return prioritized FinOps insights")
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    void shouldReturnFinOpsInsights() throws Exception {
        mockMvc.perform(get("/api/analytics/insights")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/analytics/export/spending-trends - Should stream CSV attachment")
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    void shouldExportTrendsCsv() throws Exception {
        mockMvc.perform(get("/api/analytics/export/spending-trends?granularity=MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"spending-trends.csv\"")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @DisplayName("GET /api/analytics/export/insights - Should stream CSV attachment")
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    void shouldExportInsightsCsv() throws Exception {
        mockMvc.perform(get("/api/analytics/export/insights"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"finops-insights.csv\"")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }
}

