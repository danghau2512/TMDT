package com.example.demo.dao;

import com.example.demo.model.*;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.*;
import java.util.Optional;

public interface UserDao {
    String ACCOUNT_COLUMNS = "id, email, password_hash AS passwordHash, display_name AS displayName, phone, public_contact AS publicContact, role, status";

    @SqlQuery("SELECT " + ACCOUNT_COLUMNS + " FROM users WHERE email=:email")
    @RegisterConstructorMapper(UserAccount.class)
    Optional<UserAccount> findByEmail(@Bind("email") String email);

    @SqlQuery("SELECT " + ACCOUNT_COLUMNS + " FROM users WHERE id=:id FOR UPDATE")
    @RegisterConstructorMapper(UserAccount.class)
    Optional<UserAccount> lockById(@Bind("id") long id);

    @SqlQuery("SELECT id, display_name AS displayName, role FROM users WHERE id=:id AND status='ACTIVE'")
    @RegisterConstructorMapper(CurrentUser.class)
    Optional<CurrentUser> findActive(@Bind("id") long id);

    @SqlQuery("SELECT email, display_name AS displayName, phone, public_contact AS publicContact FROM users WHERE id=:id AND status='ACTIVE'")
    @RegisterConstructorMapper(UserProfile.class)
    Optional<UserProfile> profile(@Bind("id") long id);

    @SqlUpdate("INSERT INTO users(email,password_hash,display_name,phone,role,status) VALUES(:email,:hash,:name,:phone,'USER','ACTIVE')")
    void insert(@Bind("email") String email, @Bind("hash") String hash, @Bind("name") String name, @Bind("phone") String phone);

    @SqlUpdate("UPDATE users SET display_name=:name, phone=:phone, public_contact=:contact, updated_at=CURRENT_TIMESTAMP(6) WHERE id=:id AND status='ACTIVE'")
    int updateProfile(@Bind("id") long id, @Bind("name") String name, @Bind("phone") String phone, @Bind("contact") String contact);
}
