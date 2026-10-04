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
        request.setAttribute("errorMessage", message);
        view(request, response, "errors/message");
    }
}
