package com.example.demo.dao;

import com.example.demo.model.CurrentUser;
import org.jdbi.v3.core.Handle;
import java.util.*;

public final class ReviewDao {
    private final Handle handle;
    public ReviewDao(Handle handle) { this.handle = handle; }

    public Map<String,Object> item(long order, long item) {
        return handle.createQuery("SELECT * FROM order_items WHERE id=:item AND order_id=:order")
                .bind("item",item).bind("order",order).mapToMap().findOne()
                .orElseThrow(() -> new com.example.demo.exception.ShopException(404,"Không tìm thấy sản phẩm trong đơn."));
    }
    public Optional<Map<String,Object>> existing(long item) {
        return handle.createQuery("SELECT * FROM reviews WHERE order_item_id=:item")
                .bind("item",item).mapToMap().findOne();
    }
    public Map<String,Map<String,Object>> byOrder(long order) {
        var result = new LinkedHashMap<String,Map<String,Object>>();
        for (var review : handle.createQuery("SELECT * FROM reviews WHERE order_id=:order").bind("order",order).mapToMap().list())
            result.put(review.get("order_item_id").toString(),review);
        return result;
    }
    public long insert(long order, long item, CurrentUser buyer, int rating, String comment) {
        // Form demo có một điểm chung cho giao dịch; giữ hai cột rating bắt buộc của V001.
        return handle.createUpdate("INSERT INTO reviews(order_item_id,order_id,buyer_id,product_rating,seller_rating,comment) VALUES(:item,:order,:buyer,:rating,:rating,:comment)")
                .bind("item",item).bind("order",order).bind("buyer",buyer.id()).bind("rating",rating).bind("comment",comment).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();
    }
    private static final String PUBLIC = """
        FROM reviews r JOIN order_items i ON i.id=r.order_item_id AND i.order_id=r.order_id
        JOIN orders o ON o.id=r.order_id AND o.buyer_id=r.buyer_id
        JOIN users u ON u.id=r.buyer_id
        WHERE i.product_id=:product AND r.visibility='VISIBLE' AND o.status='COMPLETED'
        """;
    public Map<String,Object> summary(long product) {
        return handle.createQuery("SELECT COUNT(*) review_count,COALESCE(AVG(r.product_rating),0) average_rating,COALESCE(SUM(r.product_rating=5),0) stars5,COALESCE(SUM(r.product_rating=4),0) stars4,COALESCE(SUM(r.product_rating=3),0) stars3,COALESCE(SUM(r.product_rating=2),0) stars2,COALESCE(SUM(r.product_rating=1),0) stars1 "+PUBLIC)
                .bind("product",product).mapToMap().one();
    }
    public Map<String,Object> sellerSummary(long seller) {
        return handle.createQuery("""
            SELECT COUNT(*) review_count,COALESCE(AVG(r.seller_rating),0) average_rating
            FROM reviews r JOIN order_items i ON i.id=r.order_item_id AND i.order_id=r.order_id
            JOIN orders o ON o.id=r.order_id AND o.buyer_id=r.buyer_id
            JOIN products p ON p.id=i.product_id AND p.seller_id=o.seller_id
            JOIN users u ON u.id=p.seller_id JOIN categories c ON c.id=p.category_id
            WHERE o.seller_id=:seller AND r.visibility='VISIBLE' AND o.status='COMPLETED'
            AND p.visibility='PUBLIC' AND p.moderation_status='APPROVED' AND u.status='ACTIVE' AND c.status='ACTIVE'
            """).bind("seller",seller).mapToMap().one();
    }
    public List<Map<String,Object>> publicReviews(long product) {
        return publicReviews(product,0,false,1);
    }
    private static final String FILTER=" AND (:stars=0 OR r.product_rating=:stars) AND (:photos=0 OR EXISTS(SELECT 1 FROM review_images ri WHERE ri.review_id=r.id))";
    public int count(long product,int stars,boolean photos) {
        return handle.createQuery("SELECT COUNT(*) "+PUBLIC+FILTER).bind("product",product).bind("stars",stars).bind("photos",photos?1:0).mapTo(Integer.class).one();
    }
    public List<Map<String,Object>> publicReviews(long product,int stars,boolean photos,int page) {
        var reviews=handle.createQuery("SELECT r.id,r.product_rating,r.comment,r.created_at,u.display_name reviewer_name "+PUBLIC+FILTER+" ORDER BY r.created_at DESC,r.id DESC LIMIT 8 OFFSET :offset")
            .bind("product",product).bind("stars",stars).bind("photos",photos?1:0).bind("offset",(page-1)*8).mapToMap().list();
        for(var review:reviews) review.put("photos",images(((Number)review.get("id")).longValue()));
        return reviews;
    }
    public List<Map<String,Object>> images(long review) {return handle.createQuery("SELECT asset_id FROM review_images WHERE review_id=:id ORDER BY sort_order").bind("id",review).mapToMap().list();}
    public Optional<Map<String,Object>> publicImage(long review,long asset) {
        return handle.createQuery("""
            SELECT a.storage_key,a.mime_type FROM review_images ri JOIN media_assets a ON a.id=ri.asset_id
            JOIN reviews r ON r.id=ri.review_id JOIN order_items i ON i.id=r.order_item_id AND i.order_id=r.order_id
            JOIN orders o ON o.id=r.order_id AND o.buyer_id=r.buyer_id JOIN products p ON p.id=i.product_id
            JOIN users u ON u.id=p.seller_id JOIN categories c ON c.id=p.category_id
            WHERE ri.review_id=:review AND ri.asset_id=:asset AND a.purpose='REVIEW_IMAGE'
            AND r.visibility='VISIBLE' AND o.status='COMPLETED'
            AND p.visibility='PUBLIC' AND p.moderation_status='APPROVED' AND u.status='ACTIVE' AND c.status='ACTIVE'
            """).bind("review",review).bind("asset",asset).mapToMap().findOne();
    }
    public void attachImages(long review,long actor,List<com.example.demo.model.StoredImage> images) {
        int index=0;
        for(var image:images) {
            long asset=handle.createUpdate("INSERT INTO media_assets(uploaded_by,storage_key,original_name,mime_type,byte_size,sha256,width,height,purpose) VALUES(:actor,:key,'review',:mime,:size,:hash,:width,:height,'REVIEW_IMAGE')")
                .bind("actor",actor).bind("key",image.key()).bind("mime",image.mime()).bind("size",image.size()).bind("hash",image.sha256()).bind("width",image.width()).bind("height",image.height()).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();
            handle.createUpdate("INSERT INTO review_images(review_id,asset_id,sort_order) VALUES(:review,:asset,:index)").bind("review",review).bind("asset",asset).bind("index",index++).execute();
        }
    }
}
