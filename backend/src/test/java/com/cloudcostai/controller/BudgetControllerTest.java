package com.cloudcostai.controller;

import com.cloudcostai.dto.BudgetRequest;
import com.cloudcostai.entity.BudgetPeriod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/budgets - Should return budget list with utilization metrics")
    void shouldReturnBudgetsList() throws Exception {
        mockMvc.perform(get("/api/budgets")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/budgets - Should reject zero or negative amount limit")
    void shouldRejectZeroAmountBudget() throws Exception {
        BudgetRequest request = BudgetRequest.builder()
                .name("Invalid Zero Budget")
                .amountLimit(new BigDecimal("0.00")) // Invalid: min 0.01
                .period(BudgetPeriod.MONTHLY)
                .alertThresholdPercentage(new BigDecimal("80.00"))
                .build();

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.amountLimit", notNullValue()));
    }

    @Test
    @DisplayName("POST, PUT and DELETE /api/budgets - Full lifecycle with live utilization")
    void shouldExecuteBudgetLifecycle() throws Exception {
        String budgetName = "Test Budget " + System.currentTimeMillis();
        BudgetRequest createRequest = BudgetRequest.builder()
                .name(budgetName)
                .amountLimit(new BigDecimal("15000.00"))
                .currency("USD")
                .period(BudgetPeriod.MONTHLY)
                .alertThresholdPercentage(new BigDecimal("75.00"))
                .status("ACTIVE")
                .build();

        // 1. Create
        String createResponse = mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", is(budgetName)))
                .andExpect(jsonPath("$.data.utilizationPercentage", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long budgetId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // 2. Read by ID
        mockMvc.perform(get("/api/budgets/" + budgetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(budgetId.intValue())))
                .andExpect(jsonPath("$.data.amountLimit", is(15000.00)));

        // 3. Update
        BudgetRequest updateRequest = BudgetRequest.builder()
                .name(budgetName + " - Revised")
                .amountLimit(new BigDecimal("20000.00"))
                .currency("USD")
                .period(BudgetPeriod.MONTHLY)
                .alertThresholdPercentage(new BigDecimal("85.00"))
                .status("ACTIVE")
                .build();

        mockMvc.perform(put("/api/budgets/" + budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is(budgetName + " - Revised")))
                .andExpect(jsonPath("$.data.amountLimit", is(20000.00)));

        // 4. Delete
        mockMvc.perform(delete("/api/budgets/" + budgetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 5. Verify deleted
        mockMvc.perform(get("/api/budgets/" + budgetId))
                .andExpect(status().isNotFound());
    }
}
