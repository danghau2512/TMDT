package com.example.demo.controller.chat;

import com.example.demo.controller.*;
import com.example.demo.exception.ShopException;
import com.example.demo.security.SessionAuth;
import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet({"/messages","/messages/start","/messages/api","/messages/unread","/messages/send","/messages/read"})
public final class ChatServlet extends HttpServlet {
    private static void json(HttpServletResponse response,Object value) throws IOException {
        response.setContentType("application/json;charset=UTF-8");response.getWriter().write(new Gson().toJson(value));
    }
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        var shop=services(getServletContext());var user=SessionAuth.current(r);String path=r.getServletPath();
        if(path.equals("/messages/unread")){json(s,Map.of("unreadTotal",shop.chat().unread(user)));return;}
        if(path.equals("/messages/start")){
            data(r,shop.products().detail(number(r,"productId",0),user,false));view(r,s,"chat/start");return;
        }
        if(path.equals("/messages/api")){
            long chat=number(r,"conversationId",0);
            json(s,chat==0?shop.chat().inbox(user):shop.chat().thread(user,chat,number(r,"cursor",0),"older".equals(value(r,"direction"))));return;
        }
        if(!path.equals("/messages")){s.sendError(405);return;}
        page(r,s,number(r,"id",0));
    }
    private void page(HttpServletRequest r,HttpServletResponse s,long id) throws ServletException,IOException {
        var chat=services(getServletContext()).chat();var user=SessionAuth.current(r);
        data(r,id>0?chat.thread(user,id,0,true):chat.inbox(user));
        if(r.getAttribute("sendNonce")==null) r.setAttribute("sendNonce",UUID.randomUUID().toString());
        view(r,s,"chat/inbox");
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        var chat=services(getServletContext()).chat();var user=SessionAuth.current(r);String path=r.getServletPath();
        long id=number(r,"conversationId",0);
        if(path.equals("/messages/start")){long opened=chat.start(user,number(r,"productId",0));redirect(r,s,"/messages?id="+opened);return;}
        if(path.equals("/messages/read")){long total=chat.read(user,id,number(r,"through",0));if(AccountSupport.chatJson(r))json(s,Map.of("unreadTotal",total));else redirect(r,s,"/messages?id="+id);return;}
        if(!path.equals("/messages/send")){s.sendError(405);return;}
        try {
            var sent=chat.send(user,id,r.getParameter("body"),r.getParameter("nonce"));
            if(AccountSupport.chatJson(r))json(s,Map.of("message",sent));else redirect(r,s,"/messages?id="+id+"&notice=sent");
        }catch(ShopException error){
            if(AccountSupport.chatJson(r))throw error;
            s.setStatus(error.status());r.setAttribute("chatError",error.getMessage());
            // Giữ bản nháp/nonce để retry đúng một tin khi phản hồi mạng thất lạc.
            r.setAttribute("draft",r.getParameter("body"));r.setAttribute("sendNonce",r.getParameter("nonce"));page(r,s,id);
        }
    }
}
