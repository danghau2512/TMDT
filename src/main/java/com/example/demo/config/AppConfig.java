package com.example.demo.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public record AppConfig(Optional<DatabaseConfig> database, boolean diagnosticsEnabled, Path uploadRoot, Optional<VnpayConfig> vnpay) {
    public static AppConfig load() throws IOException {
        return load(System.getProperty("c2c.config"), System.getenv());
    }
    static AppConfig load(String jvmPath, Map<String, String> environment) {
        String path = jvmPath != null ? jvmPath : environment.get("APP_CONFIG_FILE");
        Properties properties = new Properties();
        if (path != null) {
            if (path.isBlank()) throw new ConfigurationException("Đường dẫn c2c.config / APP_CONFIG_FILE đang rỗng. Hãy chọn file cấu hình cục bộ.");
            Path file;
            try { file = Path.of(path).toAbsolutePath().normalize(); }
            catch (InvalidPathException exception) {
                throw new ConfigurationException("Đường dẫn c2c.config / APP_CONFIG_FILE không hợp lệ.");
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { properties.load(reader); }
            catch (IOException | SecurityException exception) {
                throw new ConfigurationException("Không đọc được file cấu hình: " + file + ". Kiểm tra file tồn tại, là file thường và có quyền đọc.");
            } catch (IllegalArgumentException exception) {
                throw new ConfigurationException("Sai cú pháp properties trong file cấu hình: " + file + ". Không hiển thị giá trị cấu hình.");
            }
        }
        return from(properties, environment);
    }
    public static AppConfig from(Properties properties, Map<String, String> environment) {
        String url = value(properties, environment, "DB_URL", "db.url", null);
        String username = value(properties, environment, "DB_USERNAME", "db.username", null);
        String password = value(properties, environment, "DB_PASSWORD", "db.password", null);
        String flag = value(properties, environment, "APP_DIAGNOSTICS_ENABLED", "app.diagnostics.enabled", "false");
        if (!"true".equalsIgnoreCase(flag) && !"false".equalsIgnoreCase(flag)) throw new ConfigurationException("APP_DIAGNOSTICS_ENABLED / app.diagnostics.enabled phải là true hoặc false.");
        Optional<DatabaseConfig> database = Optional.empty();
        // Không có config DB vẫn smoke test WAR được; config dở dang phải báo lỗi.
        if (url != null || username != null || password != null) {
            int size;
            long timeout;
            try { size = Integer.parseInt(value(properties, environment, "DB_POOL_MAXIMUM_SIZE", "db.pool.maximumSize", "5")); }
            catch (NumberFormatException exception) { throw new ConfigurationException("DB_POOL_MAXIMUM_SIZE / db.pool.maximumSize phải là số nguyên từ 1 đến 20."); }
            try { timeout = Long.parseLong(value(properties, environment, "DB_CONNECTION_TIMEOUT_MS", "db.pool.connectionTimeoutMs", "5000")); }
            catch (NumberFormatException exception) { throw new ConfigurationException("DB_CONNECTION_TIMEOUT_MS / db.pool.connectionTimeoutMs phải là số nguyên từ 1000 đến 30000 ms."); }
            database = Optional.of(new DatabaseConfig(url, username, password, size, timeout));
        }
        String storage = value(properties, environment, "UPLOAD_ROOT", "upload.root", "");
        Path uploadRoot = storage.isBlank() ? Path.of(System.getProperty("user.home"), ".c2c-demo", "uploads") : Path.of(storage);
        if (!uploadRoot.isAbsolute()) throw new ConfigurationException("UPLOAD_ROOT / upload.root phải là đường dẫn tuyệt đối ngoài WAR.");
        return new AppConfig(database, Boolean.parseBoolean(flag), uploadRoot.normalize(), VnpayConfig.from(properties,environment));
    }
    private static String value(Properties properties, Map<String, String> environment, String env, String key, String fallback) {
        return environment.containsKey(env) ? environment.get(env) : properties.getProperty(key, fallback);
    }
}
