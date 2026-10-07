package com.example.demo.controller;

import com.example.demo.config.*;
import com.example.demo.dto.*;
import com.example.demo.model.*;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.util.*;
import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static com.example.demo.service.ShopRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** Một hành trình HTTP M7/M8, dùng lại Browser/guard M3–M6. */
class FeedbackHttpIT {
    static Database db; static ShopServices shop;
    static final CurrentUser admin=new CurrentUser(500001,"Admin","ADMIN"),seller=new CurrentUser(500002,"Seller","USER"),buyer=new CurrentUser(500004,"Buyer","USER");
    @BeforeAll static void open() throws Exception {
        String base=System.getenv("C2C_HTTP_BASE");Assumptions.assumeTrue(base!=null && !base.isBlank());
        assertEquals("true",System.getenv("C2C_IT_ALLOWED"));var uri=URI.create(base);assertEquals("http",uri.getScheme());assertTrue(Set.of("127.0.0.1","localhost").contains(uri.getHost()));
        assertNull(uri.getUserInfo());assertNull(uri.getQuery());assertNull(uri.getFragment());
        var config=AppConfig.load();db=new Database(config.database().orElseThrow());assertTrue(db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));
        HttpTestTarget.verify(db,base);ShopHttpIT.base=base;shop=new ShopServices(db,config.uploadRoot());com.example.demo.service.SellerTestProfiles.approve(db,seller);
    }
    @AfterAll static void close() { if(db!=null)db.close();JdbcLifecycle.shutdown(); }
    static long checkout(long product) {
        for(var p:shop.cart().view(buyer).items())shop.cart().change(buyer,id(p,"id"),1,"REMOVE");shop.cart().change(buyer,product,1,"ADD");
        long batch=shop.orders().checkout(buyer,new CheckoutForm("Người nhận thử","0901234567","Địa chỉ thử tại Hà Nội","","COD",UUID.randomUUID().toString(),shop.cart().view(buyer).quote()));
        return id(shop.orders().receipt(buyer,batch).get(0),"id");
    }
    static void deliver(long order) { for(String action:List.of("CONFIRM","SHIP","DELIVER"))shop.orders().action(seller,order,"seller",action,""); }
    @Test void verifiedReviewAndPrivateComplaintWithSnapshotResponseAndCompletionGuardWorkOverHttp() throws Exception {
        var user=new ShopHttpIT.Browser();user.login("buyer1");var staff=new ShopHttpIT.Browser();staff.login("admin");var other=new ShopHttpIT.Browser();other.login("buyer2");var merchant=new ShopHttpIT.Browser();merchant.login("seller1");
        String title="Tin hỗ trợ HTTP "+UUID.randomUUID(),description="Mô tả snapshot gốc tiếng Việt";
        long product=shop.products().save(seller,null,new ProductForm(title,510001,description,new BigDecimal("123000"),"USED",5,"PUBLIC",seller.id(),0,0,""),List.of(),false);shop.products().action(admin,product,"APPROVE",1,"Duyệt fixture",true);
        long complete=checkout(product);deliver(complete);shop.orders().action(buyer,complete,"buyer","COMPLETE","");
        long item=db.read(h->h.createQuery("SELECT id FROM order_items WHERE order_id=:order").bind("order",complete).mapTo(Long.class).one());String form="/buyer/reviews/new?orderId="+complete+"&itemId="+item;
        assertEquals(404,other.get(form).statusCode());
        var review=new HashMap<>(Map.of("csrfToken",user.token(form),"orderId",""+complete,"itemId",""+item,"rating","0","comment","Rất tốt <script>demo</script>"));
        assertEquals(400,user.post("/buyer/reviews/create",review).statusCode());review.put("rating","5");assertEquals(303,user.post("/buyer/reviews/create",review).statusCode());
        assertEquals(409,user.post("/buyer/reviews/create",review).statusCode());var rated=user.get("/buyer/orders/detail?id="+complete);assertEquals(200,rated.statusCode());assertTrue(rated.body().contains("Đánh giá đã gửi"));
        var catalog=new ShopHttpIT.Browser().get("/products/detail?id="+product);assertEquals(200,catalog.statusCode());assertTrue(catalog.body().contains("Đã mua qua hệ thống"));assertTrue(catalog.body().contains("Rất tốt &lt;script&gt;demo&lt;/script&gt;"));
        long pending=checkout(product);var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(3,3,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var create=Map.of("csrfToken",user.token("/buyer/complaints/new?orderId="+pending),"orderId",""+pending,"reason","NOT_RECEIVED","content","Chưa nhận được hàng <script>demo</script>");
        assertEquals(400,user.multipart("/buyer/complaints/create",create,"không phải PNG".getBytes(java.nio.charset.StandardCharsets.UTF_8)).statusCode());
        var submitted=user.multipart("/buyer/complaints/create",create,bytes.toByteArray());assertEquals(303,submitted.statusCode());
        assertEquals(submitted.headers().firstValue("Location"),user.multipart("/buyer/complaints/create",create,bytes.toByteArray()).headers().firstValue("Location"));
        long complaint=db.read(h->h.createQuery("SELECT id FROM complaints WHERE order_id=:order").bind("order",pending).mapTo(Long.class).one());String buyerDetail="/buyer/complaints/detail?id="+complaint,adminDetail="/admin/complaints/detail?id="+complaint;
        assertEquals(303,user.get("/buyer/complaints/new?orderId="+pending).statusCode());assertEquals(200,user.get(buyerDetail).statusCode());assertEquals(404,other.get(buyerDetail).statusCode());assertEquals(403,merchant.get(adminDetail).statusCode());
        long asset=db.read(h->h.createQuery("SELECT asset_id FROM complaint_evidence WHERE complaint_id=:id").bind("id",complaint).mapTo(Long.class).one());String evidence="/media/complaint-evidence?complaintId="+complaint+"&asset="+asset;
        assertEquals(200,user.get(evidence).statusCode());assertEquals(200,staff.get(evidence).statusCode());assertEquals(404,merchant.get(evidence).statusCode());assertEquals(404,other.get(evidence).statusCode());assertEquals(401,new ShopHttpIT.Browser().get(evidence).statusCode());assertEquals(404,new ShopHttpIT.Browser().get("/media/product?asset="+asset).statusCode());
        var current=(Map<String,Object>)shop.products().detail(product,seller,true).get("product");
        shop.products().save(seller,product,new ProductForm("Tên tin đã sửa",510001,"Mô tả hiện tại đã đổi",new BigDecimal("200000"),"NEW",number(current,"stock_quantity"),"PUBLIC",seller.id(),id(current,"listing_version"),id(current,"stock_version"),""),List.of(),false);
        assertTrue(staff.get(adminDetail).body().contains(description));assertFalse(staff.get(adminDetail).body().contains("Mô tả hiện tại đã đổi"));assertFalse(user.get(buyerDetail).body().contains("<script>demo</script>"));
        assertEquals(303,user.multipart("/buyer/complaints/supplement",Map.of("csrfToken",user.token(buyerDetail),"id",""+complaint,"content","Thông tin bổ sung của người mua"),bytes.toByteArray()).statusCode());
        var action=new HashMap<>(Map.of("csrfToken",staff.token(adminDetail),"id",""+complaint,"action","PROCESS","response","Admin đang đối chiếu snapshot"));assertEquals(303,staff.post("/admin/complaints/action",action).statusCode());
        deliver(pending);String orderDetail="/buyer/orders/detail?id="+pending;assertTrue(user.get(orderDetail).body().contains("Đơn đang có khiếu nại chưa xử lý xong"));
        assertEquals(409,user.post("/buyer/orders/action",Map.of("csrfToken",user.token(orderDetail),"id",""+pending,"action","COMPLETE")).statusCode());
        action.put("action","RESOLVE");action.put("resolution","OTHER");action.put("summary","");action.put("response","Admin phản hồi <script>demo</script>");assertEquals(400,staff.post("/admin/complaints/action",action).statusCode());
        action.put("summary","Đã đối chiếu và hướng dẫn người mua");assertEquals(303,staff.post("/admin/complaints/action",action).statusCode());assertTrue(user.get(buyerDetail).body().contains("Đã đối chiếu và hướng dẫn người mua"));assertTrue(user.get(buyerDetail).body().contains("&lt;script&gt;demo&lt;/script&gt;"));
        assertEquals("DELIVERED",((Map<?,?>)shop.orders().detail(buyer,pending,"buyer").get("order")).get("status"));user.action("buyer",pending,"COMPLETE");
        assertEquals(303,staff.post("/admin/complaints/action",Map.of("csrfToken",staff.token(adminDetail),"id",""+complaint,"action","REOPEN","response","Mở lại sau khi đơn hoàn thành")).statusCode());assertEquals("COMPLETED",((Map<?,?>)shop.orders().detail(buyer,pending,"buyer").get("order")).get("status"));
        assertEquals(200,user.get("/buyer/complaints").statusCode());assertEquals(200,staff.get("/admin/complaints?status=PROCESSING").statusCode());
    }
}
