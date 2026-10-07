package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.dto.*;
import com.example.demo.model.*;
import com.example.demo.exception.ShopException;
import com.example.demo.storage.ImageStorage;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.lang.reflect.Proxy;
import static com.example.demo.service.ShopRules.*;
import static org.junit.jupiter.api.Assertions.*;

class ReputationIT {
    static Database db; static ShopServices shop;
    static final CurrentUser admin=new CurrentUser(500001,"Admin","ADMIN"),seller=new CurrentUser(500002,"Seller","USER"),buyer=new CurrentUser(500004,"Buyer","USER"),other=new CurrentUser(500005,"Other","USER");
    record Fixture(long product,long order,long item) { }
    @BeforeAll static void open() throws Exception {
        assertEquals("true",System.getenv("C2C_IT_ALLOWED")); var cfg=AppConfig.load();db=new Database(cfg.database().orElseThrow());
        assertTrue(db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test")); shop=new ShopServices(db,cfg.uploadRoot());
    }
    @AfterAll static void close() { if(db!=null)db.close();JdbcLifecycle.shutdown(); }
    static Fixture order() {
        for(var item:shop.cart().view(buyer).items()) shop.cart().change(buyer,id(item,"id"),1,"REMOVE");
        SellerTestProfiles.approve(db,seller);long product=shop.products().save(seller,null,new ProductForm("Tin đánh giá "+UUID.randomUUID(),510001,"Mô tả gốc để đối chiếu",new BigDecimal("123000"),"USED",3,"PUBLIC",seller.id(),0,0,""),List.of(),false);
        shop.products().action(admin,product,"APPROVE",1,"Duyệt fixture",true);shop.cart().change(buyer,product,1,"ADD");
        long batch=shop.orders().checkout(buyer,new CheckoutForm("Người nhận thử","0901234567","Địa chỉ thử tại Hà Nội","","COD",UUID.randomUUID().toString(),shop.cart().view(buyer).quote()));
        long order=id(shop.orders().receipt(buyer,batch).get(0),"id");long item=db.read(h->h.createQuery("SELECT id FROM order_items WHERE order_id=:id").bind("id",order).mapTo(Long.class).one());
        return new Fixture(product,order,item);
    }
    static void deliver(long order) { for(String action:List.of("CONFIRM","SHIP","DELIVER"))shop.orders().action(seller,order,"seller",action,""); }
    static int status(Executable call) { return assertThrows(ShopException.class,call::run).status(); }
    @FunctionalInterface interface Executable { void run(); }
    @Test void onlyBuyerOfCompletedItemCanReviewOnceAndPublicSummaryUsesThatReview() {
        var f=order();assertEquals(409,status(()->shop.reviews().create(buyer,f.order,f.item,4,"Chưa hoàn thành")));
        assertEquals(404,status(()->shop.reviews().create(other,f.order,f.item,4,"Đánh giá của người khác")));
        deliver(f.order);shop.orders().action(buyer,f.order,"buyer","COMPLETE","");
        assertEquals(400,status(()->shop.reviews().create(buyer,f.order,f.item,6,"Điểm sai")));
        assertEquals(404,status(()->shop.reviews().create(buyer,f.order,Long.MAX_VALUE,4,"Dòng đơn giả")));
        shop.reviews().create(buyer,f.order,f.item,4,"Nhận xét <script>demo</script>");
        assertEquals(409,status(()->shop.reviews().create(buyer,f.order,f.item,5,"Gửi lần hai")));
        var context=shop.reviews().form(buyer,f.order,f.item);assertFalse(((Map<?,?>)context.get("review")).isEmpty());
        assertEquals(404,status(()->shop.reviews().form(other,f.order,f.item)));
        var summary=(Map<?,?>)shop.products().detail(f.product,null,false).get("ratingSummary");
        assertEquals(1,((Number)summary.get("review_count")).intValue());assertEquals(4,((Number)summary.get("average_rating")).doubleValue());
    }
    @Test void complaintEvidenceOwnershipResolveReopenAndCompletionGuardKeepOrderAndPaymentIndependent() throws Exception {
        var f=order();var encoded=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",encoded);byte[] bytes=encoded.toByteArray();
        Part part=(Part)Proxy.newProxyInstance(Part.class.getClassLoader(),new Class<?>[]{Part.class},(proxy,method,args)->switch(method.getName()){case "getSize"->(long)bytes.length;case "getInputStream"->new ByteArrayInputStream(bytes);default->null;});
        var image=shop.storage().save(part);
        long complaint=shop.complaints().create(buyer,f.order,"NOT_AS_DESCRIBED","Nội dung khiếu nại có ảnh",List.of(image)).id();
        assertEquals(404,status(()->shop.complaints().create(other,f.order,"OTHER","Tạo thay người mua",List.of())));
        assertEquals(404,status(()->shop.complaints().supplement(other,complaint,"Bổ sung trái quyền",List.of())));
        assertEquals(complaint,shop.complaints().create(buyer,f.order,"OTHER","Gửi lại khiếu nại",List.of()).id());
        assertEquals(404,status(()->shop.complaints().detail(other,complaint,false)));
        assertEquals(403,status(()->shop.complaints().act(buyer,complaint,"PROCESS","Tự xử lý","","")));
        long asset=db.read(h->h.createQuery("SELECT asset_id FROM complaint_evidence WHERE complaint_id=:id").bind("id",complaint).mapTo(Long.class).one());
        assertFalse(shop.complaints().evidence(buyer,complaint,asset).isEmpty());assertFalse(shop.complaints().evidence(admin,complaint,asset).isEmpty());
        assertEquals(404,status(()->shop.complaints().evidence(seller,complaint,asset)));assertEquals(404,status(()->shop.products().image(null,asset,null)));
        shop.complaints().supplement(buyer,complaint,"Thông tin bổ sung",List.of());
        deliver(f.order);assertEquals(409,status(()->shop.orders().action(buyer,f.order,"buyer","COMPLETE","")));
        assertEquals(409,status(()->shop.complaints().act(admin,complaint,"RESOLVE","Kết thúc quá sớm","OTHER","Kết quả")));
        shop.complaints().act(admin,complaint,"PROCESS","Đang đối chiếu snapshot","","");
        assertEquals(400,status(()->shop.complaints().act(admin,complaint,"RESOLVE","Phản hồi","OTHER","")));
        shop.complaints().act(admin,complaint,"RESOLVE","Đã phản hồi người mua","SELLER_CONTACTED","Đã liên hệ và hướng dẫn");
        var detail=shop.orders().detail(buyer,f.order,"buyer");assertEquals("DELIVERED",((Map<?,?>)detail.get("order")).get("status"));assertEquals("PAID",((Map<?,?>)detail.get("payment")).get("status"));
        shop.complaints().act(admin,complaint,"REOPEN","Cần đối chiếu thêm","","");assertEquals(409,status(()->shop.orders().action(buyer,f.order,"buyer","COMPLETE","")));
        assertTrue(((List<Map<String,Object>>)shop.complaints().detail(admin,complaint,true).get("complaintHistory")).stream().anyMatch(e->"Đã liên hệ và hướng dẫn".equals(e.get("resolution_summary_snapshot"))));
        shop.complaints().act(admin,complaint,"RESOLVE","Đã đối chiếu xong","NO_ACTION","Không đổi giao dịch");shop.orders().action(buyer,f.order,"buyer","COMPLETE","");
        shop.complaints().act(admin,complaint,"REOPEN","Khiếu nại sau hoàn thành","","");
        assertEquals("COMPLETED",((Map<?,?>)shop.orders().detail(buyer,f.order,"buyer").get("order")).get("status"));
    }
    @Test void creatingComplaintAndCompletingOrderUseSameOrderLock() throws Exception {
        var f=order();deliver(f.order);var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            var complaint=pool.submit(()->{start.await();return shop.complaints().create(buyer,f.order,"OTHER","Kiểm tra giao dịch đồng thời",List.of()).id();});
            var completion=pool.submit(()->{start.await();try{shop.orders().action(buyer,f.order,"buyer","COMPLETE","");return true;}catch(ShopException e){assertEquals(409,e.status());return false;}});
            start.countDown();assertTrue(complaint.get(15,TimeUnit.SECONDS)>0);boolean complete=completion.get(15,TimeUnit.SECONDS);
            assertEquals(complete?"COMPLETED":"DELIVERED",((Map<?,?>)shop.orders().detail(buyer,f.order,"buyer").get("order")).get("status"));
            if(!complete) assertEquals(409,status(()->shop.orders().action(buyer,f.order,"buyer","COMPLETE","")));
            else shop.orders().action(buyer,f.order,"buyer","COMPLETE","");
        } finally { pool.shutdownNow(); }
    }
}
