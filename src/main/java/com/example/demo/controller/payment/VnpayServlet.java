package com.example.demo.controller.payment;

import com.example.demo.controller.ShopWeb;
import com.example.demo.exception.ShopException;
import com.example.demo.model.VnpayAcknowledgement;
import com.example.demo.security.SessionAuth;
import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet({"/payments/vnpay/create","/payments/vnpay/return","/payments/vnpay/result","/payments/vnpay/ipn","/payments/vnpay/query"})
public final class VnpayServlet extends HttpServlet {
    private static Map<String,String> parameters(HttpServletRequest request) {
        var result=new HashMap<String,String>();
        request.getParameterMap().forEach((key,values)->{
            if(key.startsWith("vnp_")) {
                if(!key.matches("vnp_[A-Za-z0-9]{1,60}") || values.length!=1 || values[0].length()>2048) throw new ShopException(400,"Tham số kết quả VNPAY không hợp lệ.");
                result.put(key,values[0]);
            }
        });
        if(result.size()>30) throw new ShopException(400,"Quá nhiều tham số VNPAY.");
        return result;
    }
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        String path=r.getServletPath();
        if(path.equals("/payments/vnpay/ipn")) {
            VnpayAcknowledgement ack;
            try { ack=ShopWeb.services(getServletContext()).payments().ipn(parameters(r)); }
            catch(RuntimeException e) { ack=VnpayAcknowledgement.of("99"); }
            s.setContentType("application/json; charset=UTF-8");
            s.getWriter().write(new Gson().toJson(ack));return;
        }
        var payments=ShopWeb.services(getServletContext()).payments();
        String ref;
        if(path.equals("/payments/vnpay/return")) {
            var params=parameters(r);payments.validateReturn(params);ref=params.get("vnp_TxnRef");
        } else if(path.equals("/payments/vnpay/result")) ref=ShopWeb.value(r,"ref");
        else { s.sendError(405);return; }
        ShopWeb.data(r,payments.view(SessionAuth.current(r),ref));ShopWeb.view(r,s,"payments/result");
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        var payments=ShopWeb.services(getServletContext()).payments();
        switch(r.getServletPath()) {
            case "/payments/vnpay/create" -> {
                String url=payments.create(SessionAuth.current(r),ShopWeb.number(r,"orderId",0),r.getRemoteAddr());
                s.setStatus(303);s.setHeader("Location",url);
            }
            case "/payments/vnpay/query" -> {
                String ref=ShopWeb.value(r,"ref");
                // querydr yêu cầu IP của máy chủ; không tin header proxy từ request.
                String notice;
                try { notice=payments.query(SessionAuth.current(r),ref,r.getLocalAddr()); }
                catch(ShopException e) {
                    if(!Set.of(429,502,503).contains(e.status())) throw e;
                    s.setStatus(e.status());notice=e.getMessage();
                }
                ShopWeb.data(r,payments.view(SessionAuth.current(r),ref));r.setAttribute("queryNotice",notice);
                ShopWeb.view(r,s,"payments/result");
            }
            default -> s.sendError(405);
        }
    }
}
