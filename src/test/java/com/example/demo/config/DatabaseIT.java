package com.example.demo.config;

import com.example.demo.dao.HealthDao;
import com.example.demo.security.PasswordHasher;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Chỉ chạy -Pmysql-it trên schema *_test đã được người chạy cho phép. */
public class DatabaseIT {
    private static Database database;
    @BeforeAll static void open() throws Exception {
        assertEquals("true", System.getenv("C2C_IT_ALLOWED"), "Cần C2C_IT_ALLOWED=true và schema test riêng.");
        database = new Database(AppConfig.load().database().orElseThrow());
        String schema = database.read(h -> h.createQuery("SELECT DATABASE()").mapTo(String.class).one());
        assertTrue(schema.endsWith("_test"), "Cấm chạy integration test trên schema không có hậu tố _test.");
        new SchemaManager(database).migrate();
        new SchemaManager(database).seed();
    }
    @AfterAll static void close() { if (database != null) database.close(); JdbcLifecycle.shutdown(); }

    @Test void schemaHasAllCurrentInnoDbTablesAndForeignKeys() throws Exception {
        Set<String> expected = new HashSet<>();
        var matcher = Pattern.compile("CREATE TABLE (\\w+)").matcher(Files.readString(Path.of("src/main/resources/db/schema.sql")));
        while (matcher.find()) expected.add(matcher.group(1));
        Set<String> actual = database.read(h -> new HashSet<>(h.createQuery("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name <> 'flyway_schema_history'").mapTo(String.class).list()));
        assertEquals(22, expected.size());
        try(var migrations=Files.list(Path.of("src/main/resources/db/migration"))) {
            for(var migration:migrations.filter(p->!p.getFileName().toString().startsWith("V001") && p.toString().endsWith(".sql")).toList()) {
                var additions=Pattern.compile("CREATE TABLE (\\w+)").matcher(Files.readString(migration));while(additions.find())expected.add(additions.group(1));
            }
        }
        assertEquals(expected, actual);
        assertEquals(expected.size(), (int) database.read(h -> h.createQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND engine='InnoDB' AND table_name <> 'flyway_schema_history'").mapTo(Integer.class).one()));
        assertTrue(database.read(h -> h.createQuery("SELECT COUNT(*) FROM information_schema.referential_constraints WHERE constraint_schema=DATABASE()").mapTo(Integer.class).one()) >= 40);
    }
    @Test void migrationRerunDoesNotRunV001Again() { assertEquals(0, new SchemaManager(database).migrate()); }
    @Test void jdbiPingAndBindTreatInjectionTextAsData() {
        String input = "Tiếng Việt '; DROP TABLE users; --";
        database.read(h -> { var dao = h.attach(HealthDao.class); assertEquals(1,dao.ping()); assertEquals(input,dao.echo(input)); return null; });
    }
    @Test void handleIsClosedAfterCallback() {
        AtomicReference<Handle> reference = new AtomicReference<>();
        database.read(h -> { reference.set(h); return h.attach(HealthDao.class).ping(); });
        assertTrue(reference.get().isClosed());
    }
    public interface WriteFixtureDao {
        @SqlUpdate("INSERT INTO categories(name,slug) VALUES (:name,:slug)")
        void insert(@Bind("name") String name, @Bind("slug") String slug);
    }
    public interface ReadFixtureDao {
        @SqlQuery("SELECT COUNT(*) FROM categories WHERE slug=:slug") int count(@Bind("slug") String slug);
    }
    @Test void serviceTransactionRollsBackWritesFromMultipleAttachedDaos() {
        String slug = "rollback-" + UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> database.transaction(h -> {
            h.attach(WriteFixtureDao.class).insert("Kiểm tra rollback",slug);
            assertEquals(1,h.attach(ReadFixtureDao.class).count(slug));
            throw new IllegalStateException("Rollback intentionally");
        }));
        assertEquals(0, (int) database.read(h -> h.attach(ReadFixtureDao.class).count(slug)));
    }
    @Test void serviceTransactionCommitsAndPoolReusesConnections() {
        String slug = "commit-" + UUID.randomUUID();
        try {
            database.transaction(h -> { h.attach(WriteFixtureDao.class).insert("Kiểm tra commit",slug); return null; });
            assertEquals(1, (int) database.read(h -> h.attach(ReadFixtureDao.class).count(slug)));
            for (int i=0;i<20;i++) assertEquals(1, (int) database.read(h -> h.attach(HealthDao.class).ping()));
            assertEquals(0,database.dataSource().getHikariPoolMXBean().getActiveConnections());
        } finally { database.transaction(h -> { h.createUpdate("DELETE FROM categories WHERE slug=:slug").bind("slug",slug).execute(); return null; }); }
    }
    @Test void seedRerunPreservesExistingProfileAndProductStock() {
        int originalStock = database.read(h -> h.createQuery("SELECT stock_quantity FROM products WHERE id=520001").mapTo(Integer.class).one());
        String originalName = database.read(h -> h.createQuery("SELECT display_name FROM users WHERE id=500004").mapTo(String.class).one());
        try {
            database.transaction(h -> { h.createUpdate("UPDATE products SET stock_quantity=2 WHERE id=520001").execute(); h.createUpdate("UPDATE users SET display_name=:name WHERE id=500004").bind("name","Tên đã cập nhật").execute(); return null; });
            new SchemaManager(database).seed(); new SchemaManager(database).seed();
            assertEquals(5, (int) database.read(h -> h.createQuery("SELECT COUNT(*) FROM users").mapTo(Integer.class).one()));
            assertEquals(4, (int) database.read(h -> h.createQuery("SELECT COUNT(*) FROM products").mapTo(Integer.class).one()));
            assertEquals(4, (int) database.read(h -> h.createQuery("SELECT COUNT(*) FROM stock_movements").mapTo(Integer.class).one()));
            assertEquals(2, (int) database.read(h -> h.createQuery("SELECT stock_quantity FROM products WHERE id=520001").mapTo(Integer.class).one()));
            assertEquals("Tên đã cập nhật",database.read(h -> h.createQuery("SELECT display_name FROM users WHERE id=500004").mapTo(String.class).one()));
        } finally { database.transaction(h -> { h.createUpdate("UPDATE products SET stock_quantity=:q WHERE id=520001").bind("q",originalStock).execute(); h.createUpdate("UPDATE users SET display_name=:n WHERE id=500004").bind("n",originalName).execute(); return null; }); }
    }
    @Test void everyDemoPasswordMatchesSharedBcryptPolicy() {
        var hashes = database.read(h -> h.createQuery("SELECT password_hash FROM users ORDER BY id").mapTo(String.class).list());
        assertEquals(5,new HashSet<>(hashes).size());
        for (String hash : hashes) assertTrue(new PasswordHasher().verify("C2cDemo!2026".toCharArray(),hash));
    }
    @Test void checksRejectBadQuantityPriceStatusAndOrphanReferences() {
        assertThrows(RuntimeException.class, () -> database.transaction(h -> { h.createUpdate("UPDATE products SET price=-1 WHERE id=520001").execute(); return null; }));
        assertThrows(RuntimeException.class, () -> database.transaction(h -> { h.createUpdate("UPDATE products SET condition_code='INVALID' WHERE id=520001").execute(); return null; }));
        assertThrows(RuntimeException.class, () -> database.transaction(h -> { h.createUpdate("INSERT INTO cart_items(cart_id,product_id,quantity) VALUES(9999999,520001,0)").execute(); return null; }));
        assertThrows(RuntimeException.class, () -> database.transaction(h -> { h.createUpdate("INSERT INTO cart_items(cart_id,product_id,quantity) VALUES(9999999,520001,1)").execute(); return null; }));
    }

    private static long[] orderFixture(Handle h) {
        long batch = h.createUpdate("INSERT INTO checkout_batches(batch_code,buyer_id,idempotency_key,request_hash) VALUES(:code,500004,:key,:hash)")
                .bind("code",UUID.randomUUID().toString()).bind("key",UUID.randomUUID().toString()).bind("hash","0".repeat(64)).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();
        long order = h.createUpdate("INSERT INTO orders(order_code,checkout_batch_id,buyer_id,seller_id,recipient_name,recipient_phone,shipping_address,buyer_name_snapshot,seller_name_snapshot,subtotal,grand_total) VALUES(:code,:batch,500004,500002,'Người nhận thử','0000000000','Địa chỉ giả','Chi','An',200000,200000)")
                .bind("code",UUID.randomUUID().toString()).bind("batch",batch).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();
        long item = h.createUpdate("INSERT INTO order_items(order_id,product_id,product_listing_version,product_name_snapshot,description_snapshot,condition_snapshot,category_name_snapshot,unit_price,quantity,line_total) VALUES(:order,520001,1,'Tên lúc đặt','Mô tả lúc đặt','USED','Điện tử',200000,1,200000)")
                .bind("order",order).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();
        return new long[]{order,item};
    }
    @Test void snapshotSurvivesProductEditAndCompositeBuyerForeignKeyIsEnforced() {
        assertThrows(IllegalStateException.class, () -> database.transaction(h -> {
            long[] fixture = orderFixture(h);
            h.createUpdate("UPDATE products SET title='Tên mới',price=250000 WHERE id=520001").execute();
            assertEquals("Tên lúc đặt",h.createQuery("SELECT product_name_snapshot FROM order_items WHERE id=:id").bind("id",fixture[1]).mapTo(String.class).one());
            assertThrows(RuntimeException.class, () -> h.createUpdate("INSERT INTO reviews(order_item_id,order_id,buyer_id,product_rating,seller_rating,comment) VALUES(:item,:order,500005,5,5,'Sai người mua')").bind("item",fixture[1]).bind("order",fixture[0]).execute());
            throw new IllegalStateException("Rollback fixture");
        }));
    }
    @Test void holdAndCancelLedgerRejectDuplicateReleaseAndInvalidBalance() {
        assertThrows(IllegalStateException.class, () -> database.transaction(h -> {
            long[] fixture=orderFixture(h);
            h.createUpdate("INSERT INTO stock_movements(product_id,order_item_id,movement_type,quantity_delta,quantity_before,quantity_after,actor_id) VALUES(520001,:item,'ORDER_HOLD',-1,3,2,500004)").bind("item",fixture[1]).execute();
            h.createUpdate("INSERT INTO stock_movements(product_id,order_item_id,movement_type,quantity_delta,quantity_before,quantity_after,actor_id) VALUES(520001,:item,'CANCEL_RELEASE',1,2,3,500004)").bind("item",fixture[1]).execute();
            assertThrows(RuntimeException.class, () -> h.createUpdate("INSERT INTO stock_movements(product_id,order_item_id,movement_type,quantity_delta,quantity_before,quantity_after,actor_id) VALUES(520001,:item,'CANCEL_RELEASE',1,2,3,500004)").bind("item",fixture[1]).execute());
            assertThrows(RuntimeException.class, () -> h.createUpdate("INSERT INTO stock_movements(product_id,movement_type,quantity_delta,quantity_before,quantity_after,actor_id,reason) VALUES(520001,'ADJUSTMENT',1,3,9,500002,'Sai cân bằng')").execute());
            throw new IllegalStateException("Rollback fixture");
        }));
    }
}
