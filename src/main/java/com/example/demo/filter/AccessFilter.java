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
                || under(path, "/buyer") || under(path, "/cart") || under(path, "/checkout") || under(path,"/payments") || under(path,"/messages") || path.equals("/logout");
    }
    @Override public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain) throws IOException, ServletException {
        var request = (HttpServletRequest) input;
        var response = (HttpServletResponse) output;
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        if (request.getServletPath().startsWith("/assets/")) { chain.doFilter(request, response); return; }
        response.setHeader("Cache-Control", "no-store");
        String path = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        // Chỉ IPN GET được miễn session/CSRF; Servlet luôn kiểm checksum trước khi ghi.
        if(path.equals("/payments/vnpay/ipn")) {
            if(!"GET".equals(request.getMethod())) { response.sendError(405); return; }
            chain.doFilter(request,response); return;
        }
        try {
            var user = SessionAuth.current(request);
            if (user != null) {
                user = AccountSupport.service(request.getServletContext()).currentActive(user.id()).orElse(null);
                if (user == null) SessionAuth.logout(request);
                else request.getSession(false).setAttribute(SessionAuth.USER_KEY, user);
            }
            request.setAttribute("currentUser", user);
            if (requiresLogin(path) && user == null) {
                if (AccountSupport.chatJson(request)) { AccountSupport.error(request,response,401,AccountSupport.cartJson(request)?"Phiên đăng nhập đã hết hạn. Đăng nhập lại để tiếp tục dùng giỏ hàng.":"Phiên đăng nhập đã hết hạn. Đăng nhập lại để tiếp tục chat."); return; }
                if (path.equals("/messages/start")) {
                    String product=request.getParameter("productId");
                    if(product!=null && product.matches("[1-9][0-9]{0,17}"))
                        request.getSession(true).setAttribute("chatReturnTo","/messages/start?productId="+product);
                }
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
        } catch (ShopException exception) {
            if(exception.status()==503) request.getServletContext().log("Chức năng mua bán không khả dụng; kiểm tra cấu hình và schema.");
            AccountSupport.error(request,response,exception.status(),exception.getMessage());
        } catch (IllegalStateException exception) {
            if(request.getContentType()!=null && request.getContentType().startsWith("multipart/"))
                AccountSupport.error(request,response,400,"Ảnh hoặc biểu mẫu vượt giới hạn. Tối đa "
                    +(path.startsWith("/seller/verification/")?2:path.startsWith("/buyer/reviews/")?3:5)+" ảnh, mỗi ảnh 5 MB.");
            else throw exception;
        } catch (AccountUnavailableException exception) {
            request.getServletContext().log("Chức năng tài khoản không khả dụng [" + exception.reason()
                    + "]. Kiểm tra APP_CONFIG_FILE / -Dc2c.config của tiến trình Tomcat, kết nối và cấu trúc users.");
            AccountSupport.error(request, response, 503, exception.getMessage());
        } catch (FormException exception) {
            AccountSupport.error(request, response, 400, "Thông tin tài khoản chưa hợp lệ. Hãy mở lại trang và thử lại.");
        }
    }
}
