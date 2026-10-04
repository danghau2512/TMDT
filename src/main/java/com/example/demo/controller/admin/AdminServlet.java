package com.example.demo.controller.admin;

import com.example.demo.controller.AccountSupport;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet({"/admin", "/admin/"})
public final class AdminServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AccountSupport.view(request, response, "admin/index");
    }
}
