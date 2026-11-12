package com.cloudcostai.controller;

import com.cloudcostai.dto.AnomalyConfigRequest;
import com.cloudcostai.dto.AnomalyStatusUpdateRequest;
import com.cloudcostai.entity.AnomalySeverity;
import com.cloudcostai.entity.AnomalyStatus;
import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CostAnomaly;
import com.cloudcostai.entity.Notification;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.CostAnomalyRepository;
import com.cloudcostai.repository.NotificationRepository;
import com.cloudcostai.repository.OrganizationRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
class AnomalyControllerM7Test {

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

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("GET /api/anomalies/summary - Should return anomaly KPI metrics")
    void shouldReturnAnomalyKpiSummary() throws Exception {
        mockMvc.perform(get("/api/anomalies/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalAnomalies", notNullValue()))
                .andExpect(jsonPath("$.data.totalExcessCost", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/anomalies/trends - Should return daily anomaly trends")
    void shouldReturnAnomalyTrends() throws Exception {
        mockMvc.perform(get("/api/anomalies/trends")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.trends", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/anomalies/detect - Should execute anomaly detection scan")
    void shouldTriggerAnomalyDetection() throws Exception {
        mockMvc.perform(post("/api/anomalies/detect")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.evaluatedCount", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/anomalies/{id}/history - Should return transition audit trail")
    void shouldReturnAnomalyHistory() throws Exception {
        Organization org = organizationRepository.findAll().stream().findFirst().orElseThrow();
        CloudAccount account = cloudAccountRepository.findAll().stream().findFirst().orElseThrow();

        CostAnomaly anomaly = CostAnomaly.builder()
                .organization(org)
                .cloudAccount(account)
                .serviceName("Amazon DynamoDB")
                .expectedCost(new BigDecimal("80.0000"))
                .actualCost(new BigDecimal("320.0000"))
                .excessCost(new BigDecimal("240.0000"))
                .deviationPercentage(new BigDecimal("300.00"))
                .severity(AnomalySeverity.HIGH)
                .status(AnomalyStatus.OPEN)
                .detectedAt(Instant.now())
                .build();
        CostAnomaly saved = costAnomalyRepository.save(anomaly);

        // Update status to ACKNOWLEDGED with note
        AnomalyStatusUpdateRequest updateReq = AnomalyStatusUpdateRequest.builder()
                .status(AnomalyStatus.ACKNOWLEDGED)
                .notes("Reviewed read capacity units spike")
                .build();

        mockMvc.perform(patch("/api/anomalies/" + saved.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // Fetch history
        mockMvc.perform(get("/api/anomalies/" + saved.getId() + "/history")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].newStatus", is("ACKNOWLEDGED")));
    }

    @Test
    @DisplayName("GET & PUT /api/anomalies/config - Should inspect and update detection parameters")
    void shouldGetAndUpdateAnomalyConfig() throws Exception {
        mockMvc.perform(get("/api/anomalies/config")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.zScoreThreshold", notNullValue()));

        AnomalyConfigRequest updateReq = AnomalyConfigRequest.builder()
                .zScoreThreshold(new BigDecimal("3.00"))
                .movingAvgWindowDays(21)
                .movingAvgDeviationPercent(new BigDecimal("45.00"))
                .spendingSpikePercent(new BigDecimal("60.00"))
                .spendingSpikeMinAmount(new BigDecimal("75.0000"))
                .budgetWarningThresholdPercent(new BigDecimal("92.00"))
                .budgetCriticalThresholdPercent(new BigDecimal("100.00"))
                .isAutomatedDetectionEnabled(true)
                .build();

        mockMvc.perform(put("/api/anomalies/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.zScoreThreshold", is(3.0)))
                .andExpect(jsonPath("$.data.movingAvgWindowDays", is(21)));
    }

    @Test
    @DisplayName("GET /api/notifications - Should return in-app notifications and unread count")
    void shouldHandleInAppNotifications() throws Exception {
        Organization org = organizationRepository.findAll().stream().findFirst().orElseThrow();

        Notification notif = Notification.builder()
                .organization(org)
                .title("CRITICAL: Spend Spike")
                .message("Surged +250%")
                .severity("CRITICAL")
                .notificationType("ANOMALY_DETECTED")
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        Notification saved = notificationRepository.save(notif);

        // Check unread count
        mockMvc.perform(get("/api/notifications/unread-count")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.unreadCount", greaterThanOrEqualTo(1)));

        // Mark single as read
        mockMvc.perform(patch("/api/notifications/" + saved.getId() + "/read")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead", is(true)));

        // Mark all as read
        mockMvc.perform(post("/api/notifications/read-all")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}

