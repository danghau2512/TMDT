package com.example.demo.config;

import com.example.demo.service.HealthService;
import java.util.Optional;

public record ApplicationRuntime(boolean configurationValid, boolean diagnosticsEnabled,
                                 Optional<Database> database, HealthService healthService) {
    public static final String CONTEXT_KEY = ApplicationRuntime.class.getName();
}
