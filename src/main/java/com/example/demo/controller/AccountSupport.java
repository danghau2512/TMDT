package com.example.demo.controller;

import com.example.demo.exception.AccountUnavailableException;
import com.example.demo.config.ApplicationRuntime;
import com.example.demo.service.UserService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public final class AccountSupport {
    public static final String SERVICE_KEY = UserService.class.getName();
    private AccountSupport() { }
    public static UserService service(ServletContext context) {
        if (context.getAttribute(SERVICE_KEY) instanceof UserService service) return service;
        var runtime = (ApplicationRuntime) context.getAttribute(ApplicationRuntime.CONTEXT_KEY);
        var reason = AccountUnavailableException.Reason.SERVICE_NOT_INITIALIZED;
        if (runtime != null && !runtime.configurationValid()) reason = AccountUnavailableException.Reason.DATABASE_CONFIGURATION_INVALID;
        else if (runtime != null && runtime.database().isEmpty()) reason = AccountUnavailableException.Reason.DATABASE_NOT_CONFIGURED;
        throw new AccountUnavailableException(reason);
    }
    public static void redirect(HttpServletRequest request, HttpServletResponse response, String localPath) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", request.getContextPath() + localPath);
    }
    public static void view(HttpServletRequest request, HttpServletResponse response, String name) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        request.getRequestDispatcher("/WEB-INF/views/" + name + ".jsp").forward(request, response);
    }
    public static void error(HttpServletRequest request, HttpServletResponse response, int status, String message) throws ServletException, IOException {
        response.setStatus(status);
        if (chatJson(request)) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new com.google.gson.Gson().toJson(java.util.Map.of("error", message)));
            return;
        }
        request.setAttribute("errorMessage", message);
        view(request, response, "errors/message");
    }
    public static boolean chatJson(HttpServletRequest request) {
        String path=request.getServletPath();
        // Giữ lỗi từ Filter là JSON cho endpoint gợi ý công khai.
        if(path.equals("/products/suggestions"))return true;
        if(cartJson(request))return true;
        return path.startsWith("/messages") && (path.equals("/messages/api") || path.equals("/messages/unread")
                || "application/json".equals(request.getHeader("Accept")));
    }
    public static boolean cartJson(HttpServletRequest request) {
        return java.util.Set.of("/cart","/cart/update","/cart/remove").contains(request.getServletPath())
                && "application/json".equals(request.getHeader("Accept"));
    }
}
