package com.cloudcostai.service;

import com.cloudcostai.entity.CloudAccount;
import com.cloudcostai.entity.CloudProvider;
import com.cloudcostai.entity.Organization;
import com.cloudcostai.entity.User;
import com.cloudcostai.entity.UserRole;
import com.cloudcostai.repository.CloudAccountRepository;
import com.cloudcostai.repository.OrganizationRepository;
import com.cloudcostai.repository.UserRepository;
import com.cloudcostai.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class TenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudAccountRepository cloudAccountRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String tokenTenantA;
    private String tokenTenantB;
    private CloudAccount accountA;
    private CloudAccount accountB;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        Organization orgA = organizationRepository.save(Organization.builder()
                .name("Tenant Alpha " + ts)
                .slug("tenant-alpha-" + ts)
                .tier("STANDARD")
                .isActive(true)
                .build());

        Organization orgB = organizationRepository.save(Organization.builder()
                .name("Tenant Beta " + ts)
                .slug("tenant-beta-" + ts)
                .tier("STANDARD")
                .isActive(true)
                .build());

        User userA = userRepository.save(User.builder()
                .firstName("User")
                .lastName("Alpha")
                .email("alpha." + ts + "@tenant-a.com")
                .passwordHash("hashed")
                .role(UserRole.ROLE_USER.name())
                .organization(orgA)
                .isActive(true)
                .build());

        User userB = userRepository.save(User.builder()
                .firstName("User")
                .lastName("Beta")
                .email("beta." + ts + "@tenant-b.com")
                .passwordHash("hashed")
                .role(UserRole.ROLE_USER.name())
                .organization(orgB)
                .isActive(true)
                .build());

        accountA = cloudAccountRepository.save(CloudAccount.builder()
                .organization(orgA)
                .provider(CloudProvider.AWS)
                .accountId("111122223333-" + ts)
                .accountName("Alpha AWS Prod")
                .syncStatus("ACTIVE")
                .isActive(true)
                .build());

        accountB = cloudAccountRepository.save(CloudAccount.builder()
                .organization(orgB)
                .provider(CloudProvider.AWS)
                .accountId("444455556666-" + ts)
                .accountName("Beta AWS Prod")
                .syncStatus("ACTIVE")
                .isActive(true)
                .build());

        tokenTenantA = jwtTokenProvider.generateTokenFromUser(userA);
        tokenTenantB = jwtTokenProvider.generateTokenFromUser(userB);
    }

    @Test
    @DisplayName("Tenant A should only view its own cloud accounts and never see Tenant B's data")
    void shouldIsolateCloudAccountsBetweenTenants() throws Exception {
        // Tenant A requests accounts
        mockMvc.perform(get("/api/cloud-accounts")
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].accountName", hasItem("Alpha AWS Prod")))
                .andExpect(jsonPath("$.data[*].accountName", not(hasItem("Beta AWS Prod"))));

        // Tenant B requests accounts
        mockMvc.perform(get("/api/cloud-accounts")
                        .header("Authorization", "Bearer " + tokenTenantB)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].accountName", hasItem("Beta AWS Prod")))
                .andExpect(jsonPath("$.data[*].accountName", not(hasItem("Alpha AWS Prod"))));
    }

    @Test
    @DisplayName("IDOR Prevention: Tenant A cannot read Tenant B's cloud account by ID")
    void shouldPreventCrossTenantDirectObjectRead() throws Exception {
        mockMvc.perform(get("/api/cloud-accounts/" + accountB.getId())
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("CloudAccount not found")));
    }

    @Test
    @DisplayName("IDOR Prevention: Tenant A cannot delete Tenant B's cloud account by ID")
    void shouldPreventCrossTenantDirectObjectDelete() throws Exception {
        mockMvc.perform(delete("/api/cloud-accounts/" + accountB.getId())
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Verify account B still exists in repository
        assertThat(cloudAccountRepository.findById(accountB.getId())).isPresent();
    }
}
