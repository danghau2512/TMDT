package com.example.demo.dto;

public record ProfileForm(String displayName, String phone, String publicContact) {
    @Override public String toString() { return "ProfileForm[redacted]"; }
}
