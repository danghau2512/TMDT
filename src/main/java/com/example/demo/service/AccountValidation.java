package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.FormException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Mật khẩu không trim; email đăng nhập trim/lowercase, giới hạn khớp schema. */
public final class AccountValidation {
    private AccountValidation() { }
    public static String text(String value) { return value == null ? "" : value.strip(); }
    public static String email(String value) { return text(value).toLowerCase(Locale.ROOT); }
    public static boolean validEmail(String value) {
        int at = value.indexOf('@');
        if (at < 1 || at > 64 || value.length() > 254) return false;
        String local = value.substring(0, at);
        if (local.startsWith(".") || local.endsWith(".") || local.contains("..")) return false;
        for (String label : value.substring(at + 1).split("\\.", -1)) if (label.length() > 63) return false;
        return value.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+");
    }
    public static void registration(RegisterForm form) {
        Map<String, String> errors = profileErrors(form.displayName(), form.phone(), "");
        if (!validEmail(email(form.email()))) errors.put("email", "Nhập email hợp lệ, tối đa 254 ký tự.");
        char[] password = form.password();
        if (password == null || Character.codePointCount(password, 0, password.length) < 8 || utf8Bytes(password) > 72)
            errors.put("password", "Mật khẩu ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        if (form.confirmation() == null || !Arrays.equals(password, form.confirmation()))
            errors.put("confirmation", "Mật khẩu xác nhận không khớp.");
        fail(errors);
    }
    public static void profile(ProfileForm form) { fail(profileErrors(form.displayName(), form.phone(), form.publicContact())); }
    private static Map<String, String> profileErrors(String name, String phone, String contact) {
        Map<String, String> errors = new LinkedHashMap<>();
        String normalizedName = text(name);
        if (length(normalizedName) < 2 || length(normalizedName) > 100 || hasControls(normalizedName))
            errors.put("displayName", "Họ tên cần 2–100 ký tự, không chứa ký tự điều khiển.");
        String normalizedPhone = text(phone);
        if (!normalizedPhone.isEmpty() && !normalizedPhone.matches("\\+?[0-9]{9,15}"))
            errors.put("phone", "Số điện thoại gồm 9–15 chữ số, có thể bắt đầu bằng +.");
        if (length(text(contact)) > 255 || hasControls(text(contact)))
            errors.put("publicContact", "Liên hệ công khai tối đa 255 ký tự, không chứa ký tự điều khiển.");
        return errors;
    }
    private static int length(String value) { return value.codePointCount(0, value.length()); }
    private static boolean hasControls(String value) { return value.codePoints().anyMatch(Character::isISOControl); }
    public static int utf8Bytes(char[] password) { return StandardCharsets.UTF_8.encode(CharBuffer.wrap(password)).remaining(); }
    private static void fail(Map<String, String> errors) { if (!errors.isEmpty()) throw new FormException(errors); }
}
