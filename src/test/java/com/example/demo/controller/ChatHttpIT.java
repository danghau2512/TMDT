package com.example.demo.controller;

import com.example.demo.config.*;
import com.example.demo.dto.ProductForm;
import com.example.demo.model.CurrentUser;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatHttpIT {
    static Database db;static ShopServices shop;
    @BeforeAll static void open() throws Exception {
        String base=System.getenv("C2C_HTTP_BASE");Assumptions.assumeTrue(base!=null);
        assertEquals("true",System.getenv("C2C_IT_ALLOWED"));var uri=URI.create(base);assertEquals("http",uri.getScheme());assertTrue(Set.of("127.0.0.1","localhost").contains(uri.getHost()));
        var app=AppConfig.load();db=new Database(app.database().orElseThrow());assertTrue(db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));
        HttpTestTarget.verify(db,base);ShopHttpIT.base=base;shop=new ShopServices(db,app.uploadRoot());
    }
    @AfterAll static void close(){if(db!=null)db.close();JdbcLifecycle.shutdown();}
    @Test void twoAccountsExchangeOverHttpWithCsrfPrivacyAndPersistentHistory() throws Exception {
        var owner=new CurrentUser(500002,"Seller","USER");var adminActor=new CurrentUser(500001,"Admin","ADMIN");
        long p=shop.products().save(owner,null,new ProductForm("Chat HTTP "+UUID.randomUUID(),510001,"Fixture riêng cho chat",new BigDecimal("150000"),"USED",1,"PUBLIC",owner.id(),0,0,""),List.of(),false);
        shop.products().action(adminActor,p,"APPROVE",1,"Duyệt fixture",true);
        var buyer=new ShopHttpIT.Browser();buyer.login("buyer1");var seller=new ShopHttpIT.Browser();seller.login("seller1");var outsider=new ShopHttpIT.Browser();outsider.login("buyer2");var admin=new ShopHttpIT.Browser();admin.login("admin");
        var start=buyer.post("/messages/start",Map.of("csrfToken",buyer.token("/products/detail?id="+p),"productId",""+p));assertEquals(303,start.statusCode());
        String location=start.headers().firstValue("Location").orElseThrow();long c=Long.parseLong(location.substring(location.indexOf("id=")+3));String view="/messages?id="+c;
        assertEquals(location,buyer.post("/messages/start",Map.of("csrfToken",buyer.token(view),"productId",""+p)).headers().firstValue("Location").orElseThrow());
        assertEquals(400,seller.post("/messages/start",Map.of("csrfToken",seller.token("/messages"),"productId",""+p)).statusCode());
        String body="Xin chào <script>alert(1)</script>",nonce=UUID.randomUUID().toString();
        var fields=new HashMap<>(Map.of("csrfToken",buyer.token(view),"conversationId",""+c,"body",body,"nonce",nonce,"senderId","500005"));
        assertEquals(303,buyer.post("/messages/send",fields).statusCode());assertEquals(303,buyer.post("/messages/send",fields).statusCode());
        assertEquals(1,(int)db.read(h->h.createQuery("SELECT COUNT(*) FROM chat_messages WHERE conversation_id=:id AND sender_id=500004").bind("id",c).mapTo(Integer.class).one()));
        var invalid=new HashMap<>(fields);invalid.remove("csrfToken");assertEquals(403,buyer.post("/messages/send",invalid).statusCode());
        assertTrue(seller.get(view).body().contains("Xin chào &lt;script&gt;alert(1)&lt;/script&gt;"));
        var thread=JsonParser.parseString(seller.get("/messages/api?conversationId="+c).body()).getAsJsonObject();String first=thread.getAsJsonArray("messages").get(0).getAsJsonObject().get("id").getAsString();
        assertEquals(303,seller.post("/messages/read",Map.of("csrfToken",seller.token(view),"conversationId",""+c,"through",first)).statusCode());
        assertEquals(303,seller.post("/messages/send",Map.of("csrfToken",seller.token(view),"conversationId",""+c,"body","Chào bạn, sản phẩm còn nhé!","nonce",UUID.randomUUID().toString())).statusCode());
        var fresh=JsonParser.parseString(buyer.get("/messages/api?conversationId="+c+"&cursor="+first).body()).getAsJsonObject().getAsJsonArray("messages");assertEquals(1,fresh.size());assertTrue(fresh.get(0).getAsJsonObject().get("body").getAsString().contains("sản phẩm còn"));
        for(var third:List.of(outsider,admin)){assertEquals(404,third.get(view).statusCode());assertEquals(404,third.get("/messages/api?conversationId="+c).statusCode());assertEquals(404,third.post("/messages/send",Map.of("csrfToken",third.token("/messages"),"conversationId",""+c,"body","Không có quyền","nonce",UUID.randomUUID().toString())).statusCode());}
        assertTrue(buyer.get(view).body().contains("Chào bạn, sản phẩm còn nhé!"));
        assertEquals(401,new ShopHttpIT.Browser().get("/messages/api?conversationId="+c).statusCode());
    }
}
