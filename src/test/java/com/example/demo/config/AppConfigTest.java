package com.example.demo.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {
    @Test void missingDatabaseAllowsStartupWithoutDiagnostics() {
        AppConfig config = AppConfig.from(new Properties(), Map.of());
        assertTrue(config.database().isEmpty());
        assertFalse(config.diagnosticsEnabled());
    }
    @Test void environmentOverridesLocalPropertiesWithoutLoggingSecrets() {
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:mysql://localhost/file_schema");
        properties.setProperty("db.username", "file_user");
        properties.setProperty("db.password", "file_secret");
        var config = AppConfig.from(properties, Map.of("DB_URL","jdbc:mysql://localhost/env_schema", "DB_USERNAME","env_user", "DB_PASSWORD","env_secret"));
        assertEquals("env_user", config.database().orElseThrow().username());
        assertEquals("env_secret", config.database().orElseThrow().password());
        assertFalse(config.toString().contains("env_secret"));
        assertFalse(config.toString().contains("env_schema"));
    }
    @Test void rejectsPartialConfigurationAndCredentialsInUrl() {
        assertThrows(IllegalArgumentException.class, () -> AppConfig.from(new Properties(), Map.of("DB_URL","jdbc:mysql://localhost/demo")));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseConfig("jdbc:mysql://localhost/demo?password=secret","test","test",5,5000));
        assertThrows(IllegalArgumentException.class, () -> new DatabaseConfig("jdbc:mariadb://localhost/demo","test","test",5,5000));
    }
}
