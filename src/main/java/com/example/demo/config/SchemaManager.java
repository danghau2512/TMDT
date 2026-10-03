package com.example.demo.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.flywaydb.core.Flyway;

/** Chỉ gọi từ CLI/test; không chạy trong lifecycle ứng dụng web. */
public final class SchemaManager {
    private final Database database;
    public SchemaManager(Database database) { this.database = database; }
    public int migrate() {
        return Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/migration").baselineOnMigrate(false).cleanDisabled(true)
                .validateMigrationNaming(true).load().migrate().migrationsExecuted;
    }
    public void seed() {
        String sql = resource("db/seed.sql");
        // Named lock serialize hai lần seed; toàn bộ DML rollback nếu gặp lỗi.
        database.read(handle -> {
            if (handle.createQuery("SELECT GET_LOCK('c2c_demo_seed', 10)").mapTo(Integer.class).one() != 1) {
                throw new IllegalStateException("Không lấy được khóa seed.");
            }
            try {
                handle.useTransaction(transaction -> {
                    // Không gắn nhật ký seed vào sản phẩm khác chiếm ID dành cho demo.
                    int conflicts = transaction.createQuery("""
                            SELECT COUNT(*) FROM products p JOIN users u ON u.id=p.seller_id
                            WHERE (p.id IN (520001,520002) AND u.email <> :firstSeller)
                               OR (p.id IN (520003,520004) AND u.email <> :secondSeller)
                            """)
                            .bind("firstSeller", "seller1@c2c.example")
                            .bind("secondSeller", "seller2@c2c.example").mapTo(Integer.class).one();
                    if (conflicts > 0) throw new IllegalStateException("ID demo đã bị dữ liệu khác chiếm.");
                    transaction.createScript(sql).execute();
                });
            }
            finally { handle.createQuery("SELECT RELEASE_LOCK('c2c_demo_seed')").mapTo(Integer.class).one(); }
            return null;
        });
    }
    private static String resource(String path) {
        try (InputStream input = SchemaManager.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("Thiếu script trong classpath.");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) { throw new IllegalStateException("Không đọc được script."); }
    }
}
