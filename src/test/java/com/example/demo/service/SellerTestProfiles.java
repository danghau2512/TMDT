package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.model.*;
import com.example.demo.dto.VerificationForm;
import jakarta.servlet.http.Part;
import java.io.*;
import java.lang.reflect.Proxy;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Điều kiện fixture của test mua bán cũ: giấy tờ giả, duyệt bằng Service, không dùng trên DB ứng dụng. */
public final class SellerTestProfiles {
    private SellerTestProfiles() { }
    public static void approve(Database db,CurrentUser seller){
        if(!"true".equals(System.getenv("C2C_IT_ALLOWED"))||!db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"))throw new IllegalStateException("Test fixture guard");
        try {var cfg=AppConfig.load();var service=new SellerVerificationService(db,cfg.verificationConfig());if(service.approved(seller))return;
            service.purge(seller);var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"png",out);byte[] bytes=out.toByteArray();Part part=(Part)Proxy.newProxyInstance(Part.class.getClassLoader(),new Class<?>[]{Part.class},(p,m,a)->switch(m.getName()){case "getSize"->(long)bytes.length;case "getInputStream"->new ByteArrayInputStream(bytes);default->null;});
            long id=service.upload(seller,service.storage().save(part),service.storage().save(part),true);service.submit(seller,id,new VerificationForm("HỌ TÊN GIẢ CHO TEST","000000000099","01/01/2001","Nam","Nơi cư trú giả để kiểm thử","01/01/2021"));var admin=new CurrentUser(500001,"Admin fixture","ADMIN");service.claim(admin,id);service.decide(admin,id,"APPROVE","");
        }catch(Exception e){throw new IllegalStateException("Không chuẩn bị được hồ sơ giả cho test; kiểm tra schema/fixture riêng.");}
    }
}
