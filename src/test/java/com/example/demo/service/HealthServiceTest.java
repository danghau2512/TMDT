package com.example.demo.service;

import com.example.demo.model.DatabaseHealth;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthServiceTest {
    @Test void missingConfigurationReportsNotConfiguredRatherThanSuccess() {
        assertEquals(DatabaseHealth.State.NOT_CONFIGURED,
                new HealthService(Optional.empty()).checkDatabase().state());
    }
}
