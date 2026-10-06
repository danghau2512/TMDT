package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.dto.*;
import com.example.demo.exception.ShopException;
import com.example.demo.model.*;
import com.example.demo.storage.ImageStorage;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static com.example.demo.service.ShopRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** Chạy riêng sau các kiểm tra M1/M2 trên schema fixture *_test. */
class CommerceIT {
    static Database db; static ProductService products; static CartService carts; static OrderService orders;
    static final CurrentUser admin=new CurrentUser(500001,"Admin","ADMIN"), seller=new CurrentUser(500002,"Seller 1","USER"), seller2=new CurrentUser(500003,"Seller 2","USER"), buyer=new CurrentUser(500004,"Buyer 1","USER"), buyer2=new CurrentUser(500005,"Buyer 2","USER");
    @BeforeAll static void open() throws Exception {
        assertEquals("true",System.getenv("C2C_IT_ALLOWED")); var cfg=AppConfig.load(); db=new Database(cfg.database().orElseThrow());
        assertTrue(db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));
        products=new ProductService(db,new ImageStorage(cfg.uploadRoot())); carts=new CartService(db); orders=new OrderService(db);
    }
    @AfterAll static void close() { if(db!=null) db.close(); JdbcLifecycle.shutdown(); }
    @BeforeEach void emptyFixtureCarts() { for(var user:List.of(buyer,buyer2)) for(var p:carts.view(user).items()) carts.change(user,id(p,"id"),1,"REMOVE"); }
    static long product(CurrentUser owner,int stock) {
        long id=products.save(owner,null,new ProductForm("Sản phẩm thử "+UUID.randomUUID(),510001,"Mô tả tiếng Việt",new BigDecimal("100000"),"USED",stock,"PUBLIC",owner.id(),0,0,""),List.of(),false);
        products.action(admin,id,"APPROVE",1,"Duyệt fixture",true); return id;
    }
    static CheckoutForm form(CurrentUser user,String method) { return new CheckoutForm("Người nhận thử","0901234567","Địa chỉ thử nghiệm Hà Nội","Ghi chú thử",method,UUID.randomUUID().toString(),carts.view(user).quote()); }
    static int stock(long product) { return db.read(h->h.createQuery("SELECT stock_quantity FROM products WHERE id=:id").bind("id",product).mapTo(Integer.class).one()); }
    @Test void multivendorSnapshotsPaymentLifecycleAndIdempotentCancel() {
        long a=product(seller,3),b=product(seller2,3);
        assertEquals(400,assertThrows(ShopException.class,()->carts.change(seller,a,1,"ADD")).status());
        assertEquals(404,assertThrows(ShopException.class,()->products.detail(a,buyer,true)).status());
        carts.change(buyer,a,2,"ADD"); carts.change(buyer,b,1,"ADD"); var f=form(buyer,"COD");
        long batch=orders.checkout(buyer,f); assertEquals(batch,orders.checkout(buyer,f)); assertTrue(carts.view(buyer).items().isEmpty());
        var receipt=orders.receipt(buyer,batch); assertEquals(2,receipt.size()); assertEquals(1,stock(a)); assertEquals(2,stock(b));
        assertEquals(404,assertThrows(ShopException.class,()->orders.receipt(buyer2,batch)).status());
        for(var o:receipt) {
            long order=id(o,"id"); var detail=orders.detail(buyer,order,"buyer"); var owner=id((Map<String,Object>)detail.get("order"),"seller_id")==seller.id()?seller:seller2;
            orders.action(owner,order,"seller","CONFIRM",""); orders.action(owner,order,"seller","SHIP",""); orders.action(owner,order,"seller","DELIVER","");
            orders.action(buyer,order,"buyer","COMPLETE",""); orders.action(buyer,order,"buyer","COMPLETE","");
            assertEquals("COMPLETED",((Map<?,?>)orders.detail(buyer,order,"buyer").get("order")).get("status"));
        }
        var p=(Map<String,Object>)products.detail(a,seller,true).get("product");
        products.save(seller,a,new ProductForm("Tên đã đổi",510001,"Mô tả đã đổi",new BigDecimal("200000"),"NEW",stock(a),"PUBLIC",seller.id(),id(p,"listing_version"),id(p,"stock_version"),""),List.of(),false);
        assertEquals("PENDING",((Map<?,?>)products.detail(a,seller,true).get("product")).get("moderation_status"));
        assertEquals(new BigDecimal("100000.00"),db.read(h->h.createQuery("SELECT unit_price FROM order_items WHERE product_id=:id").bind("id",a).mapTo(BigDecimal.class).one()));
        carts.change(buyer,b,1,"ADD"); long cancelOrder=id(orders.receipt(buyer,orders.checkout(buyer,form(buyer,"COD"))).get(0),"id");
        // Import-like fixture of a historical simulated payment; new checkout no longer creates it.
        db.transaction(h->{h.createUpdate("UPDATE payments SET method='BANK_TRANSFER_SIMULATED',status='PENDING_CONFIRMATION' WHERE order_id=:id").bind("id",cancelOrder).execute();return null;});
        orders.action(buyer,cancelOrder,"buyer","PAY",""); orders.action(buyer,cancelOrder,"buyer","CANCEL","Thử hủy"); orders.action(buyer,cancelOrder,"buyer","CANCEL","Thử hủy");
        assertEquals(2,stock(b)); assertEquals("REFUND_SIMULATED",((Map<?,?>)orders.detail(buyer,cancelOrder,"buyer").get("payment")).get("status"));
        assertEquals(1,(int)db.read(h->h.createQuery("SELECT COUNT(*) FROM stock_movements sm JOIN order_items oi ON oi.id=sm.order_item_id WHERE oi.order_id=:id AND sm.movement_type='CANCEL_RELEASE'").bind("id",cancelOrder).mapTo(Integer.class).one()));
        carts.change(buyer,b,1,"ADD"); long cod=id(orders.receipt(buyer,orders.checkout(buyer,form(buyer,"COD"))).get(0),"id");
        orders.action(seller2,cod,"seller","CONFIRM",""); orders.action(seller2,cod,"seller","SHIP","");
        assertEquals("UNPAID",((Map<?,?>)orders.detail(buyer,cod,"buyer").get("payment")).get("status"));
        orders.action(seller2,cod,"seller","DELIVER",""); orders.action(buyer,cod,"buyer","COMPLETE","");
        assertEquals("PAID",((Map<?,?>)orders.detail(buyer,cod,"buyer").get("payment")).get("status"));
        carts.change(buyer,b,1,"ADD"); long unpaid=id(orders.receipt(buyer,orders.checkout(buyer,form(buyer,"COD"))).get(0),"id");
        orders.action(buyer,unpaid,"buyer","CANCEL","Thử hủy COD");
        assertEquals("VOIDED",((Map<?,?>)orders.detail(buyer,unpaid,"buyer").get("payment")).get("status"));
        assertEquals(409,assertThrows(ShopException.class,()->carts.change(buyer,product(seller,0),1,"ADD")).status());
    }
    @Test void lastUnitCompetitionAndMultisellerFailureAreAtomic() throws Exception {
        long a=product(seller,1),b=product(seller2,2); carts.change(buyer,a,1,"ADD"); carts.change(buyer,b,1,"ADD"); carts.change(buyer2,a,1,"ADD");
        var first=form(buyer,"COD"); var second=form(buyer2,"COD"); var start=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> one=()->{start.await();try{orders.checkout(buyer,first);return true;}catch(ShopException e){assertEquals(409,e.status());return false;}};
            Callable<Boolean> two=()->{start.await();try{orders.checkout(buyer2,second);return true;}catch(ShopException e){assertEquals(409,e.status());return false;}};
            var x=pool.submit(one); var y=pool.submit(two); start.countDown(); boolean won=x.get(20,TimeUnit.SECONDS),other=y.get(20,TimeUnit.SECONDS);
            assertNotEquals(won,other); assertEquals(0,stock(a)); assertEquals(won?1:2,stock(b));
            assertEquals(1,(int)db.read(h->h.createQuery("SELECT COUNT(*) FROM order_items WHERE product_id=:id").bind("id",a).mapTo(Integer.class).one()));
            CurrentUser losing=won?buyer2:buyer; assertFalse(carts.view(losing).items().isEmpty());
        } finally { pool.shutdownNow(); }
    }
    @Test void forcedFailureOnSecondSellerRollsBackBatchOrdersStockAndCart() {
        long a=product(seller,2),b=product(seller2,2); carts.change(buyer,a,1,"ADD");carts.change(buyer,b,1,"ADD"); var f=form(buyer,"COD");
        String trigger="c2c_test_fail_"+UUID.randomUUID().toString().replace("-","");
        // Tên trigger sinh nội bộ; không lấy identifier từ dữ liệu đầu vào.
        db.transaction(h->{h.execute("CREATE TRIGGER "+trigger+" BEFORE INSERT ON payments FOR EACH ROW BEGIN IF EXISTS (SELECT 1 FROM orders WHERE id=NEW.order_id AND seller_id=500003) THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Forced test rollback'; END IF; END");return null;});
        try {
            assertEquals(503,assertThrows(ShopException.class,()->orders.checkout(buyer,f)).status());
            assertEquals(2,stock(a));assertEquals(2,stock(b));assertEquals(2,carts.view(buyer).items().size());
            assertEquals(0,(int)db.read(h->h.createQuery("SELECT COUNT(*) FROM checkout_batches WHERE buyer_id=:buyer AND idempotency_key=:key").bind("buyer",buyer.id()).bind("key",f.key()).mapTo(Integer.class).one()));
            assertEquals(0,(int)db.read(h->h.createQuery("SELECT COUNT(*) FROM order_items WHERE product_id IN (:a,:b)").bind("a",a).bind("b",b).mapTo(Integer.class).one()));
            assertEquals(0,(int)db.read(h->h.createQuery("SELECT COUNT(*) FROM stock_movements WHERE product_id IN (:a,:b) AND movement_type='ORDER_HOLD'").bind("a",a).bind("b",b).mapTo(Integer.class).one()));
        } finally { db.transaction(h->{h.execute("DROP TRIGGER "+trigger);return null;}); }
        assertEquals(2,orders.receipt(buyer,orders.checkout(buyer,f)).size());
    }
}
