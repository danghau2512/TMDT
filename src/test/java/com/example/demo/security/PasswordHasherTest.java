package com.example.demo.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void hashesWithDistinctSaltsAndVerifies() {
        PasswordHasher hasher = new PasswordHasher();
        char[] password = "C2cDemo!2026".toCharArray();
        String first = hasher.hash(password);
        String second = hasher.hash(password);
        assertNotEquals(first, second);
        assertTrue(first.startsWith("$2b$12$"));
        assertTrue(hasher.verify(password, first));
        assertFalse(hasher.verify("incorrect".toCharArray(), first));
    }
    @Test void rejectsLongUtf8PasswordRatherThanTruncating() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHasher().hash("ệ".repeat(25).toCharArray()));
    }
}
