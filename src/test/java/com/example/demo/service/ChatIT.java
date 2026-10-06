package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.dto.ProductForm;
import com.example.demo.exception.ShopException;
import com.example.demo.model.*;
import com.example.demo.storage.ImageStorage;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra tập trung, chỉ schema *_test trên MySQL/InnoDB thật. */
class ChatIT {
    static Database db;static ChatService chat;static ProductService products;
    static final CurrentUser buyer=new CurrentUser(500004,"Buyer","USER"),seller=new CurrentUser(500002,"Seller","USER"),
        other=new CurrentUser(500005,"Other","USER"),admin=new CurrentUser(500001,"Admin","ADMIN");
    @BeforeAll static void open() throws Exception {
        assertEquals("true",System.getenv("C2C_IT_ALLOWED"));var app=AppConfig.load();db=new Database(app.database().orElseThrow());
        assertTrue(db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));
        chat=new ChatService(db);products=new ProductService(db,new ImageStorage(app.uploadRoot()));
    }
    @AfterAll static void close(){if(db!=null)db.close();JdbcLifecycle.shutdown();}
    static long product(){long id=products.save(seller,null,new ProductForm("Chat fixture "+UUID.randomUUID(),510001,"Trao đổi trước khi mua",new BigDecimal("150000"),"USED",3,"PUBLIC",seller.id(),0,0,""),List.of(),false);products.action(admin,id,"APPROVE",1,"Duyệt fixture",true);return id;}
    static String nonce(){return UUID.randomUUID().toString();}
    @Test void twoPeopleExchangePersistedMessagesAndReadCursorDoesNotConsumeFutureMessages(){
        long p=product(),c=chat.start(buyer,p);assertEquals(c,chat.start(buyer,p));
        assertEquals(400,assertThrows(ShopException.class,()->chat.start(seller,p)).status());
        String key=nonce();var first=chat.send(buyer,c,"Xin chào <script>alert(1)</script> 😊",key);assertEquals(first,chat.send(buyer,c,first.body(),key));
        assertEquals(409,assertThrows(ShopException.class,()->chat.send(buyer,c,"Nội dung khác",key)).status());
        var before=chat.unread(seller);assertTrue(before>0);chat.read(seller,c,Long.parseLong(first.id()));assertEquals(before-1,chat.unread(seller));
        var second=chat.send(buyer,c,"Tin đến sau mốc đã đọc",nonce());chat.read(seller,c,Long.parseLong(first.id()));assertEquals(before,chat.unread(seller));
        var reply=chat.send(seller,c,"Chào bạn, món đồ vẫn còn nhé!",nonce());
        var history=(List<ChatMessage>)chat.thread(buyer,c,0,true).get("messages");assertEquals(List.of(first.id(),second.id(),reply.id()),history.stream().map(ChatMessage::id).toList());
        for(var outsider:List.of(other,admin)){
            assertEquals(404,assertThrows(ShopException.class,()->chat.thread(outsider,c,0,true)).status());
            assertEquals(404,assertThrows(ShopException.class,()->chat.send(outsider,c,"Xâm nhập",nonce())).status());
            assertEquals(404,assertThrows(ShopException.class,()->chat.read(outsider,c,Long.parseLong(reply.id()))).status());
        }
        long another=chat.start(other,p);var foreign=chat.send(other,another,"Tin ở cuộc khác",nonce());
        assertEquals(400,assertThrows(ShopException.class,()->chat.read(buyer,c,Long.parseLong(foreign.id()))).status());
        assertEquals(400,assertThrows(ShopException.class,()->chat.send(buyer,c," \n\t ",nonce())).status());
        assertEquals(400,assertThrows(ShopException.class,()->chat.send(buyer,c,"a".repeat(2001),nonce())).status());
    }
    @Test void hiddenListingRetainsHistoryAndInactiveAccountCannotSend(){
        long p=product(),c=chat.start(buyer,p);var first=chat.send(buyer,c,"Lịch sử phải được giữ",nonce());
        products.action(seller,p,"HIDE",1,"",false);assertEquals(c,chat.start(buyer,p));
        var info=(Map<?,?>)chat.thread(buyer,c,0,true).get("conversation");assertEquals(false,info.get("available"));assertEquals("",info.get("productPath"));assertEquals("/assets/images/product-placeholder.svg",info.get("imagePath"));
        assertEquals(404,assertThrows(ShopException.class,()->chat.start(other,p)).status());
        db.transaction(h->{h.createUpdate("UPDATE users SET status='INACTIVE' WHERE id=:id").bind("id",seller.id()).execute();return null;});
        try{
            assertEquals(403,assertThrows(ShopException.class,()->chat.send(seller,c,"Không được gửi",nonce())).status());
            assertEquals(409,assertThrows(ShopException.class,()->chat.send(buyer,c,"Đối tác đã khóa",nonce())).status());
            assertEquals(first.id(),((List<ChatMessage>)chat.thread(buyer,c,0,true).get("messages")).get(0).id());
        }finally{db.transaction(h->{h.createUpdate("UPDATE users SET status='ACTIVE' WHERE id=:id").bind("id",seller.id()).execute();return null;});}
    }
    @Test void concurrentStartAndSendRetryAreUniqueAndHistoryPagesDoNotOverlap() throws Exception {
        long p=product();var pool=Executors.newFixedThreadPool(2);
        try{
            var a=pool.submit(()->chat.start(buyer,p));var b=pool.submit(()->chat.start(buyer,p));long c=a.get(10,TimeUnit.SECONDS);assertEquals(c,b.get(10,TimeUnit.SECONDS));
            String key=nonce();var x=pool.submit(()->chat.send(buyer,c,"Chỉ một lần",key));var y=pool.submit(()->chat.send(buyer,c,"Chỉ một lần",key));assertEquals(x.get(10,TimeUnit.SECONDS).id(),y.get(10,TimeUnit.SECONDS).id());
            for(int i=0;i<51;i++)chat.send(buyer,c,"Tin phân trang "+i,nonce());
            var latest=chat.thread(seller,c,0,true);assertEquals(true,latest.get("more"));var rows=(List<ChatMessage>)latest.get("messages");assertEquals(50,rows.size());
            var older=(List<ChatMessage>)chat.thread(seller,c,Long.parseLong(rows.get(0).id()),true).get("messages");assertEquals(2,older.size());
            assertTrue(rows.stream().noneMatch(m->older.stream().anyMatch(o->o.id().equals(m.id()))));
        }finally{pool.shutdownNow();}
    }
}
