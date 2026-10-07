package com.example.demo.controller.auth;

import com.example.demo.controller.AccountSupport;
import com.example.demo.dto.ProfileForm;
import com.example.demo.exception.FormException;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/account/profile")
public final class ProfileServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var profile = AccountSupport.service(getServletContext()).profile(SessionAuth.current(request));
        request.setAttribute("profile", profile);
        request.setAttribute("displayName", profile.displayName());
        request.setAttribute("phone", profile.phone());
        request.setAttribute("publicContact", profile.publicContact());
        request.setAttribute("sellerVerified",com.example.demo.controller.ShopWeb.services(getServletContext()).verification().approved(SessionAuth.current(request)));
        AccountSupport.view(request, response, "auth/profile");
    }
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var service = AccountSupport.service(getServletContext());
        var user = SessionAuth.current(request);
        try {
            var updated = service.updateProfile(user, new ProfileForm(request.getParameter("displayName"), request.getParameter("phone"), request.getParameter("publicContact")));
            request.getSession(false).setAttribute(SessionAuth.USER_KEY, updated);
            AccountSupport.redirect(request, response, "/account/profile?notice=saved");
        } catch (FormException exception) {
            response.setStatus(400);
            request.setAttribute("errors", exception.errors());
            request.setAttribute("profile", service.profile(user));
            request.setAttribute("sellerVerified",com.example.demo.controller.ShopWeb.services(getServletContext()).verification().approved(user));
            for (String field : new String[]{"displayName", "phone", "publicContact"}) request.setAttribute(field, request.getParameter(field));
            AccountSupport.view(request, response, "auth/profile");
        }
    }
}
