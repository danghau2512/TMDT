package com.example.demo.controller.buyer;

import com.example.demo.controller.*;
import com.example.demo.dto.CheckoutForm;
import com.example.demo.exception.ShopException;
import com.example.demo.security.SessionAuth;
import com.example.demo.model.CartResponse;
import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet({"/cart","/cart/add","/cart/update","/cart/remove","/checkout","/checkout/success"})
public final class CartCheckoutServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        var shop=services(getServletContext()); String path=r.getServletPath();
        if(path.equals("/checkout/success")) { r.setAttribute("orders",shop.orders().receipt(SessionAuth.current(r),number(r,"batch",0))); view(r,s,"buyer/receipt"); return; }
        if(!path.equals("/cart") && !path.equals("/checkout")) { s.sendError(405); return; }
        if(AccountSupport.cartJson(r)) { json(s,shop.cart().view(SessionAuth.current(r))); return; }
        r.setAttribute("cart",shop.cart().view(SessionAuth.current(r)));
        if(path.equals("/checkout")) { r.setAttribute("checkoutKey",UUID.randomUUID().toString()); view(r,s,"buyer/checkout"); }
        else view(r,s,"buyer/cart");
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        var shop=services(getServletContext()); String path=r.getServletPath();
        if(path.equals("/checkout")) {
            try {
                long batch=shop.orders().checkout(SessionAuth.current(r),new CheckoutForm(value(r,"name"),value(r,"phone"),value(r,"address"),value(r,"note"),value(r,"method"),value(r,"key"),value(r,"quote")));
                redirect(r,s,"/checkout/success?batch="+batch);
            } catch(ShopException exception) {
                if(exception.status()!=400 && exception.status()!=409) throw exception;
                s.setStatus(exception.status()); r.setAttribute("formError",exception.getMessage()); r.setAttribute("cart",shop.cart().view(SessionAuth.current(r)));
                // Trang lỗi hiển thị giá mới và cấp key mới; request cũ vẫn retry idempotent tại Service.
                r.setAttribute("checkoutKey",UUID.randomUUID().toString()); view(r,s,"buyer/checkout");
            }
        } else if(path.startsWith("/cart/")) { String action=switch(path) { case "/cart/add"->"ADD"; case "/cart/update"->"UPDATE"; case "/cart/remove"->"REMOVE"; default->""; };
            int quantity=integer(r,"quantity",1);
            // Nút −/+ vẫn submit form thường nếu JS bị tắt; AJAX gửi số lượng tuyệt đối.
            if(path.equals("/cart/update") && !value(r,"adjust").isEmpty()) {
                String adjust=value(r,"adjust");
                if(!adjust.equals("-1") && !adjust.equals("1"))throw new ShopException(400,"Thao tác số lượng không hợp lệ.");
                long next=(long)quantity+Integer.parseInt(adjust);
                if(next<1 || next>999)throw new ShopException(400,"Số lượng từ 1 đến 999.");
                quantity=(int)next;
            }
            if(AccountSupport.cartJson(r))json(s,shop.cart().changeAndView(SessionAuth.current(r),number(r,"productId",0),quantity,action));
            else {shop.cart().change(SessionAuth.current(r),number(r,"productId",0),quantity,action);redirect(r,s,"/cart?notice=saved");}
        } else s.sendError(405);
    }
    private void json(HttpServletResponse response,com.example.demo.model.CartSummary cart)throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new Gson().toJson(CartResponse.from(cart)));
    }
}
