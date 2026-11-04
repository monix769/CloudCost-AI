package com.cloudcostai.controller;

import com.cloudcostai.dto.CloudAccountRequest;
import com.cloudcostai.entity.CloudProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
class CloudAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/cloud-accounts - Should return list of cloud accounts")
    void shouldReturnCloudAccountsList() throws Exception {
        mockMvc.perform(get("/api/cloud-accounts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/cloud-accounts/999999 - Should return 404 for missing account")
    void shouldReturn404ForMissingAccount() throws Exception {
        mockMvc.perform(get("/api/cloud-accounts/999999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("POST /api/cloud-accounts - Should reject request with missing fields")
    void shouldRejectInvalidAccountRequest() throws Exception {
        CloudAccountRequest invalidRequest = CloudAccountRequest.builder()
                .provider(null)
                .accountId("")
                .accountName("")
                .build();

        mockMvc.perform(post("/api/cloud-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors", notNullValue()));
    }

    @Test
    @DisplayName("POST, PUT and DELETE /api/cloud-accounts - Full CRUD lifecycle")
    void shouldExecuteFullCrudLifecycle() throws Exception {
        String uniqueAccountId = "test-acc-" + System.currentTimeMillis();
        CloudAccountRequest createRequest = CloudAccountRequest.builder()
                .provider(CloudProvider.AWS)
                .accountId(uniqueAccountId)
                .accountName("Integration Test AWS Account")
                .arnOrClientId("arn:aws:iam::123456789012:role/TestRole")
                .isActive(true)
                .build();

        // 1. Create
        String createResponse = mockMvc.perform(post("/api/cloud-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.accountId", is(uniqueAccountId)))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // 2. Read by ID
        mockMvc.perform(get("/api/cloud-accounts/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(createdId.intValue())))
                .andExpect(jsonPath("$.data.accountName", is("Integration Test AWS Account")));

        // 3. Update
        CloudAccountRequest updateRequest = CloudAccountRequest.builder()
                .provider(CloudProvider.AWS)
                .accountId(uniqueAccountId)
                .accountName("Updated AWS Account Name")
                .isActive(false)
                .build();

        mockMvc.perform(put("/api/cloud-accounts/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountName", is("Updated AWS Account Name")))
                .andExpect(jsonPath("$.data.isActive", is(false)));

        // 4. Delete
        mockMvc.perform(delete("/api/cloud-accounts/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 5. Verify deleted
        mockMvc.perform(get("/api/cloud-accounts/" + createdId))
                .andExpect(status().isNotFound());
    }
}
