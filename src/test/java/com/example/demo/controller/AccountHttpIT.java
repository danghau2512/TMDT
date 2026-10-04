package com.example.demo.controller;

import com.example.demo.config.*;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.RegisterForm;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/** Chỉ trỏ Tomcat loopback đã deploy WAR với cùng schema *_test của APP_CONFIG_FILE. */
class AccountHttpIT {
    private static Database database;
    private static String base;
    private final List<String> fixtures = new ArrayList<>();
    @BeforeAll static void open() throws Exception {
        base = System.getenv("C2C_HTTP_BASE");
        Assumptions.assumeTrue(base != null && !base.isBlank(), "Đặt C2C_HTTP_BASE khi đã deploy Tomcat test.");
        assertEquals("true", System.getenv("C2C_IT_ALLOWED"));
        URI uri = URI.create(base);
        assertTrue("http".equals(uri.getScheme()) && Set.of("127.0.0.1", "localhost").contains(uri.getHost()));
        assertNull(uri.getUserInfo()); assertNull(uri.getQuery()); assertNull(uri.getFragment());
        database = new Database(AppConfig.load().database().orElseThrow());
        assertTrue(database.read(h -> h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));
        new SchemaManager(database).migrate(); new SchemaManager(database).seed();
        assertEquals(200, new Browser().get("/login").statusCode());
    }
    @AfterAll static void close() { if (database != null) database.close(); JdbcLifecycle.shutdown(); }
    @AfterEach void clean() {
        for (String email : fixtures) database.transaction(h -> { h.createUpdate("DELETE FROM users WHERE email=:email").bind("email", email).execute(); return null; });
    }
    private String freshEmail() { String email = UUID.randomUUID() + "@http-test.example"; fixtures.add(email); return email; }
    private static Map<String, String> registration(String email) {
        return Map.of("displayName", "Người dùng thử", "email", email, "phone", "0901234567", "password", "Account!2026", "confirmation", "Account!2026", "role", "ADMIN", "status", "INACTIVE");
    }
    private void create(String email) { new UserService(database).register(new RegisterForm("Người dùng thử", email, "", "Account!2026".toCharArray(), "Account!2026".toCharArray())); }
    private static String token(String html) {
        var matcher = Pattern.compile("name=\"csrfToken\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Trang phải có CSRF token.");
        return matcher.group(1);
    }
    private static void redirect(HttpResponse<String> response, String suffix) {
        assertEquals(303, response.statusCode());
        assertTrue(response.headers().firstValue("Location").orElse("").endsWith(suffix));
    }
    private static final class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();
        HttpResponse<String> get(String path) throws Exception { return send(HttpRequest.newBuilder(URI.create(base + path)).GET().build()); }
        HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
            String body = fields.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue())).collect(Collectors.joining("&"));
            return send(HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString(body)).build());
        }
        HttpResponse<String> form(String path, Map<String, String> fields) throws Exception {
            var withToken = new HashMap<>(fields); withToken.put("csrfToken", token(get(path).body())); return post(path, withToken);
        }
        HttpResponse<String> send(HttpRequest request) throws Exception { return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); }
        String sessionId() { return cookies.getCookieStore().getCookies().stream().filter(cookie -> "JSESSIONID".equals(cookie.getName())).findFirst().orElseThrow().getValue(); }
        private static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8); }
    }
    @Test void publicPagesVietnameseCookieAndAnonymousProtectionWork() throws Exception {
        var browser = new Browser();
        var login = browser.get("/login");
        assertEquals(200, login.statusCode()); assertTrue(login.body().contains("Đăng nhập"));
        assertTrue(login.headers().allValues("Set-Cookie").stream().anyMatch(value -> value.contains("HttpOnly") && value.contains("SameSite=Lax")));
        assertEquals(200, browser.get("/register").statusCode());
        assertEquals(200, browser.get("/").statusCode());
        redirect(browser.get("/account/profile"), "/login?notice=required");
        redirect(browser.get("/admin"), "/login?notice=required");
        redirect(browser.post("/admin", Map.of()), "/login?notice=required");
        assertEquals(404, browser.get("/WEB-INF/views/auth/login.jsp").statusCode());
    }
    @Test void registrationIgnoresInjectedRoleAndDuplicateInvalidFormsKeepOnlySafeValues() throws Exception {
        String email = freshEmail(); var browser = new Browser();
        redirect(browser.form("/register", registration(email)), "/login?notice=registered");
        var account = database.read(h -> h.attach(UserDao.class).findByEmail(email)).orElseThrow();
        assertEquals("USER", account.role()); assertEquals("ACTIVE", account.status());
        var duplicate = browser.form("/register", registration(email.toUpperCase(Locale.ROOT)));
        assertEquals(400, duplicate.statusCode()); assertTrue(duplicate.body().contains("đã được đăng ký"));
        assertFalse(duplicate.body().contains("value=\"Account!2026\""));
        var invalid = new HashMap<>(registration(freshEmail())); invalid.put("displayName", "<script>alert(1)</script>"); invalid.put("email", "invalid"); invalid.put("confirmation", "mismatch"); invalid.put("phone", "abc");
        var result = browser.form("/register", invalid);
        assertEquals(400, result.statusCode()); assertTrue(result.body().contains("không khớp"));
        assertFalse(result.body().contains("<script>alert(1)</script>")); assertTrue(result.body().contains("&lt;script&gt;"));
    }
    @Test void loginChangesSessionIdRejectsWrongUnknownInactiveAndRoutesSeedAccounts() throws Exception {
        String email = freshEmail(); create(email); var browser = new Browser();
        var first = browser.get("/login"); String before = browser.sessionId();
        redirect(browser.post("/login", Map.of("csrfToken", token(first.body()), "email", email, "password", "Account!2026")), "/home");
        assertTrue(!before.equals(browser.sessionId()), "Đăng nhập phải thay mã phiên; không xuất mã vào log.");
        var attacker = HttpClient.newHttpClient();
        var oldSession = attacker.send(HttpRequest.newBuilder(URI.create(base + "/account/profile")).header("Cookie", "JSESSIONID=" + before).build(), HttpResponse.BodyHandlers.ofString());
        redirect(oldSession, "/login?notice=required");
        assertTrue(browser.get("/home").body().contains("Người dùng thử"));
        var failed = new Browser();
        var wrong = failed.form("/login", Map.of("email", email, "password", "wrong"));
        var unknown = failed.form("/login", Map.of("email", freshEmail(), "password", "Account!2026"));
        assertEquals(400, wrong.statusCode()); assertEquals(400, unknown.statusCode());
        assertTrue(wrong.body().contains("Email hoặc mật khẩu không đúng")); assertTrue(unknown.body().contains("Email hoặc mật khẩu không đúng"));
        redirect(failed.get("/account/profile"), "/login?notice=required");
        database.transaction(h -> { h.createUpdate("UPDATE users SET status='INACTIVE' WHERE email=:email").bind("email", email).execute(); return null; });
        assertEquals(400, failed.form("/login", Map.of("email", email, "password", "Account!2026")).statusCode());
        redirect(browser.get("/account/profile"), "/login?notice=required");
        var admin = new Browser(); redirect(admin.form("/login", Map.of("email", "admin@c2c.example", "password", "C2cDemo!2026")), "/admin");
        assertEquals(200, admin.get("/admin").statusCode());
        var user = new Browser(); redirect(user.form("/login", Map.of("email", "buyer1@c2c.example", "password", "C2cDemo!2026")), "/home");
    }
    @Test void adminPathsRejectUserForGetAndPostAndRefreshDatabaseRole() throws Exception {
        String email = freshEmail(); create(email); var browser = new Browser();
        redirect(browser.form("/login", Map.of("email", email, "password", "Account!2026")), "/home");
        assertEquals(403, browser.get("/admin").statusCode());
        assertEquals(403, browser.get("/admin/not-implemented").statusCode());
        assertEquals(403, browser.post("/admin", Map.of("role", "ADMIN")).statusCode());
        database.transaction(h -> { h.createUpdate("UPDATE users SET role='ADMIN' WHERE email=:email").bind("email", email).execute(); return null; });
        assertEquals(200, browser.get("/admin").statusCode());
        database.transaction(h -> { h.createUpdate("UPDATE users SET role='USER' WHERE email=:email").bind("email", email).execute(); return null; });
        assertEquals(403, browser.get("/admin").statusCode());
    }
    @Test void profileIgnoresOtherUserIdEmailRoleStatusEscapesXssAndRejectsBadData() throws Exception {
        String email = freshEmail(); create(email); String otherEmail = freshEmail(); create(otherEmail);
        var other = database.read(h -> h.attach(UserDao.class).findByEmail(otherEmail)).orElseThrow();
        var browser = new Browser(); redirect(browser.form("/login", Map.of("email", email, "password", "Account!2026")), "/home");
        var fields = new HashMap<String, String>(); fields.put("displayName", "Tên mới <script>alert(1)</script>"); fields.put("phone", "0909876543"); fields.put("publicContact", "<img src=x onerror=alert(1)>");
        fields.put("userId", Long.toString(other.id())); fields.put("id", Long.toString(other.id())); fields.put("email", otherEmail); fields.put("role", "ADMIN"); fields.put("status", "INACTIVE");
        redirect(browser.form("/account/profile", fields), "/account/profile?notice=saved");
        var profile = browser.get("/account/profile"); assertEquals(200, profile.statusCode());
        assertTrue(profile.body().contains("&lt;script&gt;")); assertTrue(profile.body().contains("&lt;img"));
        assertFalse(profile.body().contains("<script>alert(1)</script>")); assertFalse(profile.body().contains("$2b$"));
        assertTrue(browser.get("/home").body().contains("Tên mới &lt;script&gt;"));
        var saved = database.read(h -> h.attach(UserDao.class).findByEmail(email)).orElseThrow();
        assertEquals("USER", saved.role()); assertEquals("ACTIVE", saved.status());
        assertEquals("Người dùng thử", database.read(h -> h.attach(UserDao.class).findByEmail(otherEmail)).orElseThrow().displayName());
        var invalid = browser.form("/account/profile", Map.of("displayName", "", "phone", "bad", "publicContact", "x".repeat(256)));
        assertEquals(400, invalid.statusCode());
        assertEquals(saved.displayName(), database.read(h -> h.attach(UserDao.class).findByEmail(email)).orElseThrow().displayName());
    }
    @Test void csrfMissingIncorrectAndPreLoginTokenAreRejectedAndLogoutInvalidatesSession() throws Exception {
        String email = freshEmail(); var browser = new Browser();
        assertEquals(403, browser.post("/register", registration(email)).statusCode());
        assertTrue(database.read(h -> h.attach(UserDao.class).findByEmail(email)).isEmpty());
        assertEquals(403, browser.post("/login", Map.of("email", "buyer1@c2c.example", "password", "C2cDemo!2026", "csrfToken", "x".repeat(43))).statusCode());
        String previousToken = token(browser.get("/login").body());
        redirect(browser.post("/login", Map.of("email", "buyer1@c2c.example", "password", "C2cDemo!2026", "csrfToken", previousToken)), "/home");
        assertEquals(403, browser.post("/account/profile", Map.of("displayName", "Không được ghi", "csrfToken", previousToken)).statusCode());
        assertEquals(403, browser.post("/account/profile", Map.of("displayName", "Không được ghi")).statusCode());
        assertEquals(403, browser.post("/logout", Map.of()).statusCode());
        assertEquals(403, browser.post("/logout", Map.of("csrfToken", "x".repeat(43))).statusCode());
        assertEquals(405, browser.get("/logout").statusCode()); assertEquals(200, browser.get("/account/profile").statusCode());
        String loggedInSession = browser.sessionId(); String csrf = token(browser.get("/account/profile").body());
        redirect(browser.post("/logout", Map.of("csrfToken", csrf)), "/home?notice=logged-out");
        redirect(browser.get("/account/profile"), "/login?notice=required");
        var replay = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/account/profile")).header("Cookie", "JSESSIONID=" + loggedInSession).build(), HttpResponse.BodyHandlers.ofString());
        redirect(replay, "/login?notice=required");
    }
}
