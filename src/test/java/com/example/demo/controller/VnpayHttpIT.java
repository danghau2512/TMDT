package com.example.demo.controller;

import com.example.demo.config.*;
import com.example.demo.dto.*;
import com.example.demo.model.CurrentUser;
import com.example.demo.payment.VnpayProtocol;
import com.example.demo.service.*;
import com.example.demo.storage.ImageStorage;
import java.math.BigDecimal;
import java.net.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.demo.service.ShopRules.id;

class VnpayHttpIT {
    static AppConfig app;
    @BeforeAll static void open() throws Exception {ShopHttpIT.open();app=AppConfig.load();assertTrue(app.vnpay().isPresent());}
    @AfterAll static void close(){ShopHttpIT.close();}
    static Map<String,String> decode(String url){var fields=new HashMap<String,String>();for(String pair:URI.create(url).getRawQuery().split("&")){String[] p=pair.split("=",2);fields.put(p[0],URLDecoder.decode(p[1],java.nio.charset.StandardCharsets.US_ASCII));}return fields;}
    static String encode(Map<String,String> f){return f.entrySet().stream().map(e->e.getKey()+"="+URLEncoder.encode(e.getValue(),java.nio.charset.StandardCharsets.US_ASCII)).collect(java.util.stream.Collectors.joining("&"));}
    @Test void exactIpnIsPublicButOtherPaymentEndpointsRequireLoginAndCsrf() throws Exception {
        var guest=new ShopHttpIT.Browser();
        var ipn=guest.get("/payments/vnpay/ipn?vnp_SecureHash=bad");assertEquals(200,ipn.statusCode());assertTrue(ipn.body().contains("\"RspCode\":\"97\""));
        assertTrue(ipn.headers().allValues("Set-Cookie").isEmpty());assertEquals(405,guest.post("/payments/vnpay/ipn",Map.of()).statusCode());
        assertEquals(303,guest.get("/payments/vnpay/result?ref="+"a".repeat(32)).statusCode());assertEquals(303,guest.post("/payments/vnpay/create",Map.of("orderId","1")).statusCode());
        var b=new ShopHttpIT.Browser();b.login("buyer1");assertEquals(403,b.post("/payments/vnpay/create",Map.of("orderId","1")).statusCode());
        var fields=new HashMap<>(Map.of("vnp_TmnCode",app.vnpay().orElseThrow().merchant(),"vnp_TxnRef","a".repeat(32),"vnp_Amount","10000","vnp_ResponseCode","00","vnp_TransactionStatus","00"));
        fields.put("vnp_SecureHash",VnpayProtocol.hmac(app.vnpay().orElseThrow().secret(),VnpayProtocol.canonical(fields)));assertTrue(guest.get("/payments/vnpay/ipn?"+encode(fields)).body().contains("\"RspCode\":\"01\""));
    }
    @Test void checkoutReceiptPerOrderRedirectAndSignedReturnRemainUnpaid() throws Exception {
        var db=ShopHttpIT.db;var buyer=new CurrentUser(500004,"Buyer","USER");var seller=new CurrentUser(500002,"Seller","USER");var admin=new CurrentUser(500001,"Admin","ADMIN");
        var products=new ProductService(db,new ImageStorage(app.uploadRoot()));var cart=new CartService(db);var orders=new OrderService(db);
        for(var old:cart.view(buyer).items())cart.change(buyer,id(old,"id"),1,"REMOVE");
        long p=products.save(seller,null,new ProductForm("VNPAY HTTP "+UUID.randomUUID(),510001,"Fixture HTTP",new BigDecimal("150000"),"USED",3,"PUBLIC",seller.id(),0,0,""),List.of(),false);products.action(admin,p,"APPROVE",1,"Fixture",true);cart.change(buyer,p,1,"ADD");
        long batch=orders.checkout(buyer,new CheckoutForm("Người nhận thử","0901234567","Địa chỉ thử Hà Nội","","VNPAY_SANDBOX",UUID.randomUUID().toString(),cart.view(buyer).quote()));long o=id(orders.receipt(buyer,batch).get(0),"id");
        var b=new ShopHttpIT.Browser();b.login("buyer1");var receipt=b.get("/checkout/success?batch="+batch);assertEquals(200,receipt.statusCode());assertTrue(receipt.body().contains("Thanh toán VNPAY"));
        var redirect=b.post("/payments/vnpay/create",Map.of("csrfToken",b.token("/buyer/orders/detail?id="+o),"orderId",""+o,"amount","1","buyer_id","500005"));assertEquals(303,redirect.statusCode());var fields=decode(redirect.headers().firstValue("Location").orElseThrow());assertEquals("15000000",fields.get("vnp_Amount"));assertTrue(VnpayProtocol.verify(app.vnpay().orElseThrow(),fields));
        var result=new HashMap<>(Map.of("vnp_TmnCode",fields.get("vnp_TmnCode"),"vnp_TxnRef",fields.get("vnp_TxnRef"),"vnp_Amount",fields.get("vnp_Amount"),"vnp_ResponseCode","00","vnp_TransactionStatus","00"));result.put("vnp_SecureHash",VnpayProtocol.hmac(app.vnpay().orElseThrow().secret(),VnpayProtocol.canonical(result)));
        var returned=b.get("/payments/vnpay/return?"+encode(result));assertEquals(200,returned.statusCode());assertTrue(returned.body().contains("Đang xác nhận thanh toán"));
        assertEquals("PENDING_CONFIRMATION",db.read(h->h.createQuery("SELECT status FROM payments WHERE order_id=:id").bind("id",o).mapTo(String.class).one()));
        var other=new ShopHttpIT.Browser();other.login("buyer2");assertEquals(404,other.get("/payments/vnpay/result?ref="+fields.get("vnp_TxnRef")).statusCode());
    }
}
