package com.example.demo.controller.catalog;

import com.example.demo.exception.ShopException;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet("/products/suggestions")
public final class SearchSuggestionsServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control","no-store");
        try {
            var items=services(getServletContext()).products().suggestions(request.getParameter("keyword"));
            response.getWriter().write(new Gson().toJson(Map.of("products",items)));
        } catch(ShopException exception) {
            response.setStatus(exception.status());
            response.getWriter().write(new Gson().toJson(Map.of("error",exception.getMessage())));
        }
    }
}
