package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.dto.*;
import com.example.demo.model.*;
import com.example.demo.exception.ShopException;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.*;
import java.util.*;
import java.math.BigDecimal;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.lang.reflect.Proxy;
import static com.example.demo.service.ShopRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** Small focused checks, only on an explicitly configured isolated test schema. */
class UpgradesIT {
    static Database db;static ShopServices shop;
    static final CurrentUser admin=new CurrentUser(500001,"Admin","ADMIN"),seller=new CurrentUser(500002,"Seller","USER"),buyer=new CurrentUser(500004,"Buyer","USER"),other=new CurrentUser(500005,"Other","USER");
    record Fixture(long product,long order,long item){}
    @BeforeAll static void open()throws Exception {assertEquals("true",System.getenv("C2C_IT_ALLOWED"));var cfg=AppConfig.load();db=new Database(cfg.database().orElseThrow());assertTrue(db.read(h->h.createQuery("SELECT DATABASE()").mapTo(String.class).one()).endsWith("_test"));shop=new ShopServices(db,cfg.uploadRoot());}
    @AfterAll static void close(){if(db!=null)db.close();JdbcLifecycle.shutdown();}
    static ConditionForm condition(String defect){return new ConditionForm("LIGHT_SCRATCHES","NORMAL",defect,"NEVER",null,"Sạc và hộp",Set.of(),Set.of(0));}
    static StoredImage image()throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(4,4,BufferedImage.TYPE_INT_RGB),"png",out);byte[] bytes=out.toByteArray();Part part=(Part)Proxy.newProxyInstance(Part.class.getClassLoader(),new Class<?>[]{Part.class},(p,m,a)->switch(m.getName()){case "getSize"->(long)bytes.length;case "getInputStream"->new ByteArrayInputStream(bytes);default->null;});return shop.storage().save(part);}
    static long product(ConditionForm d,List<StoredImage> images,long category){long id=shop.products().save(seller,null,new ProductForm("Đồ test "+UUID.randomUUID(),category,"Mô tả thử",new BigDecimal("123000"),"USED",8,"PUBLIC",seller.id(),0,0,"",d),images,false);shop.products().action(admin,id,"APPROVE",1,"Duyệt test",true);return id;}
    static Fixture order(long product){for(var i:shop.cart().view(buyer).items())shop.cart().change(buyer,id(i,"id"),1,"REMOVE");shop.cart().change(buyer,product,1,"ADD");long batch=shop.orders().checkout(buyer,new CheckoutForm("Người thử","0901234567","Địa chỉ thử tại Hà Nội","","COD",UUID.randomUUID().toString(),shop.cart().view(buyer).quote()));long order=id(shop.orders().receipt(buyer,batch).get(0),"id");long item=db.read(h->h.createQuery("SELECT id FROM order_items WHERE order_id=:id").bind("id",order).mapTo(Long.class).one());return new Fixture(product,order,item);}
    @Test void declarationAndDefectPhotoAreImmutableSnapshotsAndLegacyFieldsStayNull()throws Exception {
        long p=product(condition("Trầy ở góc <b>trái</b>"),List.of(image()),510001);var f=order(p);
        var snapshot=db.read(h->h.createQuery("SELECT * FROM order_items WHERE id=:id").bind("id",f.item).mapToMap().one());assertEquals("Trầy ở góc <b>trái</b>",snapshot.get("known_defects_snapshot"));
        assertEquals(1,(int)db.read(h->h.createQuery("SELECT SUM(is_defect_snapshot) FROM order_item_images WHERE order_item_id=:id").bind("id",f.item).mapTo(Integer.class).one()));
        var old=db.read(h->h.createQuery("SELECT * FROM products WHERE id=:id").bind("id",p).mapToMap().one());
        var d=new ConditionForm("WORN","FAULTY","Mô tả mới","REPAIRED","Thay pin","Sạc",Set.of(),Set.of());
        shop.products().save(seller,p,new ProductForm(text(old,"title"),510001,text(old,"description"),money(old,"price"),"USED",number(old,"stock_quantity"),"PUBLIC",seller.id(),id(old,"listing_version"),id(old,"stock_version"),"",d),List.of(),false);
        assertEquals("Trầy ở góc <b>trái</b>",db.read(h->h.createQuery("SELECT known_defects_snapshot FROM order_items WHERE id=:id").bind("id",f.item).mapTo(String.class).one()));
        long legacy=product(null,List.of(),510001);assertNull(((Map<?,?>)shop.products().detail(legacy,null,false).get("product")).get("appearance_code"));
    }
    @Test void verifiedReviewPhotoFiltersAndPrivacyUseActualRows()throws Exception {
        long p=product(null,List.of(),510001);var f=order(p);
        assertEquals(409,assertThrows(ShopException.class,()->shop.reviews().create(buyer,f.order,f.item,4,"Chưa hoàn tất")).status());
        for(String a:List.of("CONFIRM","SHIP","DELIVER"))shop.orders().action(seller,f.order,"seller",a,"");shop.orders().action(buyer,f.order,"buyer","COMPLETE","");
        assertEquals(404,assertThrows(ShopException.class,()->shop.reviews().create(other,f.order,f.item,4,"Trái quyền")).status());
        var img=image();shop.reviews().create(buyer,f.order,f.item,4,"Ảnh thật <script>test</script>",List.of(img));
        assertEquals(409,assertThrows(ShopException.class,()->shop.reviews().create(buyer,f.order,f.item,4,"Lần hai")).status());
        var result=shop.products().detail(p,null,false,4,true,1);var summary=(Map<?,?>)result.get("ratingSummary");assertEquals(1,((Number)summary.get("stars4")).intValue());
        var rows=(List<Map<String,Object>>)result.get("productReviews");assertEquals(1,rows.size());var photos=(List<Map<String,Object>>)rows.get(0).get("photos");assertEquals(1,photos.size());assertFalse(rows.get(0).containsKey("email"));
        long asset=id(photos.get(0),"asset_id"),review=id(rows.get(0),"id");assertFalse(shop.reviews().image(review,asset).isEmpty());
        assertTrue(((List<?>)shop.products().detail(p,null,false,5,true,1).get("productReviews")).isEmpty());
        shop.products().action(seller,p,"HIDE",1,"",false);assertEquals(404,assertThrows(ShopException.class,()->shop.reviews().image(review,asset)).status());
    }
    @Test void complaintCenterAndCompareKeepOwnershipAndVisibility()throws Exception {
        long p=product(null,List.of(),510001),p2=product(null,List.of(),510001),p3=product(null,List.of(),510002);var f=order(p);
        long c=shop.complaints().create(buyer,f.order,"OTHER","Nội dung phản ánh test",List.of()).id();
        assertEquals(c,shop.complaints().create(buyer,f.order,"OTHER","Gửi lại hồ sơ",List.of()).id());
        assertEquals(1,((List<?>)shop.complaints().center(buyer,false,"RECEIVED","").get("complaints")).stream().filter(r->id((Map<String,Object>)r,"id")==c).count());
        assertEquals(404,assertThrows(ShopException.class,()->shop.complaints().detail(other,c,false)).status());
        String orderCode=db.read(h->h.createQuery("SELECT order_code FROM orders WHERE id=:id").bind("id",f.order).mapTo(String.class).one());assertEquals(1,((List<?>)shop.complaints().center(admin,true,"",orderCode).get("complaints")).size());
        shop.complaints().act(admin,c,"PROCESS","Đang kiểm tra","","");shop.complaints().act(admin,c,"RESOLVE","Đã trao đổi","NO_ACTION","Đã hướng dẫn người mua");
        var detail=shop.complaints().detail(buyer,c,false);assertEquals("RESOLVED",((Map<?,?>)detail.get("complaint")).get("status"));assertEquals("PENDING",((Map<?,?>)detail.get("order")).get("status"));assertNotNull(((List<Map<String,Object>>)detail.get("messages")).get(0).get("author_name"));
        assertEquals(true,shop.products().compare(List.of(p,p2)).get("sameCategory"));assertEquals(false,shop.products().compare(List.of(p,p3)).get("sameCategory"));
        var compared=(List<Map<String,Object>>)shop.products().compare(List.of(p,p,p2)).get("comparedProducts");assertEquals(2,compared.size());assertFalse(compared.get(0).containsKey("seller_contact"));
        shop.products().action(seller,p2,"HIDE",1,"",false);assertEquals(List.of(p2),shop.products().compare(List.of(p,p2)).get("unavailableIds"));
    }
}
