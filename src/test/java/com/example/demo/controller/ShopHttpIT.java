package com.example.demo.controller;

import com.example.demo.config.*;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Một kịch bản HTTP ngắn, chỉ trên Tomcat loopback dùng cùng schema test. */
class ShopHttpIT {
    static Database db; static String base;
    @BeforeAll static void open() throws Exception {
        base=System.getenv("C2C_HTTP_BASE"); Assumptions.assumeTrue(base!=null && !base.isBlank());
        assertEquals("true",System.getenv("C2C_IT_ALLOWED")); var uri=URI.create(base);
        assertEquals("http",uri.getScheme()); assertTrue(Set.of("127.0.0.1","localhost").contains(uri.getHost()));
        db=new Database(AppConfig.load().database().orElseThrow()); assertTrue(db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));
        assertNull(uri.getUserInfo());assertNull(uri.getQuery());assertNull(uri.getFragment()); HttpTestTarget.verify(db,base);
    }
    @AfterAll static void close() { if(db!=null) db.close(); JdbcLifecycle.shutdown(); }
    static String field(String html,String name) { var m=Pattern.compile("name=\""+name+"\" value=\"([^\"]+)\"").matcher(html); assertTrue(m.find(),"Thiếu trường "+name); return m.group(1); }
    static class Browser {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        HttpResponse<String> get(String path) throws Exception { return http.send(HttpRequest.newBuilder(URI.create(base+path)).GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); }
        HttpResponse<String> post(String path,Map<String,String> values) throws Exception {
            String body=values.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
            return http.send(HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/x-www-form-urlencoded;charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
        String token(String path) throws Exception { var page=get(path); assertEquals(200,page.statusCode(),path); return field(page.body(),"csrfToken"); }
        void login(String account) throws Exception { assertEquals(303,post("/login",Map.of("csrfToken",token("/login"),"email",account+"@c2c.example","password","C2cDemo!2026")).statusCode()); }
        HttpResponse<String> multipart(Map<String,String> values,byte[] photo) throws Exception {
            return multipart("/seller/products/save",values,photo);
        }
        HttpResponse<String> multipart(String path,Map<String,String> values,byte[] photo) throws Exception {
            String boundary="Shop"+UUID.randomUUID(); var body=new ByteArrayOutputStream();
            for(var e:values.entrySet()) body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+e.getKey()+"\"\r\n\r\n"+e.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"photos\"; filename=\"photo.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8)); body.write(photo); body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            return http.send(HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
        void action(String view,long order,String action) throws Exception { assertEquals(303,post("/"+view+"/orders/action",Map.of("csrfToken",token("/"+view+"/orders/detail?id="+order),"id",""+order,"action",action,"reason","Kiểm tra demo")).statusCode(),action); }
    }
    @Test void sellApproveBuyPayDeliverAndReadImmutablePhotoSnapshotOverHttp() throws Exception {
        var seller=new Browser();seller.login("seller1"); var admin=new Browser();admin.login("admin");var buyer=new Browser();buyer.login("buyer1");
        // Chỉ dọn giỏ fixture của test; không đụng database ứng dụng.
        db.transaction(h->{h.createUpdate("DELETE ci FROM cart_items ci JOIN carts c ON c.id=ci.cart_id WHERE c.user_id=500004").execute();return null;});
        String title="Tin HTTP "+UUID.randomUUID(); var data=new HashMap<String,String>(Map.of("csrfToken",seller.token("/seller/products/new"),"title",title,"description","Sản phẩm thử HTTP tiếng Việt <script>demo</script>","categoryId","510001","price","150000","stock","2","condition","USED","visibility","PUBLIC"));
        assertEquals(400,seller.multipart(data,"không phải ảnh".getBytes(StandardCharsets.UTF_8)).statusCode());
        var png=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(3,3,BufferedImage.TYPE_INT_RGB),"png",png);
        data.put("listingVersion","\"><script>demo</script>");
        var badVersion=seller.multipart(data,png.toByteArray()); assertEquals(400,badVersion.statusCode());assertFalse(badVersion.body().contains("<script>demo</script>")); data.remove("listingVersion");
        data.put("categoryId","không phải số");assertEquals(400,seller.multipart(data,png.toByteArray()).statusCode());data.put("categoryId","510001");
        assertEquals(303,seller.multipart(data,png.toByteArray()).statusCode());
        long product=db.read(h->h.createQuery("SELECT id FROM products WHERE title=:title").bind("title",title).mapTo(Long.class).one());
        assertEquals(404,buyer.get("/products/detail?id="+product).statusCode()); assertEquals(404,buyer.get("/seller/products/edit?id="+product).statusCode()); assertEquals(403,buyer.get("/admin/products").statusCode());
        assertEquals(303,admin.post("/admin/products/action",Map.of("csrfToken",admin.token("/admin/products?status=PENDING"),"id",""+product,"listingVersion","1","action","APPROVE","reason","Duyệt tin demo")).statusCode());
        for(String path:List.of("/home","/products","/categories","/products?keyword=HTTP&min=1&max=200000&condition=USED&page=1","/products/detail?id="+product,"/seller/products","/seller/products/edit?id="+product,"/seller/orders")) assertEquals(200,seller.get(path).statusCode(),path);
        assertEquals(400,buyer.get("/products?min=1e999999999").statusCode());
        assertFalse(buyer.get("/products/detail?id="+product).body().contains("<script>demo</script>"));
        assertEquals(403,buyer.post("/cart/add",Map.of("productId",""+product,"quantity","1")).statusCode());
        assertEquals(303,buyer.post("/cart/add",Map.of("csrfToken",buyer.token("/cart"),"productId",""+product,"quantity","1")).statusCode());
        String checkout=buyer.get("/checkout").body(); var fields= new HashMap<>(Map.of("csrfToken",field(checkout,"csrfToken"),"key",field(checkout,"key"),"quote",field(checkout,"quote"),"name","Người nhận demo","phone","0901234567","address","Địa chỉ demo tại Hà Nội","note","Giao buổi sáng","method","COD"));
        var submitted=buyer.post("/checkout",fields);assertEquals(303,submitted.statusCode());assertEquals(submitted.headers().firstValue("Location"),buyer.post("/checkout",fields).headers().firstValue("Location"));
        String receipt=submitted.headers().firstValue("Location").orElseThrow().substring(base.substring(base.indexOf("/c2c")).length()); assertEquals(200,buyer.get(receipt).statusCode());
        long order=db.read(h->h.createQuery("SELECT order_id FROM order_items WHERE product_id=:id").bind("id",product).mapTo(Long.class).one());
        long asset=db.read(h->h.createQuery("SELECT asset_id FROM product_images WHERE product_id=:id").bind("id",product).mapTo(Long.class).one());
        // Thay ảnh tin sau đặt hàng; ảnh snapshot cũ vẫn đọc được theo quyền đơn.
        var edit=seller.get("/seller/products/edit?id="+product).body(); data.put("id",""+product); data.put("listingVersion",field(edit,"listingVersion"));data.put("stockVersion",field(edit,"stockVersion"));data.put("stock","1");data.put("csrfToken",field(edit,"csrfToken"));data.put("title","Tin đã sửa "+UUID.randomUUID());
        assertEquals(303,seller.multipart(data,png.toByteArray()).statusCode());
        assertEquals(404,buyer.get("/media/product?asset="+asset).statusCode());
        assertEquals(200,buyer.get("/media/order-image?asset="+asset+"&orderId="+order).statusCode());
        assertEquals(404,new Browser().get("/media/order-image?asset="+asset+"&orderId="+order).statusCode());
        seller.action("seller",order,"CONFIRM");seller.action("seller",order,"SHIP");seller.action("seller",order,"DELIVER");buyer.action("buyer",order,"COMPLETE");
        assertTrue(buyer.get("/buyer/orders/detail?id="+order).body().contains("Hoàn thành")); assertEquals(200,admin.get("/admin/orders/detail?id="+order).statusCode());assertEquals(200,buyer.get("/buyer/orders").statusCode());
    }
}
