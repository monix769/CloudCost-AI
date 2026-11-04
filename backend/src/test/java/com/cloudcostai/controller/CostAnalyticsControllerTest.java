package com.cloudcostai.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
class CostAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/costs/summary - Should return cost summary calculations")
    void shouldReturnCostSummary() throws Exception {
        mockMvc.perform(get("/api/costs/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalCost", notNullValue()))
                .andExpect(jsonPath("$.data.currency", is("USD")))
                .andExpect(jsonPath("$.data.totalRecords", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/costs/by-provider - Should return provider cost breakdown")
    void shouldReturnCostsByProvider() throws Exception {
        mockMvc.perform(get("/api/costs/by-provider")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/costs/by-service - Should return service breakdown with limit")
    void shouldReturnCostsByServiceWithLimit() throws Exception {
        mockMvc.perform(get("/api/costs/by-service?limit=5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/costs/trends - Should return monthly spend trend series")
    void shouldReturnMonthlyCostTrends() throws Exception {
        mockMvc.perform(get("/api/costs/trends")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }
}
