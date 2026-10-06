package com.example.demo.dao;

import com.example.demo.model.StoredImage;
import org.jdbi.v3.core.Handle;
import java.util.*;
public final class MediaDao {
    private final Handle h;
    public MediaDao(Handle h) { this.h=h; }
    public void replace(long product,long actor,List<StoredImage> images,String title) {
        // Chỉ bỏ liên kết của tin; asset/file cũ và snapshot đơn không bị xóa.
        h.createUpdate("DELETE FROM product_images WHERE product_id=:product").bind("product",product).execute();
        int index=0;
        for (var image:images) {
            long asset=h.createUpdate("INSERT INTO media_assets(uploaded_by,storage_key,original_name,mime_type,byte_size,sha256,width,height,purpose) VALUES(:actor,:key,'photo',:mime,:size,:hash,:width,:height,'PRODUCT_IMAGE')")
                .bind("actor",actor).bind("key",image.key()).bind("mime",image.mime()).bind("size",image.size()).bind("hash",image.sha256()).bind("width",image.width()).bind("height",image.height()).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();
            h.createUpdate("INSERT INTO product_images(product_id,asset_id,sort_order,alt_text) VALUES(:product,:asset,:sort,:title)").bind("product",product).bind("asset",asset).bind("sort",index++).bind("title",title).execute();
        }
    }
    public Optional<Map<String,Object>> productImage(long asset,long actor,boolean admin) { return h.createQuery("""
        SELECT a.storage_key,a.mime_type FROM media_assets a JOIN product_images i ON i.asset_id=a.id
        JOIN products p ON p.id=i.product_id JOIN users u ON u.id=p.seller_id JOIN categories c ON c.id=p.category_id
        WHERE a.id=:asset AND ((p.visibility='PUBLIC' AND p.moderation_status='APPROVED' AND u.status='ACTIVE' AND c.status='ACTIVE') OR p.seller_id=:actor OR :admin=1) LIMIT 1
        """).bind("asset",asset).bind("actor",actor).bind("admin",admin?1:0).mapToMap().findOne(); }
    public Optional<Map<String,Object>> orderImage(long asset,long order,long actor,boolean admin) { return h.createQuery("""
        SELECT a.storage_key,a.mime_type FROM media_assets a JOIN order_item_images i ON i.asset_id=a.id
        JOIN order_items oi ON oi.id=i.order_item_id JOIN orders o ON o.id=oi.order_id
        WHERE a.id=:asset AND o.id=:order AND (o.buyer_id=:actor OR o.seller_id=:actor OR :admin=1) LIMIT 1
        """).bind("asset",asset).bind("order",order).bind("actor",actor).bind("admin",admin?1:0).mapToMap().findOne(); }
    public void addEvidence(long complaint, long actor, List<StoredImage> images) {
        for(var image:images) {
            long asset=h.createUpdate("INSERT INTO media_assets(uploaded_by,storage_key,original_name,mime_type,byte_size,sha256,width,height,purpose) VALUES(:actor,:key,'evidence',:mime,:size,:hash,:width,:height,'COMPLAINT_EVIDENCE')")
                    .bind("actor",actor).bind("key",image.key()).bind("mime",image.mime()).bind("size",image.size()).bind("hash",image.sha256())
                    .bind("width",image.width()).bind("height",image.height()).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();
            h.createUpdate("INSERT INTO complaint_evidence(complaint_id,asset_id,uploaded_by) VALUES(:complaint,:asset,:actor)")
                    .bind("complaint",complaint).bind("asset",asset).bind("actor",actor).execute();
        }
    }
    public Optional<Map<String,Object>> evidence(long asset,long complaint) {
        return h.createQuery("SELECT a.storage_key,a.mime_type FROM media_assets a JOIN complaint_evidence e ON e.asset_id=a.id WHERE e.complaint_id=:complaint AND a.id=:asset AND a.purpose='COMPLAINT_EVIDENCE'")
                .bind("complaint",complaint).bind("asset",asset).mapToMap().findOne();
    }
}
