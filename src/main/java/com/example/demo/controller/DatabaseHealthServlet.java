package com.example.demo.controller;

import com.example.demo.config.ApplicationRuntime;
import com.example.demo.security.LocalDiagnostics;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/health/db")
public final class DatabaseHealthServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var runtime = (ApplicationRuntime) getServletContext().getAttribute(ApplicationRuntime.CONTEXT_KEY);
        if (!LocalDiagnostics.allowed(request, runtime)) { response.sendError(404); return; }
        var health = runtime.healthService().checkDatabase();
        response.setStatus(health.isConnected() ? 200 : 503);
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().println(health.getState() + ": " + health.getMessage());
    }
}
