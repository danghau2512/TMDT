package com.example.demo.dao;

import org.jdbi.v3.core.Handle;
import java.util.*;

/** Một DAO/Handle mỗi callback; quyền tham gia được lọc cả trong SQL. */
public final class ChatDao {
    private final Handle h;
    public ChatDao(Handle h){this.h=h;}
    private static final String INFO="""
        SELECT cc.*,p.title,p.visibility,p.moderation_status,p.seller_id current_seller_id,
          owner.status seller_status,cat.status category_status,
          peer.display_name other_name,peer.status other_status,
          (SELECT asset_id FROM product_images WHERE product_id=p.id ORDER BY sort_order LIMIT 1) image_id,
          (SELECT body FROM chat_messages WHERE conversation_id=cc.id ORDER BY id DESC LIMIT 1) last_body,
          (SELECT COUNT(*) FROM chat_messages m WHERE m.conversation_id=cc.id AND m.sender_id<>:actor
            AND m.id>CASE WHEN cc.buyer_id=:actor THEN cc.buyer_read_id ELSE cc.seller_read_id END) unread
        FROM chat_conversations cc JOIN products p ON p.id=cc.product_id
        JOIN users owner ON owner.id=p.seller_id JOIN categories cat ON cat.id=p.category_id
        JOIN users peer ON peer.id=CASE WHEN cc.buyer_id=:actor THEN cc.seller_id ELSE cc.buyer_id END
        """;
    public List<Map<String,Object>> inbox(long actor){return h.createQuery(INFO+" WHERE cc.buyer_id=:actor OR cc.seller_id=:actor ORDER BY cc.updated_at DESC,cc.id DESC LIMIT 100").bind("actor",actor).mapToMap().list();}
    public Optional<Map<String,Object>> info(long id,long actor){return h.createQuery(INFO+" WHERE cc.id=:id AND (cc.buyer_id=:actor OR cc.seller_id=:actor)").bind("id",id).bind("actor",actor).mapToMap().findOne();}
    public Optional<Map<String,Object>> lock(long id,long actor){return h.createQuery("SELECT * FROM chat_conversations WHERE id=:id AND (buyer_id=:actor OR seller_id=:actor) FOR UPDATE").bind("id",id).bind("actor",actor).mapToMap().findOne();}
    public Optional<Long> existing(long product,long buyer,long seller){return h.createQuery("SELECT id FROM chat_conversations WHERE product_id=:product AND buyer_id=:buyer AND seller_id=:seller").bind("product",product).bind("buyer",buyer).bind("seller",seller).mapTo(Long.class).findOne();}
    public long create(long product,long buyer,long seller,String title){return h.createUpdate("INSERT INTO chat_conversations(product_id,buyer_id,seller_id,product_title_snapshot) VALUES(:product,:buyer,:seller,:title)").bind("product",product).bind("buyer",buyer).bind("seller",seller).bind("title",title).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();}
    public long unread(long actor){return h.createQuery("""
        SELECT COUNT(*) FROM chat_messages m JOIN chat_conversations cc ON cc.id=m.conversation_id
        WHERE (cc.buyer_id=:actor OR cc.seller_id=:actor) AND m.sender_id<>:actor
          AND m.id>CASE WHEN cc.buyer_id=:actor THEN cc.buyer_read_id ELSE cc.seller_read_id END
        """).bind("actor",actor).mapTo(Long.class).one();}
    public List<Map<String,Object>> messages(long id,long cursor,boolean older){
        // SQL động chỉ gồm các hằng nội bộ, cursor luôn bind.
        String sql=older?"SELECT * FROM chat_messages WHERE conversation_id=:id AND (:cursor=0 OR id<:cursor) ORDER BY id DESC LIMIT 51"
            :"SELECT * FROM chat_messages WHERE conversation_id=:id AND id>:cursor ORDER BY id LIMIT 51";
        return h.createQuery(sql).bind("id",id).bind("cursor",cursor).mapToMap().list();
    }
    public Optional<Map<String,Object>> retry(long conversation,long sender,String nonce){return h.createQuery("SELECT * FROM chat_messages WHERE conversation_id=:id AND sender_id=:sender AND client_nonce=:nonce").bind("id",conversation).bind("sender",sender).bind("nonce",nonce).mapToMap().findOne();}
    public long insert(long conversation,long sender,String nonce,String body){return h.createUpdate("INSERT INTO chat_messages(conversation_id,sender_id,client_nonce,body) VALUES(:id,:sender,:nonce,:body)").bind("id",conversation).bind("sender",sender).bind("nonce",nonce).bind("body",body).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();}
    public void touch(long conversation){h.createUpdate("UPDATE chat_conversations SET updated_at=CURRENT_TIMESTAMP(6) WHERE id=:id").bind("id",conversation).execute();}
    public boolean hasMessage(long conversation,long through){return h.createQuery("SELECT COUNT(*) FROM chat_messages WHERE conversation_id=:id AND id=:through").bind("id",conversation).bind("through",through).mapTo(Integer.class).one()==1;}
    public void read(long conversation,long actor,long through){h.createUpdate("""
        UPDATE chat_conversations SET
          buyer_read_id=CASE WHEN buyer_id=:actor THEN GREATEST(buyer_read_id,:through) ELSE buyer_read_id END,
          seller_read_id=CASE WHEN seller_id=:actor THEN GREATEST(seller_read_id,:through) ELSE seller_read_id END
        WHERE id=:id
        """).bind("id",conversation).bind("actor",actor).bind("through",through).execute();}
}
