package com.example.demo.controller.orders;

import com.example.demo.controller.*;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet({"/buyer/orders","/buyer/orders/detail","/buyer/orders/action","/seller/orders","/seller/orders/detail","/seller/orders/action","/admin/orders","/admin/orders/detail","/admin/orders/action"})
public final class OrdersServlet extends HttpServlet {
    private String context(HttpServletRequest r) { String view=r.getServletPath().split("/")[1]; r.setAttribute("orderView",view); r.setAttribute("orderBase","/"+view+"/orders"); return view; }
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        var orders=services(getServletContext()).orders(); String view=context(r);
        if(r.getServletPath().endsWith("/detail")) { data(r,orders.detail(SessionAuth.current(r),number(r,"id",0),view)); view(r,s,"orders/detail"); }
        else if(r.getServletPath().endsWith("/orders")) { r.setAttribute("orders",orders.list(SessionAuth.current(r),view,value(r,"status"))); view(r,s,"orders/list"); }
        else s.sendError(405);
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        if(!r.getServletPath().endsWith("/action")) { s.sendError(405); return; }
        String view=context(r); long order=number(r,"id",0);
        services(getServletContext()).orders().action(SessionAuth.current(r),order,view,value(r,"action"),value(r,"reason"));
        redirect(r,s,"/"+view+"/orders/detail?id="+order+"&notice=saved");
    }
}
