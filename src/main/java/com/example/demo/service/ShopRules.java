package com.example.demo.service;

import com.example.demo.exception.*;
import com.example.demo.model.CurrentUser;
import org.jdbi.v3.core.Handle;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;

public final class ShopRules {
    private ShopRules() { }
    public static long id(Map<String,Object> row, String key) { return ((Number) row.get(key)).longValue(); }
    public static int number(Map<String,Object> row, String key) { return ((Number) row.get(key)).intValue(); }
    public static String text(Map<String,Object> row, String key) { Object value=row.get(key); return value==null ? "" : value.toString(); }
    public static BigDecimal money(Map<String,Object> row, String key) { return (BigDecimal) row.get(key); }
    public static void require(boolean condition, int status, String message) { if (!condition) throw new ShopException(status,message); }
    public static String bounded(String value, int min, int max, String label) {
        String text=AccountValidation.text(value);
        int size=text.codePointCount(0,text.length());
        require(size>=min && size<=max && text.indexOf('\0')<0,400,label+" cần "+min+"–"+max+" ký tự."); return text;
    }
    /** Luôn kiểm actor từ DB ở Service; FOR UPDATE chỉ dùng trong transaction ghi. */
    public static CurrentUser actor(Handle handle, CurrentUser supplied, boolean lock) {
        require(supplied!=null,401,"Bạn cần đăng nhập.");
        var row=handle.createQuery("SELECT id,display_name,role,status FROM users WHERE id=:id"+(lock?" FOR UPDATE":""))
                .bind("id",supplied.id()).mapToMap().findOne().orElseThrow(()->new ShopException(401,"Tài khoản không tồn tại."));
        require("ACTIVE".equals(row.get("status")),403,"Tài khoản không còn hoạt động.");
        return new CurrentUser(id(row,"id"),text(row,"display_name"),text(row,"role"));
    }
    public static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 không khả dụng."); }
    }
    public static <T> T safe(Supplier<T> work) {
        try { return work.get(); }
        catch (ShopException | FormException exception) { throw exception; }
        catch (RuntimeException exception) { throw new ShopException(503,"Không thể kết nối hoặc xử lý dữ liệu. Vui lòng kiểm tra cấu hình và thử lại."); }
    }
}
