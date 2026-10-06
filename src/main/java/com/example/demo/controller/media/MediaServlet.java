package com.example.demo.controller.media;

import com.example.demo.controller.*;
import com.example.demo.security.SessionAuth;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import static com.example.demo.controller.ShopWeb.*;

@WebServlet({"/media/product","/media/order-image","/media/complaint-evidence","/media/review-image"})
public final class MediaServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        var shop=services(getServletContext());
        Long order=r.getServletPath().endsWith("order-image")?number(r,"orderId",0):null;
        var metadata=r.getServletPath().endsWith("review-image")?shop.reviews().image(number(r,"reviewId",0),number(r,"asset",0)):r.getServletPath().endsWith("complaint-evidence")
                ?shop.complaints().evidence(SessionAuth.current(r),number(r,"complaintId",0),number(r,"asset",0))
                :shop.products().image(SessionAuth.current(r),number(r,"asset",0),order);
        byte[] bytes=shop.storage().read(metadata.get("storage_key").toString());
        s.setContentType(metadata.get("mime_type").toString()); s.setContentLength(bytes.length); s.getOutputStream().write(bytes);
    }
}
