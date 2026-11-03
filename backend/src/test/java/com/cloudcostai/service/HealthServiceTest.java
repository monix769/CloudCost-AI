package com.cloudcostai.service;

import com.cloudcostai.dto.HealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private DatabaseHealthIndicatorService databaseHealthIndicatorService;

    @InjectMocks
    private HealthService healthService;

    @Test
    @DisplayName("HealthService should return UP when database is UP")
    void shouldReturnUpWhenDatabaseIsUp() {
        Map<String, Object> dbHealth = new HashMap<>();
        dbHealth.put("status", "UP");
        dbHealth.put("databaseProduct", "MySQL");
        dbHealth.put("databaseVersion", "8.0.35");

        when(databaseHealthIndicatorService.checkDatabaseHealth()).thenReturn(dbHealth);

        HealthResponse response = healthService.getHealth();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("UP");
        assertThat(response.getDatabase()).isEqualTo("CONNECTED");
        assertThat(response.getComponents()).containsKey("database");
    }

    @Test
    @DisplayName("HealthService should return DEGRADED when database is DOWN")
    void shouldReturnDegradedWhenDatabaseIsDown() {
        Map<String, Object> dbHealth = new HashMap<>();
        dbHealth.put("status", "DOWN");
        dbHealth.put("error", "Connection refused");

        when(databaseHealthIndicatorService.checkDatabaseHealth()).thenReturn(dbHealth);

        HealthResponse response = healthService.getHealth();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("DEGRADED");
        assertThat(response.getDatabase()).isEqualTo("DISCONNECTED");
    }
}
