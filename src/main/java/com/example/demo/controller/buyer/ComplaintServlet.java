package com.example.demo.controller.buyer;

import com.example.demo.exception.ShopException;
import com.example.demo.model.StoredImage;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet({"/buyer/complaints","/buyer/complaints/new","/buyer/complaints/create","/buyer/complaints/detail","/buyer/complaints/supplement",
        "/admin/complaints","/admin/complaints/detail","/admin/complaints/action"})
@MultipartConfig(maxFileSize=5*1024*1024,maxRequestSize=26*1024*1024,fileSizeThreshold=0)
public final class ComplaintServlet extends HttpServlet {
    private boolean context(HttpServletRequest r) {
        boolean admin=r.getServletPath().startsWith("/admin/");
        r.setAttribute("complaintAdmin",admin); r.setAttribute("complaintBase",admin?"/admin/complaints":"/buyer/complaints");
        return admin;
    }
    private void detail(HttpServletRequest r,HttpServletResponse s,boolean admin) throws ServletException,IOException {
        data(r,services(getServletContext()).complaints().detail(SessionAuth.current(r),number(r,"id",0),admin));
        view(r,s,"complaints/detail");
    }
    private void form(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        var data=services(getServletContext()).complaints().form(SessionAuth.current(r),number(r,"orderId",0));
        var previous=(Map<?,?>)data.get("complaint");
        if(!previous.isEmpty()) { redirect(r,s,"/buyer/complaints/detail?id="+previous.get("id")); return; }
        data(r,data); view(r,s,"complaints/form");
    }
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        boolean admin=context(r); String path=r.getServletPath();
        if(path.endsWith("/new")) form(r,s);
        else if(path.endsWith("/detail")) detail(r,s,admin);
        else if(path.endsWith("/complaints")) {
            data(r,services(getServletContext()).complaints().center(SessionAuth.current(r),admin,value(r,"status"),value(r,"query")));
            view(r,s,"complaints/list");
        } else s.sendError(405);
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        boolean admin=context(r); String path=r.getServletPath(); var shop=services(getServletContext());
        if(!Set.of("/buyer/complaints/create","/buyer/complaints/supplement","/admin/complaints/action").contains(path)) { s.sendError(405); return; }
        var images=new ArrayList<StoredImage>(); boolean committed=false;
        try {
            long complaint=number(r,"id",0);
            if(path.endsWith("/action")) shop.complaints().act(SessionAuth.current(r),complaint,value(r,"action"),value(r,"response"),value(r,"resolution"),value(r,"summary"));
            else {
                if(r.getContentType()==null || !r.getContentType().startsWith("multipart/")) throw new ShopException(400,"Cần gửi bằng biểu mẫu khiếu nại có hỗ trợ ảnh.");
                var parts=r.getParts().stream().filter(p->"photos".equals(p.getName()) && p.getSize()>0).toList();
                if(parts.size()>5) throw new ShopException(400,"Tối đa 5 ảnh cho một lần gửi.");
                for(var part:parts) images.add(shop.storage().save(part));
                if(path.endsWith("/create")) {
                    var result=shop.complaints().create(SessionAuth.current(r),number(r,"orderId",0),value(r,"reason"),value(r,"content"),images);
                    complaint=result.id(); committed=result.created();
                } else { shop.complaints().supplement(SessionAuth.current(r),complaint,value(r,"content"),images); committed=true; }
            }
            redirect(r,s,(admin?"/admin/complaints":"/buyer/complaints")+"/detail?id="+complaint+"&notice=saved");
        } catch(ShopException exception) {
            if(exception.status()!=400 && exception.status()!=409) throw exception;
            s.setStatus(exception.status()); r.setAttribute("formError",exception.getMessage());
            if(path.endsWith("/create")) form(r,s); else detail(r,s,admin);
        } finally { if(!committed) shop.storage().discard(images); }
    }
}
