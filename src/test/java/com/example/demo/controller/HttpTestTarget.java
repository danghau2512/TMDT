package com.example.demo.controller;

import com.example.demo.config.Database;
import com.example.demo.dto.RegisterForm;
import com.example.demo.service.UserService;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Chứng minh Tomcat đọc đúng DB test trước khi gửi HTTP tạo/sửa dữ liệu. */
final class HttpTestTarget {
    static void verify(Database db,String base) throws Exception {
        String nonce=UUID.randomUUID().toString(),email=nonce+"@target-test.example",password="Target!"+UUID.randomUUID();
        new UserService(db).register(new RegisterForm(nonce,email,"",password.toCharArray(),password.toCharArray()));
        try {
            var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
            var login=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            var token=Pattern.compile("name=\"csrfToken\" value=\"([^\"]+)\"").matcher(login.body()); assertTrue(token.find());
            String body="csrfToken="+URLEncoder.encode(token.group(1),StandardCharsets.UTF_8)+"&email="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8);
            var response=client.send(HttpRequest.newBuilder(URI.create(base+"/login")).header("Content-Type","application/x-www-form-urlencoded;charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.discarding());
            assertEquals(303,response.statusCode(),"Tomcat phải dùng cùng schema test của APP_CONFIG_FILE; dừng trước HTTP ghi.");
            var profile=client.send(HttpRequest.newBuilder(URI.create(base+"/account/profile")).GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200,profile.statusCode()); assertTrue(profile.body().contains(nonce),"Tomcat không khớp DB test.");
        } finally { db.transaction(h->{h.createUpdate("DELETE FROM users WHERE email=:email").bind("email",email).execute();return null;}); }
    }
}
