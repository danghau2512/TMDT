package com.example.demo.security;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public final class CsrfTokens {
    private static final String KEY = CsrfTokens.class.getName();
    private static final SecureRandom RANDOM = new SecureRandom();
    private CsrfTokens() { }
    public static String token(HttpSession session) {
        synchronized (session) {
            Object token = session.getAttribute(KEY);
            if (token instanceof String existing) return existing;
            return rotate(session);
        }
    }
    public static String rotate(HttpSession session) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(KEY, token);
        return token;
    }
    public static boolean valid(HttpSession session, String submitted) {
        if (session == null || submitted == null || submitted.length() != 43) return false;
        Object saved = session.getAttribute(KEY);
        return saved instanceof String expected && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII), submitted.getBytes(StandardCharsets.US_ASCII));
    }
}
