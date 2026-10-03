package com.example.demo.config;

/** toString được che để không ghi credential vào log. */
public record DatabaseConfig(String url, String username, String password, int poolSize, long timeoutMs) {
    public DatabaseConfig {
        if (url == null || !url.startsWith("jdbc:mysql://")
                || url.matches("(?is).*[?&](user|password)=.*")) {
            throw new ConfigurationException("DB_URL / db.url bắt buộc, bắt đầu bằng jdbc:mysql:// và không chứa tham số user/password.");
        }
        if (username == null || username.isBlank()) throw new ConfigurationException("DB_USERNAME / db.username bắt buộc và không được rỗng hoặc chỉ có khoảng trắng.");
        if (password == null || "CHANGE_ME_LOCALLY".equals(password)) throw new ConfigurationException("DB_PASSWORD / db.password chưa được thiết lập: thiếu khóa hoặc còn giá trị mẫu. Điền mật khẩu MySQL cục bộ.");
        if (poolSize < 1 || poolSize > 20) throw new ConfigurationException("DB_POOL_MAXIMUM_SIZE / db.pool.maximumSize phải từ 1 đến 20.");
        if (timeoutMs < 1000 || timeoutMs > 30000) throw new ConfigurationException("DB_CONNECTION_TIMEOUT_MS / db.pool.connectionTimeoutMs phải từ 1000 đến 30000 ms.");
    }
    @Override public String toString() { return "DatabaseConfig[redacted]"; }
}
