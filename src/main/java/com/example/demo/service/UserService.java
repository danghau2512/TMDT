package com.example.demo.service;

import com.example.demo.config.Database;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.*;
import com.example.demo.exception.*;
import com.example.demo.model.*;
import com.example.demo.security.PasswordHasher;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

public final class UserService {
    private final Database database;
    private final PasswordHasher hasher = new PasswordHasher();
    // Chạy BCrypt cả khi email không tồn tại; đây chỉ là hash mồi, không phải tài khoản.
    private final String dummyHash = hasher.hash("DummyAccount!2026".toCharArray());
    public UserService(Database database) { this.database = Objects.requireNonNull(database); }

    public void register(RegisterForm form) {
        AccountValidation.registration(form);
        String email = AccountValidation.email(form.email());
        String hash = hasher.hash(form.password());
        operation(() -> database.transaction(handle -> {
            var dao = handle.attach(UserDao.class);
            if (dao.findByEmail(email).isPresent()) throw duplicateEmail();
            dao.insert(email, hash, AccountValidation.text(form.displayName()), nullable(form.phone()));
            return null;
        }));
    }
    public Optional<CurrentUser> authenticate(String email, char[] password) {
        if (password == null || password.length == 0 || AccountValidation.utf8Bytes(password) > 72) return Optional.empty();
        String normalized = AccountValidation.email(email);
        if (!AccountValidation.validEmail(normalized)) return Optional.empty();
        return operation(() -> {
            var account = database.read(h -> h.attach(UserDao.class).findByEmail(normalized));
            boolean matches = hasher.verify(password, account.map(UserAccount::passwordHash).orElse(dummyHash));
            return account.filter(user -> matches && "ACTIVE".equals(user.status())).map(UserAccount::currentUser);
        });
    }
    public Optional<CurrentUser> currentActive(long id) { return operation(() -> database.read(h -> h.attach(UserDao.class).findActive(id))); }
    public UserProfile profile(CurrentUser actor) {
        requireActor(actor);
        return operation(() -> database.read(h -> h.attach(UserDao.class).profile(actor.id())
                .orElseThrow(() -> new FormException(Map.of("form", "Tài khoản không còn hoạt động. Vui lòng đăng nhập lại.")))));
    }
    public CurrentUser updateProfile(CurrentUser actor, ProfileForm form) {
        requireActor(actor);
        AccountValidation.profile(form);
        return operation(() -> database.transaction(handle -> {
            var dao = handle.attach(UserDao.class);
            var account = dao.lockById(actor.id()).filter(user -> "ACTIVE".equals(user.status()))
                    .orElseThrow(() -> new FormException(Map.of("form", "Tài khoản không còn hoạt động. Vui lòng đăng nhập lại.")));
            String name = AccountValidation.text(form.displayName());
            if (dao.updateProfile(actor.id(), name, nullable(form.phone()), nullable(form.publicContact())) != 1)
                throw new AccountUnavailableException();
            return new CurrentUser(account.id(), name, account.role());
        }));
    }
    private static void requireActor(CurrentUser actor) {
        if (actor == null) throw new FormException(Map.of("form", "Bạn cần đăng nhập để sử dụng hồ sơ."));
    }
    private static String nullable(String value) { String text = AccountValidation.text(value); return text.isEmpty() ? null : text; }
    private static FormException duplicateEmail() { return new FormException(Map.of("email", "Email này đã được đăng ký.")); }
    private static <T> T operation(Supplier<T> callback) {
        try { return callback.get(); }
        catch (FormException | AccountUnavailableException exception) { throw exception; }
        catch (RuntimeException exception) {
            // MySQL 1062: unique email vẫn bảo vệ khi hai đăng ký đồng thời.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && sql.getErrorCode() == 1062) throw duplicateEmail();
            }
            // Không đưa SQL, bind password_hash, email hoặc credential vào cause/log.
            throw new AccountUnavailableException();
        }
    }
}
