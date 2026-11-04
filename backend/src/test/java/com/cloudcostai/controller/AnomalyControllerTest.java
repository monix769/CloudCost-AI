package com.cloudcostai.controller;

import com.cloudcostai.dto.AnomalyStatusUpdateRequest;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CostAnomaly;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.CostAnomalyRepository;
import com.cloudcostai.repository.OrganizationRepository;
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
import java.time.Instant;

import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
class AnomalyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CostAnomalyRepository costAnomalyRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CloudAccountRepository cloudAccountRepository;

    @Test
    @DisplayName("GET /api/anomalies - Should return list of anomalies with filtering")
    void shouldReturnAnomaliesList() throws Exception {
        mockMvc.perform(get("/api/anomalies?severity=HIGH")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("PATCH /api/anomalies/{id}/status - Should update status to ACKNOWLEDGED")
    void shouldUpdateAnomalyStatus() throws Exception {
        Organization org = organizationRepository.findAll().stream().findFirst().orElseThrow();
        CloudAccount account = cloudAccountRepository.findAll().stream().findFirst().orElseThrow();

        CostAnomaly anomaly = CostAnomaly.builder()
                .organization(org)
                .cloudAccount(account)
                .serviceName("Amazon RDS")
                .expectedCost(new BigDecimal("100.0000"))
                .actualCost(new BigDecimal("450.0000"))
                .deviationPercentage(new BigDecimal("350.00"))
                .severity(AnomalySeverity.HIGH)
                .status(AnomalyStatus.OPEN)
                .detectedAt(Instant.now())
                .rootCauseSummary("Spike in DB I/O IOPS read capacity")
                .build();

        CostAnomaly saved = costAnomalyRepository.save(anomaly);

        AnomalyStatusUpdateRequest updateReq = AnomalyStatusUpdateRequest.builder()
                .status(AnomalyStatus.ACKNOWLEDGED)
                .build();

        mockMvc.perform(patch("/api/anomalies/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("ACKNOWLEDGED")));
    }

    @Test
    @DisplayName("GET /api/anomalies/999999 - Should return 404 for non-existent anomaly")
    void shouldReturn404ForMissingAnomaly() throws Exception {
        mockMvc.perform(get("/api/anomalies/999999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }
}
