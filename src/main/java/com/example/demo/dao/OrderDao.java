package com.example.demo.dao;

import org.jdbi.v3.core.Handle;
import com.example.demo.dto.CheckoutForm;
import com.example.demo.model.CurrentUser;
import java.math.BigDecimal;
import java.util.*;
import static com.example.demo.service.ShopRules.*;

public final class OrderDao {
    private final Handle h;
    public OrderDao(Handle h) { this.h=h; }
    public Optional<Map<String,Object>> batch(long buyer,String key) { return h.createQuery("SELECT * FROM checkout_batches WHERE buyer_id=:buyer AND idempotency_key=:key").bind("buyer",buyer).bind("key",key).mapToMap().findOne(); }
    public long createBatch(long buyer,String key,String hash) { return h.createUpdate("INSERT INTO checkout_batches(batch_code,buyer_id,idempotency_key,request_hash) VALUES(:code,:buyer,:key,:hash)").bind("code",UUID.randomUUID().toString()).bind("buyer",buyer).bind("key",key).bind("hash",hash).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one(); }
    public List<Map<String,Object>> batchOrders(long batch) { return h.createQuery("SELECT o.id,o.order_code,o.grand_total,o.status,p.method,p.status payment_status FROM orders o JOIN payments p ON p.order_id=o.id WHERE o.checkout_batch_id=:batch ORDER BY o.id").bind("batch",batch).mapToMap().list(); }
    public long createOrder(long batch,CurrentUser buyer,Map<String,Object> seller,CheckoutForm f,BigDecimal total) { return h.createUpdate("""
        INSERT INTO orders(order_code,checkout_batch_id,buyer_id,seller_id,recipient_name,recipient_phone,shipping_address,buyer_name_snapshot,seller_name_snapshot,seller_contact_snapshot,subtotal,grand_total)
        VALUES(:code,:batch,:buyer,:seller,:name,:phone,:address,:buyerName,:sellerName,:contact,:total,:total)
        """).bind("code",UUID.randomUUID().toString()).bind("batch",batch).bind("buyer",buyer.id()).bind("seller",id(seller,"seller_id"))
        .bind("name",f.name()).bind("phone",f.phone()).bind("address",f.address()).bind("buyerName",buyer.displayName()).bind("sellerName",text(seller,"seller_name")).bind("contact",seller.get("seller_contact")).bind("total",total).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one(); }
    public long item(long order,Map<String,Object> p) { return h.createUpdate("""
        INSERT INTO order_items(order_id,product_id,product_listing_version,product_name_snapshot,description_snapshot,condition_snapshot,category_name_snapshot,unit_price,quantity,line_total,appearance_snapshot,operation_snapshot,known_defects_snapshot,repair_snapshot,repair_details_snapshot,accessories_snapshot)
        VALUES(:order,:product,:version,:title,:description,:condition,:category,:price,:quantity,:line,:appearance,:operation,:defects,:repair,:repairDetails,:accessories)
        """).bind("order",order).bind("product",id(p,"id")).bind("version",id(p,"listing_version")).bind("title",text(p,"title")).bind("description",text(p,"description")).bind("condition",text(p,"condition_code"))
        .bind("appearance",p.get("appearance_code")).bind("operation",p.get("operation_code")).bind("defects",p.get("known_defects")).bind("repair",p.get("repair_code")).bind("repairDetails",p.get("repair_details")).bind("accessories",p.get("accessories")).bind("category",text(p,"category_name")).bind("price",money(p,"price")).bind("quantity",number(p,"quantity")).bind("line",money(p,"line_total")).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one(); }
    public void snapshotImages(long item,long product) { h.createUpdate("INSERT INTO order_item_images(order_item_id,asset_id,sort_order,alt_text_snapshot,is_defect_snapshot) SELECT :item,asset_id,sort_order,alt_text,is_defect FROM product_images WHERE product_id=:product").bind("item",item).bind("product",product).execute(); }
    public long createPayment(long order,String method,BigDecimal total) { return h.createUpdate("INSERT INTO payments(order_id,method,status,amount,reference_code) VALUES(:order,:method,:status,:amount,:reference)").bind("order",order).bind("method",method).bind("status","COD".equals(method)?"UNPAID":"PENDING_CONFIRMATION").bind("amount",total).bind("reference",UUID.randomUUID().toString()).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one(); }
    public void orderHistory(long order,String from,String to,CurrentUser actor,String reason) { h.createUpdate("INSERT INTO order_status_history(order_id,from_status,to_status,actor_id,actor_role_snapshot,reason) VALUES(:order,:from,:to,:actor,:role,:reason)").bind("order",order).bind("from",from).bind("to",to).bind("actor",actor.id()).bind("role",actor.role()).bind("reason",reason).execute(); }
    public void paymentHistory(long payment,String from,String to,CurrentUser actor,String note) { h.createUpdate("INSERT INTO payment_status_history(payment_id,from_status,to_status,actor_id,actor_role_snapshot,note) VALUES(:payment,:from,:to,:actor,:role,:note)").bind("payment",payment).bind("from",from).bind("to",to).bind("actor",actor.id()).bind("role",actor.role()).bind("note",note).execute(); }
    private static final String SELECT="SELECT o.*,p.method,p.status payment_status,p.amount payment_amount FROM orders o JOIN payments p ON p.order_id=o.id";
    public List<Map<String,Object>> list(long actor,String view,String status) { return h.createQuery(SELECT+" WHERE (:view='admin' OR (:view='buyer' AND o.buyer_id=:actor) OR (:view='seller' AND o.seller_id=:actor)) AND (:status='' OR o.status=:status) ORDER BY o.created_at DESC,o.id DESC LIMIT 200").bind("view",view).bind("actor",actor).bind("status",status).mapToMap().list(); }
    public Map<String,Object> order(long id,boolean lock) { return h.createQuery("SELECT * FROM orders WHERE id=:id"+(lock?" FOR UPDATE":"")).bind("id",id).mapToMap().findOne().orElseThrow(()->new com.example.demo.exception.ShopException(404,"Không tìm thấy đơn hàng.")); }
    public Map<String,Object> payment(long id,boolean lock) { return h.createQuery("SELECT * FROM payments WHERE order_id=:id"+(lock?" FOR UPDATE":"")).bind("id",id).mapToMap().one(); }
    public List<Map<String,Object>> items(long order) { return h.createQuery("SELECT * FROM order_items WHERE order_id=:order ORDER BY product_id").bind("order",order).mapToMap().list(); }
    public List<Map<String,Object>> images(long order) { return h.createQuery("SELECT i.* FROM order_item_images i JOIN order_items oi ON oi.id=i.order_item_id WHERE oi.order_id=:order ORDER BY i.sort_order").bind("order",order).mapToMap().list(); }
    public List<Map<String,Object>> history(long order) { return h.createQuery("SELECT oh.to_status,oh.actor_role_snapshot,oh.reason,oh.created_at,u.display_name actor_name FROM order_status_history oh LEFT JOIN users u ON u.id=oh.actor_id WHERE oh.order_id=:id ORDER BY oh.id").bind("id",order).mapToMap().list(); }
    public List<Map<String,Object>> paymentHistory(long order) { return h.createQuery("SELECT ph.to_status,ph.note,ph.actor_role_snapshot,ph.created_at,u.display_name actor_name FROM payment_status_history ph JOIN payments p ON p.id=ph.payment_id LEFT JOIN users u ON u.id=ph.actor_id WHERE p.order_id=:id ORDER BY ph.id").bind("id",order).mapToMap().list(); }
    public void status(long order,String next) { h.createUpdate("UPDATE orders SET status=:status,version=version+1,updated_at=CURRENT_TIMESTAMP(6),completed_at=CASE WHEN :status='COMPLETED' THEN CURRENT_TIMESTAMP(6) ELSE NULL END,cancelled_at=CASE WHEN :status='CANCELLED' THEN CURRENT_TIMESTAMP(6) ELSE NULL END WHERE id=:id").bind("id",order).bind("status",next).execute(); }
    public void payStatus(long payment,String status) { h.createUpdate("UPDATE payments SET status=:status,updated_at=CURRENT_TIMESTAMP(6),paid_at=CASE WHEN :status IN ('PAID','REFUND_PENDING') THEN COALESCE(paid_at,CURRENT_TIMESTAMP(6)) ELSE paid_at END,refunded_at=CASE WHEN :status='REFUND_SIMULATED' THEN CURRENT_TIMESTAMP(6) ELSE NULL END WHERE id=:id").bind("id",payment).bind("status",status).execute(); }
    public int openComplaints(long order) { return h.createQuery("SELECT COUNT(*) FROM complaints WHERE order_id=:id AND status IN ('RECEIVED','PROCESSING')").bind("id",order).mapTo(Integer.class).one(); }
}
