package com.example.demo.controller.catalog;
import com.example.demo.exception.ShopException;
import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet({"/compare","/compare/items"})
public final class CompareServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {
        boolean json=r.getServletPath().equals("/compare/items");
        try {
            String raw=value(r,"ids");if(raw.length()>100)throw new ShopException(400,"Danh sách so sánh không hợp lệ.");
            var ids=new LinkedHashSet<Long>();
            try { if(!raw.isEmpty())for(String part:raw.split(",",-1)){long id=Long.parseLong(part);if(id<=0)throw new NumberFormatException();ids.add(id);} }
            catch(NumberFormatException e){throw new ShopException(400,"ID sản phẩm không hợp lệ.");}
            var result=services(getServletContext()).products().compare(List.copyOf(ids));
            if(json){s.setContentType("application/json;charset=UTF-8");s.getWriter().write(new Gson().toJson(result));}
            else {data(r,result);view(r,s,"catalog/compare");}
        } catch(ShopException e){if(!json)throw e;s.setStatus(e.status());s.setContentType("application/json;charset=UTF-8");s.getWriter().write(new Gson().toJson(Map.of("error",e.getMessage())));}
    }
}
