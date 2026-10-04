package com.example.demo.model;

import java.io.Serializable;

/** Dữ liệu tối thiểu trong session, không có email/hash/mật khẩu. */
public record CurrentUser(long id, String displayName, String role) implements Serializable {
    public long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
    @Override public String toString() { return "CurrentUser[redacted]"; }
}
