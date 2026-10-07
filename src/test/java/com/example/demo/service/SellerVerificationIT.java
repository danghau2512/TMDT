package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.dao.SellerVerificationDao;
import com.example.demo.dto.ProductForm;
import com.example.demo.dto.VerificationForm;
import com.example.demo.verification.QrFixtures;
import com.example.demo.exception.ShopException;
import com.example.demo.model.*;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.*;
import java.util.*;
import java.math.BigDecimal;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.demo.service.ShopRules.*;

/** Dữ liệu và số giấy tờ hoàn toàn giả, chỉ chạy schema *_test với guard. */
class SellerVerificationIT {
    static Database db;static ShopServices shop;
    static final CurrentUser admin=new CurrentUser(500001,"Admin demo","ADMIN");static CurrentUser seller,other;
    @BeforeAll static void open()throws Exception {assertEquals("true",System.getenv("C2C_IT_ALLOWED"));var cfg=AppConfig.load();db=new Database(cfg.database().orElseThrow());assertTrue(db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));shop=new ShopServices(db,cfg.uploadRoot(),cfg.vnpay(),cfg.verificationConfig());seller=freshUser();other=freshUser();}
    static CurrentUser freshUser(){long id=db.transaction(h->h.createUpdate("INSERT INTO users(email,password_hash,display_name,role) SELECT :email,password_hash,'Người kiểm thử xác minh','USER' FROM users WHERE id=500001").bind("email","verification-"+UUID.randomUUID()+"@c2c.example").executeAndReturnGeneratedKeys("id").mapTo(Long.class).one());return new CurrentUser(id,"Người kiểm thử xác minh","USER");}
    @AfterAll static void close(){if(db!=null)db.close();JdbcLifecycle.shutdown();}
    static StoredImage image()throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"png",out);byte[] bytes=out.toByteArray();Part part=(Part)Proxy.newProxyInstance(Part.class.getClassLoader(),new Class<?>[]{Part.class},(p,m,a)->switch(m.getName()){case "getSize"->(long)bytes.length;case "getInputStream"->new ByteArrayInputStream(bytes);default->null;});return shop.verification().storage().save(part);}
    static void status(int status,org.junit.jupiter.api.function.Executable work){assertEquals(status,assertThrows(ShopException.class,work).status());}
    @Test void manualReviewControlsPublishingAndChangesRequireReview()throws Exception {
        var service=shop.verification();service.purge(seller);assertFalse(service.approved(seller));
        var form=new ProductForm("Tin fixture xác minh "+UUID.randomUUID(),510001,"Mô tả fixture, không có giấy tờ thật",new BigDecimal("123000"),"USED",5,"PUBLIC",seller.id(),0,0,"",null);
        status(403,()->shop.products().save(seller,null,form,List.of(),false));
        long draft=service.upload(seller,image(),image(),true);assertTrue(service.read(seller,draft).contains("Không đọc được QR"));assertFalse(service.approved(seller));
        status(404,()->service.image(other,draft,"front"));status(404,()->service.image(admin,draft,"front"));
        service.submit(seller,draft,manual("HỌ TÊN GIẢ DEMO","000000000002"));status(403,()->service.decide(admin,draft,"APPROVE",""));service.claim(admin,draft);assertNotNull(service.image(admin,draft,"front"));service.decide(admin,draft,"APPROVE","");assertTrue(service.approved(seller));
        long product=shop.products().save(seller,null,form,List.of(),false);assertEquals("PENDING",((Map<?,?>)shop.products().detail(product,seller,true).get("product")).get("moderation_status"));shop.products().action(admin,product,"APPROVE",1,"Duyệt fixture",true);
        long changed=service.upload(seller,image(),image(),true);assertFalse(service.approved(seller));assertNotNull(shop.products().detail(product,null,false));
        status(403,()->shop.products().save(seller,null,form,List.of(),false));service.submit(seller,changed,manual("HỌ TÊN GIẢ DEMO","000000000002"));service.claim(admin,changed);status(400,()->service.decide(admin,changed,"REJECT",""));service.decide(admin,changed,"REJECT","Ảnh fixture chưa rõ, hãy bổ sung.");assertEquals("REJECTED",service.own(seller).get("verificationStatus"));
        long retry=service.upload(seller,image(),image(),true);service.submit(seller,retry,manual("HỌ TÊN GIẢ DEMO","000000000002"));service.claim(admin,retry);service.decide(admin,retry,"APPROVE","");assertTrue(service.approved(seller));
        var metadata=service.image(seller,retry,"front");service.purge(seller);assertFalse(service.approved(seller));status(404,()->service.image(seller,retry,"front"));status(404,()->service.storage().read(metadata.get("key").toString()));
        assertEquals(0,db.<Integer,RuntimeException>read(h->h.createQuery("SELECT COUNT(*) FROM seller_verification_submissions WHERE user_id=:u AND (id_number IS NOT NULL OR ocr_number IS NOT NULL OR front_key IS NOT NULL)").bind("u",seller.id()).mapTo(Integer.class).one()).intValue());assertNotNull(shop.products().detail(product,null,false));
    }
    static VerificationForm manual(String name,String number){return new VerificationForm(name,number,"01/01/2001","Nam","Địa chỉ giả cho kiểm thử","01/01/2021");}
    static StoredImage qrImage(String text)throws Exception {byte[] bytes=QrFixtures.qr(text);Part part=(Part)Proxy.newProxyInstance(Part.class.getClassLoader(),new Class<?>[]{Part.class},(p,m,a)->switch(m.getName()){case "getSize"->(long)bytes.length;case "getInputStream"->new ByteArrayInputStream(bytes);default->null;});return shop.verification().storage().save(part);}
    @Test void qrFieldsEditedAuditAndConflictAreEnforced()throws Exception {
        var service=shop.verification();long draft=service.upload(other,image(),qrImage(QrFixtures.A),true);assertTrue(service.read(other,draft).contains("Đã đọc thông tin"));assertFalse(service.approved(other));
        status(404,()->service.submit(seller,draft,manual("GIẢ DEMO","000000000003")));status(400,()->service.submit(other,draft,manual("GIẢ DEMO","bad")));
        var form=new VerificationForm("TÊN ĐÃ SỬA DEMO","000000000021","29/02/2000","Nữ","Nơi cư trú đã sửa để thử","01/01/2021");service.submit(other,draft,form);service.claim(admin,draft);var detail=service.adminDetail(admin,draft);assertEquals("Đọc từ QR",detail.get("sourceLabel"));var fields=(List<Map<String,Object>>)detail.get("fields");assertEquals(2,fields.stream().filter(x->Boolean.TRUE.equals(x.get("changed"))).count());
        assertEquals(java.time.LocalDate.of(2000,2,29),db.read(h->h.createQuery("SELECT birth_date FROM seller_verification_submissions WHERE id=:id").bind("id",draft).mapTo(java.time.LocalDate.class).one()));
        service.purge(other);long conflict=service.upload(other,qrImage(QrFixtures.A),qrImage(QrFixtures.B),true);assertTrue(service.read(other,conflict).contains("mâu thuẫn"));status(409,()->service.submit(other,conflict,manual("GIẢ DEMO","000000000003")));service.purge(other);
    }
    @Test void oldOcrProfileRetainsSourceAndCanBeReviewed()throws Exception {
        var user=freshUser();var front=image();long legacy=db.transaction(h->{var dao=new SellerVerificationDao(h);dao.ensure(user.id());long id=h.createUpdate("INSERT INTO seller_verification_submissions(user_id,status,full_name,id_number,ocr_status,ocr_name,ocr_number,front_key,front_mime,data_source,submitted_at) VALUES(:u,'PENDING','TÊN GIẢ HỒ SƠ CŨ','000000000088','SUCCESS','TÊN GIẢ CŨ','000000000088',:key,:mime,'MANUAL_LEGACY',UTC_TIMESTAMP(6))").bind("u",user.id()).bind("key",front.key()).bind("mime",front.mime()).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();dao.current(user.id(),id,"PENDING");return id;});shop.verification().claim(admin,legacy);var detail=shop.verification().adminDetail(admin,legacy);assertEquals("OCR (hồ sơ cũ)",detail.get("sourceLabel"));assertEquals(true,detail.get("legacy"));shop.verification().decide(admin,legacy,"APPROVE","");assertTrue(shop.verification().approved(user));shop.verification().purge(user);
    }
    @Test void databaseRejectsNullReasonAndMissingPendingFields(){assertThrows(RuntimeException.class,()->db.transaction(h->{h.createUpdate("INSERT INTO seller_verification_submissions(user_id,status,reviewer_id,reviewed_at) VALUES(:u,'REJECTED',:a,UTC_TIMESTAMP(6))").bind("u",seller.id()).bind("a",admin.id()).execute();return null;}));assertThrows(RuntimeException.class,()->db.transaction(h->{h.createUpdate("INSERT INTO seller_verification_submissions(user_id,status) VALUES(:u,'PENDING')").bind("u",seller.id()).execute();return null;}));}
}
