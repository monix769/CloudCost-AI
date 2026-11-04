package com.cloudcostai.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RoleBasedAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ROLE_USER should be rejected with 403 Forbidden when accessing /api/admin/users")
    @WithMockUser(username = "standard.user@example.com", roles = {"USER"})
    void shouldDenyAccessToAdminEndpointForRoleUser() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    @DisplayName("ROLE_ADMIN should be granted access with 200 OK when accessing /api/admin/users")
    @WithMockUser(username = "admin@cloudcostai.local", roles = {"ADMIN"})
    void shouldAllowAccessToAdminEndpointForRoleAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
