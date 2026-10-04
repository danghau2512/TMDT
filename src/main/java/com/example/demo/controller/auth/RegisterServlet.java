package com.example.demo.controller.auth;

import com.example.demo.controller.AccountSupport;
import com.example.demo.dto.RegisterForm;
import com.example.demo.exception.FormException;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;

@WebServlet("/register")
public final class RegisterServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (SessionAuth.current(request) != null) { AccountSupport.redirect(request, response, "/home"); return; }
        AccountSupport.view(request, response, "auth/register");
    }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (SessionAuth.current(request) != null) { AccountSupport.redirect(request, response, "/home"); return; }
        char[] password = chars(request.getParameter("password"));
        char[] confirmation = chars(request.getParameter("confirmation"));
        try {
            AccountSupport.service(getServletContext()).register(new RegisterForm(request.getParameter("displayName"),
                    request.getParameter("email"), request.getParameter("phone"), password, confirmation));
            AccountSupport.redirect(request, response, "/login?notice=registered");
        } catch (FormException exception) {
            response.setStatus(400);
            request.setAttribute("errors", exception.errors());
            for (String field : new String[]{"displayName", "email", "phone"}) request.setAttribute(field, request.getParameter(field));
            AccountSupport.view(request, response, "auth/register");
        } finally { Arrays.fill(password, '\0'); Arrays.fill(confirmation, '\0'); }
    }
    private static char[] chars(String value) { return value == null ? new char[0] : value.toCharArray(); }
}
