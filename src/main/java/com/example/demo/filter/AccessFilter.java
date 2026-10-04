package com.example.demo.filter;

import com.example.demo.controller.AccountSupport;
import com.example.demo.exception.*;
import com.example.demo.security.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;

/** Thứ tự Encoding → xác thực/quyền → CSRF → Servlet được khai báo trong web.xml. */
public final class AccessFilter implements Filter {
    private static boolean under(String path, String prefix) { return path.equals(prefix) || path.startsWith(prefix + "/"); }
    public static boolean requiresLogin(String path) {
        return under(path, "/account") || under(path, "/admin") || under(path, "/seller")
                || under(path, "/buyer") || under(path, "/cart") || under(path, "/checkout") || path.equals("/logout");
    }
    @Override public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain) throws IOException, ServletException {
        var request = (HttpServletRequest) input;
        var response = (HttpServletResponse) output;
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        if (request.getServletPath().startsWith("/assets/")) { chain.doFilter(request, response); return; }
        response.setHeader("Cache-Control", "no-store");
        String path = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        try {
            var user = SessionAuth.current(request);
            if (user != null) {
                user = AccountSupport.service(request.getServletContext()).currentActive(user.id()).orElse(null);
                if (user == null) SessionAuth.logout(request);
                else request.getSession(false).setAttribute(SessionAuth.USER_KEY, user);
            }
            request.setAttribute("currentUser", user);
            if (requiresLogin(path) && user == null) {
                AccountSupport.redirect(request, response, "/login?notice=required");
                return;
            }
            if (under(path, "/admin") && !user.isAdmin()) {
                AccountSupport.error(request, response, 403, "Bạn không có quyền truy cập trang quản trị.");
                return;
            }
            if (!Set.of("GET", "HEAD", "OPTIONS", "POST").contains(request.getMethod())) {
                AccountSupport.error(request, response, 405, "Phương thức yêu cầu không được hỗ trợ.");
                return;
            }
            if ("POST".equals(request.getMethod()) && !CsrfTokens.valid(request.getSession(false), request.getParameter("csrfToken"))) {
                AccountSupport.error(request, response, 403, "Biểu mẫu đã hết hạn hoặc mã bảo vệ không hợp lệ. Hãy mở lại trang và thử lại.");
                return;
            }
            request.setAttribute("csrfToken", CsrfTokens.token(request.getSession(true)));
            chain.doFilter(request, response);
        } catch (AccountUnavailableException exception) {
            request.getServletContext().log("Chức năng tài khoản không khả dụng [" + exception.reason()
                    + "]. Kiểm tra APP_CONFIG_FILE / -Dc2c.config của tiến trình Tomcat, kết nối và cấu trúc users.");
            AccountSupport.error(request, response, 503, exception.getMessage());
        } catch (FormException exception) {
            AccountSupport.error(request, response, 400, "Thông tin tài khoản chưa hợp lệ. Hãy mở lại trang và thử lại.");
        }
    }
}
