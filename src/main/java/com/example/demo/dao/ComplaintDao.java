package com.example.demo.dao;

import com.example.demo.model.CurrentUser;
import org.jdbi.v3.core.Handle;
import java.util.*;

public final class ComplaintDao {
    private final Handle handle;
    public ComplaintDao(Handle handle) { this.handle = handle; }
    public Optional<Map<String,Object>> byOrder(long order) {
        return handle.createQuery("SELECT * FROM complaints WHERE order_id=:order").bind("order",order).mapToMap().findOne();
    }
    public Optional<Map<String,Object>> referenceByOrder(long order) {
        return handle.createQuery("SELECT id,status FROM complaints WHERE order_id=:order").bind("order",order).mapToMap().findOne();
    }
    public Map<String,Object> get(long complaint, boolean lock) {
        return handle.createQuery("SELECT * FROM complaints WHERE id=:id"+(lock?" FOR UPDATE":""))
                .bind("id",complaint).mapToMap().findOne()
                .orElseThrow(() -> new com.example.demo.exception.ShopException(404,"Không tìm thấy khiếu nại."));
    }
    public List<Map<String,Object>> list(long buyer, boolean admin, String status) {
        return list(buyer,admin,status,"");
    }
    public Map<String,Object> stats(long buyer,boolean admin) {
        return handle.createQuery("SELECT COUNT(*) total,COALESCE(SUM(status='RECEIVED'),0) received,COALESCE(SUM(status='PROCESSING'),0) processing,COALESCE(SUM(status='RESOLVED'),0) resolved FROM complaints WHERE (:admin=1 OR buyer_id=:buyer)").bind("admin",admin?1:0).bind("buyer",buyer).mapToMap().one();
    }
    public List<Map<String,Object>> list(long buyer, boolean admin, String status,String query) {
        return handle.createQuery("""
            SELECT c.*,o.order_code,o.buyer_name_snapshot,o.seller_name_snapshot,
            (SELECT product_name_snapshot FROM order_items WHERE order_id=o.id ORDER BY id LIMIT 1) product_name,
            (SELECT img.asset_id FROM order_item_images img JOIN order_items oi ON oi.id=img.order_item_id WHERE oi.order_id=o.id ORDER BY oi.id,img.sort_order LIMIT 1) image_id
            FROM complaints c JOIN orders o ON o.id=c.order_id
            WHERE (:admin=1 OR c.buyer_id=:buyer) AND (:status='' OR c.status=:status) AND (:query='' OR LOCATE(:query,o.order_code)>0)
            ORDER BY c.updated_at DESC,c.id DESC LIMIT 200
            """).bind("admin",admin?1:0).bind("buyer",buyer).bind("status",status).bind("query",query).mapToMap().list();
    }
    public long insert(long order, CurrentUser buyer, String reason, String title, String content) {
        return handle.createUpdate("INSERT INTO complaints(order_id,buyer_id,reason_code,title,content) VALUES(:order,:buyer,:reason,:title,:content)")
                .bind("order",order).bind("buyer",buyer.id()).bind("reason",reason).bind("title",title).bind("content",content)
                .executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();
    }
    public void message(long complaint, CurrentUser author, String type, String body) {
        handle.createUpdate("INSERT INTO complaint_messages(complaint_id,author_id,author_role_snapshot,message_type,body) VALUES(:id,:actor,:role,:type,:body)")
                .bind("id",complaint).bind("actor",author.id()).bind("role",author.role()).bind("type",type).bind("body",body).execute();
    }
    public void history(long complaint, String from, String to, CurrentUser actor, String reason, String code, String summary) {
        handle.createUpdate("INSERT INTO complaint_status_history(complaint_id,from_status,to_status,actor_id,reason,resolution_code_snapshot,resolution_summary_snapshot) VALUES(:id,:from,:to,:actor,:reason,:code,:summary)")
                .bind("id",complaint).bind("from",from).bind("to",to).bind("actor",actor.id()).bind("reason",reason).bind("code",code).bind("summary",summary).execute();
    }
    public void state(long complaint, CurrentUser admin, String next, String code, String summary) {
        handle.createUpdate("""
            UPDATE complaints SET status=:next,assigned_admin_id=:admin,resolution_code=:code,resolution_summary=:summary,
            resolved_at=CASE WHEN :next='RESOLVED' THEN CURRENT_TIMESTAMP(6) ELSE NULL END,updated_at=CURRENT_TIMESTAMP(6) WHERE id=:id
            """).bind("next",next).bind("admin",admin.id()).bind("code",code).bind("summary",summary).bind("id",complaint).execute();
    }
    public void touched(long complaint) { handle.createUpdate("UPDATE complaints SET updated_at=CURRENT_TIMESTAMP(6) WHERE id=:id").bind("id",complaint).execute(); }
    public int evidenceCount(long complaint) { return handle.createQuery("SELECT COUNT(*) FROM complaint_evidence WHERE complaint_id=:id").bind("id",complaint).mapTo(Integer.class).one(); }
    public List<Map<String,Object>> evidence(long complaint) { return handle.createQuery("SELECT asset_id,caption,created_at FROM complaint_evidence WHERE complaint_id=:id ORDER BY id").bind("id",complaint).mapToMap().list(); }
    public List<Map<String,Object>> messages(long complaint) { return handle.createQuery("SELECT m.author_role_snapshot,m.message_type,m.body,m.created_at,u.display_name author_name FROM complaint_messages m JOIN users u ON u.id=m.author_id WHERE m.complaint_id=:id ORDER BY m.id").bind("id",complaint).mapToMap().list(); }
    public List<Map<String,Object>> history(long complaint) { return handle.createQuery("SELECT s.from_status,s.to_status,s.reason,s.resolution_code_snapshot,s.resolution_summary_snapshot,s.created_at,u.display_name actor_name FROM complaint_status_history s LEFT JOIN users u ON u.id=s.actor_id WHERE s.complaint_id=:id ORDER BY s.id").bind("id",complaint).mapToMap().list(); }
}
