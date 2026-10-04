package com.example.demo.security;

import com.example.demo.model.CurrentUser;
import jakarta.servlet.http.*;

public final class SessionAuth {
    public static final String USER_KEY = "currentUser";
    private SessionAuth() { }
    public static CurrentUser current(HttpServletRequest request) {
        var session = request.getSession(false);
        return session != null && session.getAttribute(USER_KEY) instanceof CurrentUser user ? user : null;
    }
    public static void login(HttpServletRequest request, CurrentUser user) {
        var session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(USER_KEY, user);
        CsrfTokens.rotate(session);
    }
    public static void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
