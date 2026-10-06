package com.example.demo.controller.catalog;

import com.example.demo.controller.*;
import com.example.demo.dto.*;
import com.example.demo.exception.ShopException;
import com.example.demo.model.StoredImage;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet({"/products","/products/detail","/categories","/seller/products","/seller/products/new","/seller/products/edit","/seller/products/save","/seller/products/action","/admin/products","/admin/products/new","/admin/products/edit","/admin/products/save","/admin/products/action"})
@MultipartConfig(maxFileSize=5*1024*1024,maxRequestSize=26*1024*1024,fileSizeThreshold=0)
public final class ProductServlet extends HttpServlet {
    private void context(HttpServletRequest r) { boolean admin=r.getServletPath().startsWith("/admin/"); r.setAttribute("adminMode",admin); r.setAttribute("manageBase",admin?"/admin/products":"/seller/products"); }
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        var products=services(getServletContext()).products(); String path=r.getServletPath(); context(r);
        if(path.equals("/products") || path.equals("/categories")) { data(r,products.catalog(new CatalogFilter(value(r,"keyword"),number(r,"category",0),price(r,"min"),price(r,"max"),value(r,"condition"),integer(r,"page",1)))); r.setAttribute("page",integer(r,"page",1)); view(r,s,"catalog/list"); }
        else if(path.equals("/products/detail")) { data(r,products.detail(number(r,"id",0),SessionAuth.current(r),false,integer(r,"stars",0),"true".equals(value(r,"photos")),integer(r,"reviewPage",1))); view(r,s,"catalog/detail"); }
        else if(path.endsWith("/new") || path.endsWith("/edit")) {
            data(r,products.managed(SessionAuth.current(r),path.startsWith("/admin/"),""));
            if(path.endsWith("/edit")) data(r,products.detail(number(r,"id",0),SessionAuth.current(r),true));
            view(r,s,"seller/product-form");
        } else if(path.endsWith("/products")) { data(r,products.managed(SessionAuth.current(r),path.startsWith("/admin/"),value(r,"status"))); view(r,s,"seller/products"); }
        else s.sendError(405);
    }
    private Set<Long> longSet(HttpServletRequest r,String name) {
        var result=new HashSet<Long>();var inputs=r.getParameterValues(name);if(inputs==null)return result;
        if(inputs.length>5)throw new ShopException(400,"Tối đa 5 ảnh khuyết điểm.");
        try { for(String input:inputs) {long id=Long.parseLong(input);if(id<=0)throw new NumberFormatException();result.add(id);} }
        catch(NumberFormatException e){throw new ShopException(400,"Ảnh khuyết điểm không hợp lệ.");}return result;
    }
    private Set<Integer> indexSet(HttpServletRequest r) { var result=new HashSet<Integer>();var inputs=r.getParameterValues("defectIndex");if(inputs==null)return result;
        if(inputs.length>5)throw new ShopException(400,"Tối đa 5 ảnh khuyết điểm.");try {for(String input:inputs)result.add(Integer.parseInt(input));}catch(NumberFormatException e){throw new ShopException(400,"Ảnh khuyết điểm không hợp lệ.");}return result; }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        var shop=services(getServletContext()); String path=r.getServletPath(); boolean admin=path.startsWith("/admin/"); context(r); String base=admin?"/admin/products":"/seller/products";
        if(path.endsWith("/action")) { shop.products().action(SessionAuth.current(r),number(r,"id",0),value(r,"action"),number(r,"listingVersion",0),value(r,"reason"),admin); redirect(r,s,base+"?notice=saved"); return; }
        if(!path.endsWith("/save")) { s.sendError(405); return; }
        var images=new ArrayList<StoredImage>(); boolean committed=false;
        try {
            long id=number(r,"id",0);
            var form=new ProductForm(value(r,"title"),number(r,"categoryId",0),value(r,"description"),price(r,"price"),value(r,"condition"),integer(r,"stock",-1),value(r,"visibility"),number(r,"sellerId",0),number(r,"listingVersion",0),number(r,"stockVersion",0),value(r,"reason"),new ConditionForm(value(r,"appearance"),value(r,"operation"),value(r,"defects"),value(r,"repair"),value(r,"repairDetails"),value(r,"accessories"),longSet(r,"defectAsset"),indexSet(r)));
            var parts=r.getParts().stream().filter(p->"photos".equals(p.getName()) && p.getSize()>0).toList();
            if(parts.size()>5) throw new ShopException(400,"Tối đa 5 ảnh cho một tin.");
            for(var part:parts) images.add(shop.storage().save(part));
            shop.products().save(SessionAuth.current(r),id==0?null:id,form,images,admin); committed=true;
            redirect(r,s,base+"?notice=saved");
        } catch(ShopException exception) {
            if(exception.status()!=400 && exception.status()!=409) throw exception;
            s.setStatus(exception.status()); r.setAttribute("formError",exception.getMessage());
            data(r,shop.products().managed(SessionAuth.current(r),admin,""));
            long id=number(r,"id",0); if(id>0) data(r,shop.products().detail(id,SessionAuth.current(r),true));
            r.setAttribute("posted",true); view(r,s,"seller/product-form");
        } finally { if(!committed) shop.storage().discard(images); }
    }
}
