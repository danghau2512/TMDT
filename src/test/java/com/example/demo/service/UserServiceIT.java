package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.*;
import com.example.demo.exception.FormException;
import com.example.demo.model.CurrentUser;
import com.example.demo.security.PasswordHasher;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceIT {
    private static Database database;
    private static UserService service;
    private final List<String> emails = new ArrayList<>();
    @BeforeAll static void open() throws Exception {
        assertEquals("true", System.getenv("C2C_IT_ALLOWED"));
        database = new Database(AppConfig.load().database().orElseThrow());
        assertTrue(database.read(h -> h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));
        new SchemaManager(database).migrate();
        new SchemaManager(database).seed();
        service = new UserService(database);
    }
    @AfterAll static void close() { if (database != null) database.close(); JdbcLifecycle.shutdown(); }
    @AfterEach void removeOwnFixtures() {
        for (String email : emails) database.transaction(h -> { h.createUpdate("DELETE FROM users WHERE email=:email").bind("email", email).execute(); return null; });
    }
    private String freshEmail() { String email = UUID.randomUUID() + "@service-test.example"; emails.add(email); return email; }
    private static RegisterForm form(String email) { return new RegisterForm("Nguyễn An", email, "0901234567", "Account!2026".toCharArray(), "Account!2026".toCharArray()); }
    private CurrentUser register(String email) { service.register(form(email)); return service.authenticate(email, "Account!2026".toCharArray()).orElseThrow(); }

    @Test void registrationNormalizesEmailAndStoresBcryptWithUserRole() {
        String email = freshEmail();
        service.register(form("  " + email.toUpperCase(Locale.ROOT) + "  "));
        var account = database.read(h -> h.attach(UserDao.class).findByEmail(email)).orElseThrow();
        assertEquals("USER", account.role()); assertEquals("ACTIVE", account.status());
        assertNotEquals("Account!2026", account.passwordHash());
        assertTrue(account.passwordHash().startsWith("$2b$12$"));
        assertTrue(new PasswordHasher().verify("Account!2026".toCharArray(), account.passwordHash()));
        assertFalse(account.toString().contains(account.passwordHash()));
        assertEquals(0, database.dataSource().getHikariPoolMXBean().getActiveConnections());
    }
    @Test void duplicateEmailIsRejectedByServiceAndUniqueConstraint() {
        String email = freshEmail(); register(email);
        assertTrue(assertThrows(FormException.class, () -> service.register(form(email.toUpperCase(Locale.ROOT)))).errors().containsKey("email"));
        assertThrows(RuntimeException.class, () -> database.transaction(h -> { h.attach(UserDao.class).insert(email, "unused", "Trùng email", null); return null; }));
    }
    @Test void concurrentRegistrationCreatesExactlyOneAccountAndReportsConflict() throws Exception {
        String email = freshEmail();
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> { start.await(); try { service.register(form(email)); return true; } catch (FormException exception) { assertTrue(exception.errors().containsKey("email")); return false; } };
        try {
            var first = pool.submit(attempt); var second = pool.submit(attempt); start.countDown();
            int successes = (first.get(15, TimeUnit.SECONDS) ? 1 : 0) + (second.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, (int) database.read(h -> h.createQuery("SELECT COUNT(*) FROM users WHERE email=:email").bind("email", email).mapTo(Integer.class).one()));
        } finally { pool.shutdownNow(); }
    }
    @Test void validationFailureDoesNotWriteAndLoginInputIsBoundAsData() {
        String email = freshEmail();
        assertThrows(FormException.class, () -> service.register(new RegisterForm("", email, "abc", "short".toCharArray(), new char[0])));
        assertTrue(database.read(h -> h.attach(UserDao.class).findByEmail(email)).isEmpty());
        assertTrue(service.authenticate("' OR 1=1 --@example.com", "Account!2026".toCharArray()).isEmpty());
        assertTrue(service.authenticate("admin@c2c.example", "wrong".toCharArray()).isEmpty());
        assertTrue(service.authenticate(freshEmail(), "Account!2026".toCharArray()).isEmpty());
    }
    @Test void seedAccountsAuthenticateAndInactiveAccountsCannotAuthenticate() {
        assertTrue(service.authenticate("admin@c2c.example", "C2cDemo!2026".toCharArray()).orElseThrow().isAdmin());
        assertFalse(service.authenticate("buyer1@c2c.example", "C2cDemo!2026".toCharArray()).orElseThrow().isAdmin());
        String email = freshEmail(); var user = register(email);
        database.transaction(h -> { h.createUpdate("UPDATE users SET status='INACTIVE' WHERE id=:id").bind("id", user.id()).execute(); return null; });
        assertTrue(service.authenticate(email, "Account!2026".toCharArray()).isEmpty());
        assertTrue(service.currentActive(user.id()).isEmpty());
        assertThrows(FormException.class, () -> service.updateProfile(user, new ProfileForm("Tên mới", "", "")));
    }
    @Test void profileUpdateUsesActorAndPreservesEmailHashRoleStatusAndOtherAccounts() {
        String email = freshEmail(); var user = register(email);
        var other = register(freshEmail());
        var before = database.read(h -> h.attach(UserDao.class).findByEmail(email)).orElseThrow();
        var updated = service.updateProfile(user, new ProfileForm(" Tên mới <script> ", "+84912345678", "Liên hệ công khai"));
        assertEquals("Tên mới <script>", updated.displayName());
        var after = database.read(h -> h.attach(UserDao.class).findByEmail(email)).orElseThrow();
        assertEquals(before.email(), after.email()); assertEquals(before.passwordHash(), after.passwordHash());
        assertEquals(before.role(), after.role()); assertEquals(before.status(), after.status());
        assertEquals("Nguyễn An", service.profile(other).displayName());
        assertThrows(FormException.class, () -> service.updateProfile(user, new ProfileForm("", "bad", "")));
        assertEquals("Tên mới <script>", service.profile(user).displayName());
        assertThrows(FormException.class, () -> service.profile(null));
    }
    @Test void refreshedCurrentUserUsesDatabaseRoleRatherThanStaleSessionRole() {
        var user = register(freshEmail());
        database.transaction(h -> { h.createUpdate("UPDATE users SET role='ADMIN' WHERE id=:id").bind("id", user.id()).execute(); return null; });
        assertTrue(service.currentActive(user.id()).orElseThrow().isAdmin());
        database.transaction(h -> { h.createUpdate("UPDATE users SET role='USER' WHERE id=:id").bind("id", user.id()).execute(); return null; });
        assertFalse(service.currentActive(user.id()).orElseThrow().isAdmin());
    }
}
