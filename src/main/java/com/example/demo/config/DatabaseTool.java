package com.example.demo.config;

import com.example.demo.dao.HealthDao;
import com.example.demo.security.PasswordHasher;

/** migrate | seed | check; credential lấy từ AppConfig, không từ command args. */
public final class DatabaseTool {
    private DatabaseTool() { }
    public static void main(String[] arguments) throws Exception {
        String command = arguments.length == 1 ? arguments[0] : "";
        if ("demo-hashes".equals(command)) {
            var hasher = new PasswordHasher();
            for (int i = 0; i < 5; i++) System.out.println(hasher.hash("C2cDemo!2026".toCharArray()));
            return;
        }
        if (!java.util.Set.of("migrate","seed","check").contains(command)) {
            throw new IllegalArgumentException("Cách dùng: DatabaseTool migrate | seed | check");
        }
        DatabaseConfig config;
        try {
            config = AppConfig.load().database().orElseThrow(() -> new ConfigurationException(
                    "Chưa có cấu hình DB. Chọn file bằng APP_CONFIG_FILE hoặc -Dc2c.config; "
                    + "file cần db.url, db.username, db.password. Hoặc đặt đủ DB_URL, DB_USERNAME, DB_PASSWORD."));
        } catch (ConfigurationException exception) {
            throw new IllegalStateException("Cấu hình MySQL: " + exception.getMessage());
        } catch (Exception exception) {
            // Chỉ chuyển tiếp thông báo do bộ kiểm tra cấu hình an toàn tạo ra.
            throw new IllegalStateException("Không thể đọc cấu hình MySQL. Kiểm tra c2c.config / APP_CONFIG_FILE theo README.");
        }
        try (Database database = new Database(config)) {
            if ("migrate".equals(command)) System.out.println("Migration đã thực hiện: " + new SchemaManager(database).migrate());
            else if ("seed".equals(command)) { new SchemaManager(database).seed(); System.out.println("Nạp seed hoàn tất; không ghi đè bản ghi đã có."); }
            else System.out.println("JDBI SELECT 1 = " + database.read(handle -> handle.attach(HealthDao.class).ping()));
        } catch (Exception exception) {
            // Không giữ cause chứa URL/credential/SQL trong log Maven.
            throw new IllegalStateException("Thao tác DB thất bại (" + exception.getClass().getSimpleName()
                    + "). Kiểm tra endpoint, quyền và trạng thái schema theo README.");
        } finally { JdbcLifecycle.shutdown(); }
    }
}
