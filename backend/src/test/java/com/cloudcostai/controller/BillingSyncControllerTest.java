package com.cloudcostai.controller;

import com.cloudcostai.cloud.service.BillingIngestionService;
import com.cloudcostai.dto.SyncHistoryResponse;
import com.cloudcostai.entity.SyncHistory;
import com.cloudcostai.mapper.SyncHistoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BillingSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingIngestionService billingIngestionService;

    @MockBean
    private SyncHistoryMapper syncHistoryMapper;

    @Test
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    @DisplayName("POST /api/billing/sync/{id} should return 200 and sync summary")
    void shouldTriggerSyncSuccessfully() throws Exception {
        SyncHistory history = SyncHistory.builder()
                .provider("AWS")
                .syncStatus("SUCCESS")
                .recordsFetched(10)
                .recordsInserted(8)
                .recordsSkipped(2)
                .build();
        history.setId(1L);

        SyncHistoryResponse response = SyncHistoryResponse.builder()
                .id(1L)
                .provider("AWS")
                .syncStatus("SUCCESS")
                .recordsFetched(10)
                .recordsInserted(8)
                .recordsSkipped(2)
                .build();

        when(billingIngestionService.synchronizeAccount(eq(1L), any(), any(), anyBoolean())).thenReturn(history);
        when(syncHistoryMapper.toResponse(history)).thenReturn(response);

        mockMvc.perform(post("/api/billing/sync/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.syncStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.recordsInserted").value(8))
                .andExpect(jsonPath("$.data.recordsSkipped").value(2));
    }

    @Test
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    @DisplayName("GET /api/billing/sync/history should return paginated sync logs")
    void shouldReturnPaginatedSyncHistory() throws Exception {
        SyncHistory history = SyncHistory.builder().provider("AZURE").syncStatus("SUCCESS").build();
        history.setId(5L);

        SyncHistoryResponse response = SyncHistoryResponse.builder().id(5L).provider("AZURE").syncStatus("SUCCESS").build();

        Page<SyncHistory> page = new PageImpl<>(List.of(history));
        when(billingIngestionService.getSyncHistory(eq(null), any(Pageable.class))).thenReturn(page);
        when(syncHistoryMapper.toResponse(history)).thenReturn(response);

        mockMvc.perform(get("/api/billing/sync/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].provider").value("AZURE"));
    }

    @Test
    @DisplayName("Unauthenticated request to /api/billing/sync/1 should return 401 Unauthorized")
    void shouldRejectUnauthenticatedSync() throws Exception {
        mockMvc.perform(post("/api/billing/sync/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
