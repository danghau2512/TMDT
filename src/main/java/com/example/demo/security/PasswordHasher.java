package com.example.demo.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/** Hash dùng chung cho seed và M2; chưa triển khai chức năng đăng nhập. */
public final class PasswordHasher {
    public static final int COST = 12;
    public String hash(char[] password) {
        validate(password);
        return BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(COST, password);
    }
    public boolean verify(char[] password, String hash) {
        validate(password);
        return hash != null && hash.startsWith("$2b$") && BCrypt.verifyer().verify(password, hash).verified;
    }
    private static void validate(char[] password) {
        if (password == null || password.length == 0 || StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)).remaining() > 72) {
            throw new IllegalArgumentException("Mật khẩu phải có 1–72 byte UTF-8; không cắt ngắn âm thầm.");
        }
    }
}
