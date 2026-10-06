package com.example.demo.controller.buyer;

import com.example.demo.exception.ShopException;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet({"/buyer/reviews/new","/buyer/reviews/create"})
@MultipartConfig(maxFileSize=5*1024*1024,maxRequestSize=16*1024*1024,fileSizeThreshold=0)
public final class ReviewServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        if(!r.getServletPath().endsWith("/new")) { s.sendError(405); return; }
        show(r,s);
    }
    private void show(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        data(r,services(getServletContext()).reviews().form(SessionAuth.current(r),number(r,"orderId",0),number(r,"itemId",0)));
        view(r,s,"reviews/form");
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        if(!r.getServletPath().endsWith("/create")) { s.sendError(405); return; }
        var shop=services(getServletContext());var images=new java.util.ArrayList<com.example.demo.model.StoredImage>();boolean committed=false;
        try {
            long order=number(r,"orderId",0);
            if(r.getContentType()!=null && r.getContentType().startsWith("multipart/")) {
                var parts=r.getParts().stream().filter(p->"photos".equals(p.getName()) && p.getSize()>0).toList();
                if(parts.size()>3)throw new ShopException(400,"Tối đa 3 ảnh thực tế cho một đánh giá.");
                for(var part:parts)images.add(shop.storage().save(part));
            }
            shop.reviews().create(SessionAuth.current(r),order,number(r,"itemId",0),integer(r,"rating",0),value(r,"comment"),images);committed=true;
            redirect(r,s,"/buyer/orders/detail?id="+order+"&notice=saved");
        } catch(ShopException exception) {
            if(exception.status()!=400 && exception.status()!=409) throw exception;
            s.setStatus(exception.status()); r.setAttribute("formError",exception.getMessage()); show(r,s);
        } finally { if(!committed)shop.storage().discard(images); }
    }
}
