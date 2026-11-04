package com.cloudcostai.controller;

import com.cloudcostai.dto.LoginRequest;
import com.cloudcostai.dto.RegisterRequest;
import com.cloudcostai.entity.User;
import com.cloudcostai.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/auth/register - Successfully register new user with BCrypt hashed password")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        String uniqueEmail = "testuser." + System.currentTimeMillis() + "@example.com";
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Sarah Connor")
                .email(uniqueEmail)
                .password("Cyberdyne@2026")
                .organizationName("Cyberdyne Systems")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.email", is(uniqueEmail.toLowerCase())))
                .andExpect(jsonPath("$.data.user.fullName", is("Sarah Connor")))
                .andReturn();

        // Verify BCrypt password hashing in database (never plain-text)
        Optional<User> saved = userRepository.findByEmailIgnoreCase(uniqueEmail);
        assertThat(saved).isPresent();
        assertThat(saved.get().getPasswordHash()).isNotEqualTo("Cyberdyne@2026");
        assertThat(passwordEncoder.matches("Cyberdyne@2026", saved.get().getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("POST /api/auth/register - Should reject duplicate email registration with 409 Conflict")
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        String duplicateEmail = "dup." + System.currentTimeMillis() + "@example.com";

        RegisterRequest first = RegisterRequest.builder()
                .fullName("First User")
                .email(duplicateEmail)
                .password("Secret@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = RegisterRequest.builder()
                .fullName("Second User")
                .email(duplicateEmail)
                .password("Secret@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should reject weak password with 400 Bad Request")
    void shouldRejectWeakPassword() throws Exception {
        RegisterRequest invalid = RegisterRequest.builder()
                .fullName("Weak User")
                .email("weak." + System.currentTimeMillis() + "@example.com")
                .password("weak") // Lacks uppercase, digits, and min 8 chars
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/auth/login - Valid credentials should return JWT and user profile")
    void shouldAuthenticateValidUser() throws Exception {
        String email = "login." + System.currentTimeMillis() + "@example.com";
        RegisterRequest register = RegisterRequest.builder()
                .fullName("Login User")
                .email(email)
                .password("LoginPass@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .email(email)
                .password("LoginPass@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is(email.toLowerCase())));
    }

    @Test
    @DisplayName("POST /api/auth/login - Invalid credentials should return 401 Unauthorized")
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequest invalid = LoginRequest.builder()
                .email("admin@cloudcostai.local")
                .password("CompletelyWrongPassword@999")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password.")));
    }

    @Test
    @DisplayName("GET /api/auth/me - Access profile with valid JWT")
    void shouldReturnProfileWithValidJwt() throws Exception {
        String email = "me." + System.currentTimeMillis() + "@example.com";
        RegisterRequest register = RegisterRequest.builder()
                .fullName("Profile User")
                .email(email)
                .password("ProfilePass@123")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String token = root.path("data").path("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(email.toLowerCase())))
                .andExpect(jsonPath("$.data.fullName", is("Profile User")));
    }

    @Test
    @DisplayName("GET /api/auth/me - Missing token should return 401 Unauthorized")
    void shouldRejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("GET /api/auth/me - Invalid token should return 401 Unauthorized")
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-garbage-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Revokes token via denylist preventing subsequent access")
    void shouldLogoutAndRevokeToken() throws Exception {
        String email = "logout." + System.currentTimeMillis() + "@example.com";
        RegisterRequest register = RegisterRequest.builder()
                .fullName("Logout User")
                .email(email)
                .password("LogoutPass@123")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String token = root.path("data").path("token").asText();

        // Verify token works
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Perform logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify token is now revoked and rejected
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
