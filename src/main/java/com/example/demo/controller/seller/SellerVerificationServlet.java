package com.example.demo.controller.seller;

import com.example.demo.controller.*;
import com.example.demo.exception.ShopException;
import com.example.demo.model.StoredImage;
import com.example.demo.dto.VerificationForm;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet({"/seller/verification","/seller/verification/upload","/seller/verification/read","/seller/verification/submit","/seller/verification/purge","/seller/verification/image","/admin/verifications","/admin/verifications/detail","/admin/verifications/claim","/admin/verifications/decide"})
@MultipartConfig(maxFileSize=5*1024*1024,maxRequestSize=11*1024*1024,fileSizeThreshold=0)
public final class SellerVerificationServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {
        var service=services(getServletContext()).verification();var user=SessionAuth.current(r);String path=r.getServletPath();
        if(path.equals("/seller/verification/image")){var image=service.image(user,number(r,"id",0),value(r,"side"));byte[] bytes=service.storage().read(image.get("key").toString());s.setContentType(image.get("mime").toString());s.setHeader("Content-Disposition","inline; filename=\"document\"");s.setHeader("Cache-Control","private, no-store");s.setHeader("Referrer-Policy","no-referrer");s.setContentLength(bytes.length);s.getOutputStream().write(bytes);}
        else if(path.equals("/seller/verification")){data(r,service.own(user));view(r,s,"verification/own");}
        else if(path.equals("/admin/verifications")){r.setAttribute("verificationQueue",service.queue(user,value(r,"status"),integer(r,"page",1)));r.setAttribute("queuePage",integer(r,"page",1));view(r,s,"verification/queue");}
        else if(path.equals("/admin/verifications/detail")){r.setAttribute("submission",service.adminDetail(user,number(r,"id",0)));view(r,s,"verification/detail");}
        else s.sendError(405);
    }
    @Override protected void doPost(HttpServletRequest r,HttpServletResponse s)throws ServletException,IOException {
        var service=services(getServletContext()).verification();var user=SessionAuth.current(r);String path=r.getServletPath();
        try {
            if(path.equals("/seller/verification/upload")) {
                // Kiểm consent trước khi lưu bytes; không lấy đường dẫn hoặc key ảnh từ request.
                if(!"true".equals(value(r,"consent")))throw new ShopException(400,"Cần đồng ý lưu giấy tờ riêng tư để quản trị viên đối chiếu.");
                if(r.getContentType()==null||!r.getContentType().startsWith("multipart/"))throw new ShopException(400,"Hãy chọn ảnh giấy tờ và gửi bằng biểu mẫu tải ảnh.");
                StoredImage front=null,back=null;boolean committed=false;
                try {var parts=r.getParts();requireParts(parts);var f=r.getPart("front");var b=r.getPart("back");
                    if(f==null||f.getSize()==0||b==null||b.getSize()==0)throw new ShopException(400,"Cần đủ ảnh mặt trước và mặt sau căn cước.");
                    front=service.storage().save(f);back=service.storage().save(b);
                    long id=service.upload(user,front,back,true);committed=true;
                    r.setAttribute("qrMessage",service.read(user,id));
                } finally {if(!committed)service.storage().discard(front==null?List.of():back==null?List.of(front):List.of(front,back));}
                data(r,service.own(user));view(r,s,"verification/own");
            } else if(path.equals("/seller/verification/read")){r.setAttribute("qrMessage",service.read(user,number(r,"id",0)));var own=service.own(user);data(r,own);if(r.getParameter("fullName")!=null&&!"SUCCESS".equals(((Map<?,?>)own.get("submission")).get("qr_status")))r.setAttribute("posted",true);view(r,s,"verification/own");}
            else if(path.equals("/seller/verification/submit")){service.submit(user,number(r,"id",0),new VerificationForm(value(r,"fullName"),value(r,"idNumber"),value(r,"birthDate"),value(r,"gender"),value(r,"residence"),value(r,"issueDate")));redirect(r,s,"/seller/verification?notice=submitted");}
            else if(path.equals("/seller/verification/purge")){if(!"true".equals(value(r,"confirm")))throw new ShopException(400,"Hãy xác nhận xóa giấy tờ và quyền đăng tin mới.");service.purge(user);redirect(r,s,"/seller/verification?notice=purged");}
            else if(path.equals("/admin/verifications/claim")){long id=number(r,"id",0);service.claim(user,id);redirect(r,s,"/admin/verifications/detail?id="+id);}
            else if(path.equals("/admin/verifications/decide")){long id=number(r,"id",0);service.decide(user,id,value(r,"action"),value(r,"reason"));redirect(r,s,"/admin/verifications/detail?id="+id+"&notice=saved");}
            else s.sendError(405);
        }catch(ShopException e){if(path.startsWith("/seller/")&&Set.of(400,409,429).contains(e.status())){s.setStatus(e.status());r.setAttribute("formError",e.getMessage());r.setAttribute("posted",path.endsWith("/submit"));data(r,service.own(user));view(r,s,"verification/own");}else throw e;}
    }
    private void requireParts(Collection<Part> parts){var files=parts.stream().filter(p->p.getSubmittedFileName()!=null&&p.getSize()>0).toList();if(files.size()>2||files.stream().anyMatch(p->!Set.of("front","back").contains(p.getName()))||files.stream().map(Part::getName).distinct().count()!=files.size())throw new ShopException(400,"Chỉ tải một mặt trước và một mặt sau, tối đa 5 MB mỗi ảnh.");}
}
