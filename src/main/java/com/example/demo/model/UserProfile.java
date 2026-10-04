package com.example.demo.model;

/** DTO riêng tư của chủ hồ sơ, không chứa credential. */
public record UserProfile(String email, String displayName, String phone, String publicContact) {
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPhone() { return phone; }
    public String getPublicContact() { return publicContact; }
    @Override public String toString() { return "UserProfile[redacted]"; }
}
