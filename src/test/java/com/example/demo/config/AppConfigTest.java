package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {
    @TempDir Path directory;
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
    @Test void jvmSelectorOverridesEnvironmentAndRelativePathsAreSupported() throws Exception {
        Path file = directory.resolve("local.properties");
        Files.writeString(file, "db.url=jdbc:mysql://localhost/c2c_demo\ndb.username=test\ndb.password=test\n");
        Path relative = Path.of("").toAbsolutePath().relativize(file);
        var config = AppConfig.load(relative.toString(), Map.of("APP_CONFIG_FILE", directory.resolve("missing.properties").toString()));
        assertEquals("test", config.database().orElseThrow().username());
        assertTrue(AppConfig.load(null, Map.of("APP_CONFIG_FILE", file.toString())).database().isPresent());
    }
    @Test void missingSelectedFileIdentifiesResolvedPathWithoutCause() {
        Path missing = directory.resolve("missing.properties");
        var error = assertThrows(ConfigurationException.class, () -> AppConfig.load(null, Map.of("APP_CONFIG_FILE", missing.toString())));
        assertTrue(error.getMessage().contains(missing.toString()));
        assertNull(error.getCause());
    }
    @Test void blankSelectorAndMalformedPropertiesHaveSafeErrors() throws Exception {
        assertThrows(ConfigurationException.class, () -> AppConfig.load(" ", Map.of()));
        Path file = directory.resolve("malformed.properties");
        String secret = "private-value";
        Files.writeString(file, "db.password=" + secret + "\\uXXXX\n");
        var error = assertThrows(ConfigurationException.class, () -> AppConfig.load(file.toString(), Map.of()));
        assertTrue(error.getMessage().contains(file.toString()));
        assertFalse(error.getMessage().contains(secret));
        assertNull(error.getCause());
    }
    @Test void invalidNumericValuesIdentifyKeysWithoutEchoingValues() {
        Map<String, String> required = Map.of("DB_URL", "jdbc:mysql://localhost/demo", "DB_USERNAME", "test", "DB_PASSWORD", "private-value");
        for (String key : new String[]{"DB_POOL_MAXIMUM_SIZE", "DB_CONNECTION_TIMEOUT_MS"}) {
            var environment = new java.util.HashMap<>(required);
            environment.put(key, "private-value");
            var error = assertThrows(ConfigurationException.class, () -> AppConfig.from(new Properties(), environment));
            assertTrue(error.getMessage().contains(key));
            assertFalse(error.getMessage().contains("private-value"));
            assertNull(error.getCause());
        }
    }
    @Test void passwordPlaceholderAndEmptyUsernameAreRejectedButExplicitEmptyPasswordIsSupported() {
        var error = assertThrows(ConfigurationException.class, () -> new DatabaseConfig("jdbc:mysql://localhost/demo", "test", "CHANGE_ME_LOCALLY", 5, 5000));
        assertTrue(error.getMessage().contains("db.password"));
        assertFalse(error.getMessage().contains("CHANGE_ME_LOCALLY"));
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:mysql://localhost/demo");
        properties.setProperty("db.username", "file_user");
        properties.setProperty("db.password", "");
        assertEquals("", AppConfig.from(properties, Map.of()).database().orElseThrow().password());
        error = assertThrows(ConfigurationException.class, () -> AppConfig.from(properties, Map.of("DB_USERNAME", "")));
        assertTrue(error.getMessage().contains("DB_USERNAME"));
    }
    @Test void rangeAndBooleanFailuresIdentifyTheirOwnFields() {
        assertTrue(assertThrows(ConfigurationException.class, () -> new DatabaseConfig("jdbc:mysql://localhost/demo", "test", "test", 0, 5000)).getMessage().contains("db.pool.maximumSize"));
        assertTrue(assertThrows(ConfigurationException.class, () -> new DatabaseConfig("jdbc:mysql://localhost/demo", "test", "test", 5, 999)).getMessage().contains("db.pool.connectionTimeoutMs"));
        var error = assertThrows(ConfigurationException.class, () -> AppConfig.from(new Properties(), Map.of("APP_DIAGNOSTICS_ENABLED", "private-value")));
        assertTrue(error.getMessage().contains("app.diagnostics.enabled"));
        assertFalse(error.getMessage().contains("private-value"));
    }
}
