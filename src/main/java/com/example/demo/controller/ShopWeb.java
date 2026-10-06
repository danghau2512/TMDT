package com.example.demo.controller;

import com.example.demo.config.ShopServices;
import com.example.demo.exception.ShopException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.math.BigDecimal;
import java.io.IOException;

public final class ShopWeb {
    private ShopWeb() { }
    public static ShopServices services(ServletContext context) { var services=context.getAttribute(ShopServices.KEY); if(services instanceof ShopServices ready) return ready; throw new ShopException(503,"Chức năng mua bán chưa sẵn sàng. Vui lòng kiểm tra cấu hình ứng dụng."); }
    public static String value(HttpServletRequest r,String key) { String value=r.getParameter(key); return value==null?"":value.strip(); }
    public static long number(HttpServletRequest r,String key,long fallback) { String value=value(r,key); if(value.isEmpty()) return fallback; try { return Long.parseLong(value); } catch(NumberFormatException exception) { throw new ShopException(400,"Thông tin số không hợp lệ: "+key); } }
    public static int integer(HttpServletRequest r,String key,int fallback) { long result=number(r,key,fallback); if(result<Integer.MIN_VALUE || result>Integer.MAX_VALUE) throw new ShopException(400,"Giá trị vượt giới hạn."); return (int)result; }
    public static BigDecimal price(HttpServletRequest r,String key) { String value=value(r,key); if(value.isEmpty()) return null; try { if(value.length()>24) throw new NumberFormatException(); var parsed=new BigDecimal(value); if(Math.abs((long)parsed.scale())>12) throw new NumberFormatException(); return parsed; } catch(NumberFormatException exception) { throw new ShopException(400,"Giá không hợp lệ."); } }
    public static void data(HttpServletRequest request,java.util.Map<String,Object> data) { data.forEach(request::setAttribute); }
    public static void view(HttpServletRequest r,HttpServletResponse s,String name) throws ServletException,IOException { AccountSupport.view(r,s,name); }
    public static void redirect(HttpServletRequest r,HttpServletResponse s,String path) { AccountSupport.redirect(r,s,path); }
}
