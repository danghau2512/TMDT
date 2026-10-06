package com.example.demo.dao;

import com.example.demo.dto.*;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import java.util.*;

/** DAO chỉ tồn tại trong callback có Handle; mọi giá trị đầu vào đều bind. */
public final class CatalogDao {
    private final Handle h;
    public CatalogDao(Handle h) { this.h=h; }
    public static final String PUBLIC="p.visibility='PUBLIC' AND p.moderation_status='APPROVED' AND u.status='ACTIVE' AND c.status='ACTIVE'";
    private static final String SELECT="""
        SELECT p.*,u.display_name seller_name,u.public_contact seller_contact,u.status seller_status,
        c.name category_name,c.status category_status,
        (SELECT asset_id FROM product_images WHERE product_id=p.id ORDER BY sort_order LIMIT 1) image_id,
        (SELECT reason FROM product_moderation_events WHERE product_id=p.id ORDER BY id DESC LIMIT 1) rejection_reason
        FROM products p JOIN users u ON u.id=p.seller_id JOIN categories c ON c.id=p.category_id
        """;
    public List<Map<String,Object>> categories() { return h.createQuery("SELECT id,name FROM categories WHERE status='ACTIVE' ORDER BY sort_order,id").mapToMap().list(); }
    public List<Map<String,Object>> suggestions(String keyword) {
        return h.createQuery("""
            SELECT p.id,p.title,p.price,
            (SELECT asset_id FROM product_images WHERE product_id=p.id ORDER BY sort_order LIMIT 1) image_id
            FROM products p JOIN users u ON u.id=p.seller_id JOIN categories c ON c.id=p.category_id
            """+" WHERE "+PUBLIC+" "+"""
             AND (LOCATE(:keyword,p.title)>0 OR LOCATE(:keyword,p.description)>0)
            ORDER BY CASE WHEN LOCATE(:keyword,p.title)=1 THEN 0
                          WHEN LOCATE(:keyword,p.title)>0 THEN 1 ELSE 2 END,
                     p.created_at DESC,p.id DESC
            LIMIT 8
            """).bind("keyword",keyword).mapToMap().list();
    }
    public List<Map<String,Object>> sellers() { return h.createQuery("SELECT id,display_name FROM users WHERE status='ACTIVE' ORDER BY display_name,id").mapToMap().list(); }
    public Map<String,Object> product(long id) { return h.createQuery(SELECT+" WHERE p.id=:id").bind("id",id).mapToMap().findOne().orElseThrow(()->new com.example.demo.exception.ShopException(404,"Không tìm thấy sản phẩm.")); }
    public Map<String,Object> lock(long id) { return h.createQuery("SELECT * FROM products WHERE id=:id FOR UPDATE").bind("id",id).mapToMap().findOne().orElseThrow(()->new com.example.demo.exception.ShopException(404,"Không tìm thấy sản phẩm.")); }
    private static final String FILTER="""
        AND (:category=0 OR p.category_id=:category) AND (:keyword='' OR LOCATE(:keyword,p.title)>0 OR LOCATE(:keyword,p.description)>0)
        AND (:min IS NULL OR p.price>=:min) AND (:max IS NULL OR p.price<=:max) AND (:condition='' OR p.condition_code=:condition)
        """;
    private Query bind(Query query,CatalogFilter f) { return query.bind("category",f.category()).bind("keyword",f.keyword()).bind("min",f.minPrice()).bind("max",f.maxPrice()).bind("condition",f.condition()); }
    public List<Map<String,Object>> list(CatalogFilter f) { return bind(h.createQuery(SELECT+" WHERE "+PUBLIC+FILTER+" ORDER BY p.created_at DESC,p.id DESC LIMIT 12 OFFSET :offset"),f).bind("offset",(f.page()-1)*12).mapToMap().list(); }
    public int count(CatalogFilter f) { return bind(h.createQuery("SELECT COUNT(*) FROM products p JOIN users u ON u.id=p.seller_id JOIN categories c ON c.id=p.category_id WHERE "+PUBLIC+FILTER),f).mapTo(Integer.class).one(); }
    public List<Map<String,Object>> managed(long seller,boolean admin,String status) { return h.createQuery(SELECT+" WHERE (:admin=1 OR p.seller_id=:seller) AND (:status='' OR p.moderation_status=:status) ORDER BY p.updated_at DESC,p.id DESC LIMIT 200").bind("admin",admin?1:0).bind("seller",seller).bind("status",status).mapToMap().list(); }
    public List<Map<String,Object>> images(long product) { return h.createQuery("SELECT asset_id,alt_text,is_defect,sort_order FROM product_images WHERE product_id=:id ORDER BY sort_order").bind("id",product).mapToMap().list(); }
    public long insert(ProductForm f,long seller) { return h.createUpdate("INSERT INTO products(seller_id,category_id,title,description,price,condition_code,stock_quantity,visibility) VALUES(:seller,:category,:title,:description,:price,:condition,:stock,:visibility)")
        .bind("seller",seller).bind("category",f.categoryId()).bind("title",f.title()).bind("description",f.description()).bind("price",f.price()).bind("condition",f.condition()).bind("stock",f.stock()).bind("visibility",f.visibility()).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one(); }
    public void edit(long id,ProductForm f,boolean contentChanged) { h.createUpdate("""
        UPDATE products SET category_id=:category,title=:title,description=:description,price=:price,condition_code=:condition,
        stock_quantity=:stock,visibility=:visibility,listing_version=listing_version+:change,
        stock_version=stock_version+1,moderation_status=CASE WHEN :change=1 THEN 'PENDING' ELSE moderation_status END,updated_at=CURRENT_TIMESTAMP(6) WHERE id=:id
        """).bind("id",id).bind("category",f.categoryId()).bind("title",f.title()).bind("description",f.description()).bind("price",f.price()).bind("condition",f.condition()).bind("stock",f.stock()).bind("visibility",f.visibility()).bind("change",contentChanged?1:0).execute(); }
    public void declaration(long product,ConditionForm d) {
        h.createUpdate("UPDATE products SET appearance_code=:appearance,operation_code=:operation,known_defects=:defects,repair_code=:repair,repair_details=:details,accessories=:accessories WHERE id=:id")
            .bind("id",product).bind("appearance",d.appearance()).bind("operation",d.operation()).bind("defects",d.defects()).bind("repair",d.repair()).bind("details",d.repairDetails()).bind("accessories",d.accessories()).execute();
    }
    public void defectImages(long product,Set<Long> assets,Set<Integer> indexes,boolean replaced) {
        h.createUpdate("UPDATE product_images SET is_defect=0 WHERE product_id=:id").bind("id",product).execute();
        if(replaced) for(int index:indexes) h.createUpdate("UPDATE product_images SET is_defect=1 WHERE product_id=:id AND sort_order=:index").bind("id",product).bind("index",index).execute();
        else for(long asset:assets) h.createUpdate("UPDATE product_images SET is_defect=1 WHERE product_id=:id AND asset_id=:asset").bind("id",product).bind("asset",asset).execute();
    }
    public void moderation(long id,String status) { h.createUpdate("UPDATE products SET moderation_status=:status,updated_at=CURRENT_TIMESTAMP(6) WHERE id=:id").bind("status",status).bind("id",id).execute(); }
    public void hide(long id) { h.createUpdate("UPDATE products SET visibility='HIDDEN',updated_at=CURRENT_TIMESTAMP(6) WHERE id=:id").bind("id",id).execute(); }
    public void event(long id,long actor,String from,String to,long version,String reason) { h.createUpdate("INSERT INTO product_moderation_events(product_id,actor_id,from_status,to_status,listing_version,reason) VALUES(:id,:actor,:from,:to,:version,:reason)").bind("id",id).bind("actor",actor).bind("from",from).bind("to",to).bind("version",version).bind("reason",reason).execute(); }
    public void stock(long id,int after) { h.createUpdate("UPDATE products SET stock_quantity=:quantity,stock_version=stock_version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=:id").bind("quantity",after).bind("id",id).execute(); }
    public void ledger(long product,Long item,String type,int before,int after,long actor,String reason) { h.createUpdate("INSERT INTO stock_movements(product_id,order_item_id,movement_type,quantity_delta,quantity_before,quantity_after,actor_id,reason) VALUES(:product,:item,:type,:delta,:before,:after,:actor,:reason)").bind("product",product).bind("item",item).bind("type",type).bind("delta",(long)after-before).bind("before",before).bind("after",after).bind("actor",actor).bind("reason",reason).execute(); }
}
