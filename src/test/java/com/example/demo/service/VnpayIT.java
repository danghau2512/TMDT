package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.dao.VnpayDao;
import com.example.demo.dto.*;
import com.example.demo.exception.ShopException;
import com.example.demo.model.CurrentUser;
import com.example.demo.payment.*;
import com.example.demo.storage.ImageStorage;
import java.math.BigDecimal;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import static com.example.demo.service.ShopRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** Signed fixtures exercise transactions on isolated MySQL; they are not real gateway confirmations. */
class VnpayIT {
    static Database db;static CartService cart;static OrderService orders;static ProductService products;static VnpayService pay;
    static final CurrentUser admin=new CurrentUser(500001,"Admin","ADMIN"),seller=new CurrentUser(500002,"Seller","USER"),seller2=new CurrentUser(500003,"Seller 2","USER"),buyer=new CurrentUser(500004,"Buyer","USER"),other=new CurrentUser(500005,"Other","USER");
    static final AtomicLong txn=new AtomicLong(System.currentTimeMillis());
    static final VnpayConfig cfg=new VnpayConfig("TEST0001","test-secret-not-a-credential","https://sandbox.vnpayment.vn/paymentv2/vpcpay.html","https://sandbox.vnpayment.vn/merchant_webapi/api/transaction","http://localhost:18080/c2c/payments/vnpay/return","http://localhost:18080/c2c/payments/vnpay/ipn",15);
    @BeforeAll static void open() throws Exception {
        assertEquals("true",System.getenv("C2C_IT_ALLOWED"));var app=AppConfig.load();db=new Database(app.database().orElseThrow());
        assertTrue(db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));
        cart=new CartService(db);orders=new OrderService(db);products=new ProductService(db,new ImageStorage(app.uploadRoot()));pay=new VnpayService(db,Optional.of(cfg));
    }
    @AfterAll static void close(){if(db!=null)db.close();JdbcLifecycle.shutdown();}
    @BeforeEach void clearCart(){for(var p:cart.view(buyer).items())cart.change(buyer,id(p,"id"),1,"REMOVE");}
    static long product(CurrentUser owner){long p=products.save(owner,null,new ProductForm("VNPAY fixture "+UUID.randomUUID(),510001,"Fixture gateway",new BigDecimal("125000"),"USED",5,"PUBLIC",owner.id(),0,0,""),List.of(),false);products.action(admin,p,"APPROVE",1,"Fixture",true);return p;}
    static CheckoutForm form(String method){return new CheckoutForm("Người nhận thử","0901234567","Địa chỉ thử nghiệm Hà Nội","",method,UUID.randomUUID().toString(),cart.view(buyer).quote());}
    static long order(){cart.change(buyer,product(seller),1,"ADD");return id(orders.receipt(buyer,orders.checkout(buyer,form("VNPAY_SANDBOX"))).get(0),"id");}
    static String ref(String url){for(String part:URI.create(url).getRawQuery().split("&"))if(part.startsWith("vnp_TxnRef="))return URLDecoder.decode(part.substring(11),java.nio.charset.StandardCharsets.US_ASCII);throw new AssertionError();}
    static Map<String,String> result(String ref,String response,String status){
        var a=db.read(h->new VnpayDao(h).byRef(ref,false).orElseThrow());
        return new HashMap<>(Map.of("vnp_TmnCode",cfg.merchant(),"vnp_TxnRef",ref,"vnp_Amount",VnpayProtocol.amount(money(a,"expected_amount")),"vnp_ResponseCode",response,"vnp_TransactionStatus",status,"vnp_BankCode","NCB","vnp_PayDate",VnpayProtocol.date(Instant.now()),"vnp_TransactionNo",""+txn.incrementAndGet()));
    }
    static Map<String,String> sign(Map<String,String> f){f.put("vnp_SecureHash",VnpayProtocol.hmac(cfg.secret(),VnpayProtocol.canonical(f)));return f;}
    static String status(long order){return db.read(h->h.createQuery("SELECT status FROM payments WHERE order_id=:id").bind("id",order).mapTo(String.class).one());}
    static int ledger(long order,String type){return db.read(h->h.createQuery("SELECT COUNT(*) FROM stock_movements sm JOIN order_items oi ON oi.id=sm.order_item_id WHERE oi.order_id=:id AND sm.movement_type=:t").bind("id",order).bind("t",type).mapTo(Integer.class).one());}
    @Test void multisellerAmountsOwnershipAndConcurrentClickReuse() throws Exception {
        cart.change(buyer,product(seller),2,"ADD");cart.change(buyer,product(seller2),1,"ADD");var receipt=orders.receipt(buyer,orders.checkout(buyer,form("VNPAY_SANDBOX")));assertEquals(2,receipt.size());
        var references=new HashSet<String>();
        for(var o:receipt){long id=id(o,"id");var pool=Executors.newFixedThreadPool(2);try{
            var a=pool.submit(()->pay.create(buyer,id,"127.0.0.1"));var b=pool.submit(()->pay.create(buyer,id,"127.0.0.1"));String url=a.get(10,TimeUnit.SECONDS);assertEquals(url,b.get(10,TimeUnit.SECONDS));
            assertTrue(url.contains("vnp_Amount="+VnpayProtocol.amount(money(o,"grand_total"))));assertFalse(url.contains("vnp_SecureHashType"));assertTrue(references.add(ref(url)));
            assertEquals(1,ledger(id,"ORDER_HOLD"));assertEquals(404,assertThrows(ShopException.class,()->pay.create(other,id,"127.0.0.1")).status());
        }finally{pool.shutdownNow();}}
        assertEquals(400,assertThrows(ShopException.class,()->orders.checkout(buyer,form("BANK_TRANSFER_SIMULATED"))).status());
        long large=products.save(seller,null,new ProductForm("VNPAY amount limit "+UUID.randomUUID(),510001,"Fixture amount",new BigDecimal("10000000000"),"USED",1,"PUBLIC",seller.id(),0,0,""),List.of(),false);
        products.action(admin,large,"APPROVE",1,"Fixture",true);cart.change(buyer,large,1,"ADD");
        assertEquals(400,assertThrows(ShopException.class,()->orders.checkout(buyer,form("VNPAY_SANDBOX"))).status());assertEquals(1,cart.view(buyer).items().size());
        assertEquals(0,(int)db.read(h->h.createQuery("SELECT COUNT(*) FROM order_items WHERE product_id=:id").bind("id",large).mapTo(Integer.class).one()));
    }
    @Test void returnCannotPayAndInvalidCallbacksDoNotMutateThenDuplicateSuccessIsAtomic(){
        long o=order();String r=ref(pay.create(buyer,o,"127.0.0.1"));var good=sign(result(r,"00","00"));pay.validateReturn(good);assertEquals("PENDING_CONFIRMATION",status(o));
        var bad=new HashMap<>(good);bad.put("vnp_SecureHash","0".repeat(128));assertEquals("97",pay.ipn(bad).RspCode());
        bad=new HashMap<>(good);bad.put("vnp_Amount","1");assertEquals("04",pay.ipn(sign(bad)).RspCode());assertEquals("PENDING_CONFIRMATION",status(o));
        assertEquals("00",pay.ipn(good).RspCode());assertEquals("02",pay.ipn(good).RspCode());assertEquals("02",pay.ipn(sign(result(r,"24","02"))).RspCode());assertEquals("PAID",status(o));
        assertEquals(1,(int)db.read(h->h.createQuery("SELECT COUNT(*) FROM payment_status_history ph JOIN payments p ON p.id=ph.payment_id WHERE p.order_id=:o AND ph.to_status='PAID'").bind("o",o).mapTo(Integer.class).one()));
        assertEquals(1,ledger(o,"ORDER_HOLD"));assertEquals(409,assertThrows(ShopException.class,()->pay.create(buyer,o,"127.0.0.1")).status());
    }
    @Test void failedOrExpiredAttemptCanRetryAndOldSuccessSupersedesPendingAttempt(){
        long o=order();String first=ref(pay.create(buyer,o,"127.0.0.1"));assertEquals("00",pay.ipn(sign(result(first,"24","11"))).RspCode());
        String second=ref(pay.create(buyer,o,"127.0.0.1"));assertNotEquals(first,second);
        db.transaction(h->{h.createUpdate("UPDATE vnpay_attempts SET expires_at=UTC_TIMESTAMP()-INTERVAL 1 MINUTE WHERE txn_ref=:r").bind("r",second).execute();return null;});
        String third=ref(pay.create(buyer,o,"127.0.0.1"));assertNotEquals(second,third);
        assertEquals("00",pay.ipn(sign(result(first,"00","00"))).RspCode());assertEquals("PAID",status(o));
        assertEquals("SUPERSEDED",((Map<?,?>)pay.view(buyer,third).get("attempt")).get("status"));
        assertEquals("00",pay.ipn(sign(result(second,"24","02"))).RspCode());assertEquals("PAID",status(o));
    }
    @Test void paidCancellationAndLateSuccessNeedRefundReviewWithoutRestockingTwice(){
        long paid=order();String r=ref(pay.create(buyer,paid,"127.0.0.1"));assertEquals("00",pay.ipn(sign(result(r,"00","00"))).RspCode());
        orders.action(buyer,paid,"buyer","CANCEL","Fixture paid cancellation");assertEquals("REFUND_PENDING",status(paid));assertEquals(1,ledger(paid,"CANCEL_RELEASE"));
        long cancelled=order();String late=ref(pay.create(buyer,cancelled,"127.0.0.1"));orders.action(buyer,cancelled,"buyer","CANCEL","Fixture late result");
        assertEquals(409,assertThrows(ShopException.class,()->pay.create(buyer,cancelled,"127.0.0.1")).status());var success=sign(result(late,"00","00"));
        assertEquals("00",pay.ipn(success).RspCode());assertEquals("02",pay.ipn(success).RspCode());assertEquals("REFUND_PENDING",status(cancelled));
        assertEquals("NEEDS_REVIEW",((Map<?,?>)pay.view(buyer,late).get("attempt")).get("status"));assertEquals(1,ledger(cancelled,"CANCEL_RELEASE"));
        assertEquals("CANCELLED",((Map<?,?>)orders.detail(buyer,cancelled,"buyer").get("order")).get("status"));
    }
    @Test void querydrRequiresSignedMatchingPaymentStatusAndSharesIdempotentApplication(){
        long o=order();String r=ref(pay.create(buyer,o,"127.0.0.1"));var response=new HashMap<>(result(r,"00","01"));response.putAll(Map.of("vnp_ResponseId","fixture","vnp_Command","querydr","vnp_Message","Success","vnp_TransactionType","01","vnp_OrderInfo","Fixture query"));
        VnpayGateway stub=request->{assertEquals("querydr",request.get("vnp_Command"));response.put("vnp_SecureHash",VnpayProtocol.hmac(cfg.secret(),VnpayProtocol.queryData(response,true)));return new HashMap<>(response);};
        var query=new VnpayService(db,Optional.of(cfg),stub);query.query(buyer,r,"127.0.0.1");assertEquals("PENDING_CONFIRMATION",status(o));
        assertEquals(429,assertThrows(ShopException.class,()->query.query(buyer,r,"127.0.0.1")).status());assertEquals(404,assertThrows(ShopException.class,()->query.query(other,r,"127.0.0.1")).status());
        db.transaction(h->{h.createUpdate("UPDATE vnpay_attempts SET last_query_at=NULL WHERE txn_ref=:r").bind("r",r).execute();return null;});
        var apiError=new VnpayService(db,Optional.of(cfg),request->Map.of("vnp_ResponseCode","94","vnp_Message","untrusted message"));
        assertTrue(apiError.query(buyer,r,"127.0.0.1").contains("94"));assertEquals("PENDING_CONFIRMATION",status(o));
        db.transaction(h->{h.createUpdate("UPDATE vnpay_attempts SET last_query_at=NULL WHERE txn_ref=:r").bind("r",r).execute();return null;});
        String expectedAmount=response.get("vnp_Amount");response.put("vnp_Amount","1");assertEquals(502,assertThrows(ShopException.class,()->query.query(buyer,r,"127.0.0.1")).status());assertEquals("PENDING_CONFIRMATION",status(o));response.put("vnp_Amount",expectedAmount);
        db.transaction(h->{h.createUpdate("UPDATE vnpay_attempts SET last_query_at=NULL WHERE txn_ref=:r").bind("r",r).execute();return null;});response.put("vnp_TransactionStatus","00");
        var wrong=new VnpayService(db,Optional.of(cfg),request->{var f=new HashMap<>(response);f.put("vnp_SecureHash","0".repeat(128));return f;});assertEquals(502,assertThrows(ShopException.class,()->wrong.query(buyer,r,"127.0.0.1")).status());assertEquals("PENDING_CONFIRMATION",status(o));
        db.transaction(h->{h.createUpdate("UPDATE vnpay_attempts SET last_query_at=NULL WHERE txn_ref=:r").bind("r",r).execute();return null;});query.query(buyer,r,"127.0.0.1");assertEquals("PAID",status(o));assertEquals("02",pay.ipn(sign(response)).RspCode());
    }
}

