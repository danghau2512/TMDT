package com.example.demo.controller.auth;

import com.example.demo.controller.AccountSupport;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/logout")
public final class LogoutServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        SessionAuth.logout(request);
        AccountSupport.redirect(request, response, "/home?notice=logged-out");
    }
}
