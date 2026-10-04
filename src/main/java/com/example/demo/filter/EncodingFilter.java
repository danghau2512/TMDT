package com.example.demo.filter;

import jakarta.servlet.*;
import java.io.IOException;

public final class EncodingFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        chain.doFilter(request, response);
    }
}
