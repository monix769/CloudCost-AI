package com.cloudcostai.controller;

import com.cloudcostai.cloud.provider.ConnectionValidationResult;
import com.cloudcostai.cloud.provider.ProviderCapabilities;
import com.cloudcostai.cloud.provider.ProviderCredentials;
import com.cloudcostai.cloud.service.CloudProviderService;
import com.cloudcostai.entity.CloudProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CloudProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudProviderService cloudProviderService;

    @Test
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    @DisplayName("GET /api/cloud-providers should return list of supported providers")
    void shouldReturnSupportedProviders() throws Exception {
        ProviderCapabilities awsCap = ProviderCapabilities.builder()
                .provider(CloudProvider.AWS)
                .providerName("Amazon Web Services")
                .supportedApi("AWS Cost Explorer API")
                .liveSupported(true)
                .build();

        when(cloudProviderService.getAllCapabilities()).thenReturn(List.of(awsCap));

        mockMvc.perform(get("/api/cloud-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].provider").value("AWS"))
                .andExpect(jsonPath("$.data[0].providerName").value("Amazon Web Services"));
    }

    @Test
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    @DisplayName("POST /api/cloud-providers/{provider}/test-connection should validate candidate credentials")
    void shouldTestCredentialsAdHoc() throws Exception {
        ConnectionValidationResult result = ConnectionValidationResult.builder()
                .valid(true)
                .message("Test validation succeeded")
                .build();

        when(cloudProviderService.testCredentials(eq(CloudProvider.AWS), any(ProviderCredentials.class)))
                .thenReturn(result);

        mockMvc.perform(post("/api/cloud-providers/AWS/test-connection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"awsAccessKeyId\":\"AKIAIOSFODNN7EXAMPLE\",\"awsSecretAccessKey\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true));
    }
}
