package com.cloudcostai.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / - Should return 200 OK with platform metadata and endpoints")
    void shouldReturnRootEndpointInfo() throws Exception {
        mockMvc.perform(get("/")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.data.platform", is("CloudCost AI")))
                .andExpect(jsonPath("$.data.status", is("ONLINE")))
                .andExpect(jsonPath("$.data.endpoints.health", is("GET /api/health")))
                .andExpect(jsonPath("$.data.endpoints.status", is("GET /api/status")));
    }
}
