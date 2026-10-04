package com.example.demo.dto;

public record RegisterForm(String displayName, String email, String phone, char[] password, char[] confirmation) {
    @Override public String toString() { return "RegisterForm[redacted]"; }
}
