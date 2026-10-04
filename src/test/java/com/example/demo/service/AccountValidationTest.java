package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.FormException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountValidationTest {
    @Test void trimsAndNormalizesEmailWhileAllowingVietnameseNamesAndOptionalPhone() {
        assertEquals("an@example.com", AccountValidation.email(" An@EXAMPLE.com "));
        assertDoesNotThrow(() -> AccountValidation.registration(new RegisterForm(" Nguyễn An ", " An@EXAMPLE.com ", "", "Secret!2026".toCharArray(), "Secret!2026".toCharArray())));
        assertDoesNotThrow(() -> AccountValidation.profile(new ProfileForm("Nguyễn An", "+84901234567", "Liên hệ qua cửa hàng")));
    }
    @Test void missingFieldsAndMismatchedConfirmationHaveFieldErrorsWithoutCredentials() {
        var error = assertThrows(FormException.class, () -> AccountValidation.registration(new RegisterForm("", "", "abc", "private".toCharArray(), "different".toCharArray())));
        assertTrue(error.errors().keySet().containsAll(java.util.Set.of("displayName", "email", "phone", "password", "confirmation")));
        assertFalse(error.errors().toString().contains("private"));
    }
    @Test void rejectsInvalidEmailsRatherThanLeavingThemToDatabase() {
        for (String input : new String[]{"bad", "a@localhost", "a..b@example.com", ".a@example.com", "a.@example.com", "a@-bad.com", "a b@example.com", "a".repeat(65) + "@example.com"})
            assertFalse(AccountValidation.validEmail(input), input);
    }
    @Test void rejectsPasswordsOver72Utf8BytesAndCountsUnicodeCharacters() {
        String password = "ệ".repeat(25);
        assertTrue(assertThrows(FormException.class, () -> AccountValidation.registration(new RegisterForm("Nguyễn An", "an@example.com", null, password.toCharArray(), password.toCharArray()))).errors().containsKey("password"));
        String shortUnicodePassword = "😀".repeat(4);
        var form = new RegisterForm("Nguyễn An", "an@example.com", null, shortUnicodePassword.toCharArray(), shortUnicodePassword.toCharArray());
        assertTrue(assertThrows(FormException.class, () -> AccountValidation.registration(form)).errors().containsKey("password"));
    }
    @Test void profileValidationRejectsControlCharactersAndOversizedContact() {
        var error = assertThrows(FormException.class, () -> AccountValidation.profile(new ProfileForm("An\nAdmin", "12", "x".repeat(256))));
        assertTrue(error.errors().keySet().containsAll(java.util.Set.of("displayName", "phone", "publicContact")));
    }
}
