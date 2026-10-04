package com.example.demo.controller.auth;

import com.example.demo.controller.AccountSupport;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/login")
public final class LoginServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var user = SessionAuth.current(request);
        if (user != null) { AccountSupport.redirect(request, response, user.isAdmin() ? "/admin" : "/home"); return; }
        AccountSupport.view(request, response, "auth/login");
    }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (SessionAuth.current(request) != null) { doGet(request, response); return; }
        String submitted = request.getParameter("password");
        char[] password = submitted == null ? new char[0] : submitted.toCharArray();
        try {
            var user = AccountSupport.service(getServletContext()).authenticate(request.getParameter("email"), password);
            if (user.isEmpty()) {
                response.setStatus(400);
                request.setAttribute("email", request.getParameter("email"));
                request.setAttribute("errors", Map.of("form", "Email hoặc mật khẩu không đúng, hoặc tài khoản không hoạt động."));
                AccountSupport.view(request, response, "auth/login");
                return;
            }
            SessionAuth.login(request, user.get());
            AccountSupport.redirect(request, response, user.get().isAdmin() ? "/admin" : "/home");
        } finally { Arrays.fill(password, '\0'); }
    }
}
