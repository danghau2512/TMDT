package com.example.demo.controller;

import com.example.demo.config.ApplicationRuntime;
import com.example.demo.security.LocalDiagnostics;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/home")
public final class HomeServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var runtime = (ApplicationRuntime) getServletContext().getAttribute(ApplicationRuntime.CONTEXT_KEY);
        request.setAttribute("configurationValid", runtime != null && runtime.configurationValid());
        boolean diagnostics = LocalDiagnostics.allowed(request, runtime);
        request.setAttribute("diagnosticsEnabled", diagnostics);
        if (diagnostics) request.setAttribute("databaseHealth", runtime.healthService().checkDatabase());
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        request.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(request, response);
    }
}
