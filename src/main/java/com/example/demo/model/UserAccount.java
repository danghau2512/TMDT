package com.example.demo.model;

/** Chỉ Service/DAO dùng; không đưa đối tượng chứa hash vào session hoặc JSP. */
public record UserAccount(long id, String email, String passwordHash, String displayName,
                          String phone, String publicContact, String role, String status) {
    public CurrentUser currentUser() { return new CurrentUser(id, displayName, role); }
    @Override public String toString() { return "UserAccount[redacted]"; }
}
