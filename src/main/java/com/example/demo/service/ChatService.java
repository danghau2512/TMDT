package com.example.demo.service;

import com.example.demo.config.Database;
import com.example.demo.dao.*;
import com.example.demo.model.*;
import com.example.demo.exception.ShopException;
import org.jdbi.v3.core.Handle;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static com.example.demo.service.ShopRules.*;

public final class ChatService {
    public static final int MAX_LENGTH=2000;
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy");
    private final Database db;
    public ChatService(Database db){this.db=db;}
    public long start(CurrentUser supplied,long product){return safe(()->db.transaction(h->{
        var actor=actor(h,supplied,true);var catalog=new CatalogDao(h);
        // Actor → product khóa chung với luồng sửa tin; UNIQUE là lớp bảo vệ thứ hai.
        catalog.lock(product);var p=catalog.product(product);long seller=id(p,"seller_id");
        require(actor.id()!=seller,400,"Bạn không thể chat với chính mình.");
        var dao=new ChatDao(h);var old=dao.existing(product,actor.id(),seller);
        if(old.isPresent()) return old.get();
        require(ProductService.publiclyVisible(p),404,"Sản phẩm không còn được công khai để bắt đầu trò chuyện.");
        return dao.create(product,actor.id(),seller,text(p,"title"));
    }));}
    public Map<String,Object> inbox(CurrentUser supplied){return safe(()->db.read(h->{var actor=actor(h,supplied,false);return inbox(h,actor);}));}
    public long unread(CurrentUser supplied){return safe(()->db.read(h->{var actor=actor(h,supplied,false);return new ChatDao(h).unread(actor.id());}));}
    private Map<String,Object> inbox(Handle h,CurrentUser actor){var dao=new ChatDao(h);return Map.of("conversations",dao.inbox(actor.id()).stream().map(this::conversation).toList(),"unreadTotal",dao.unread(actor.id()));}
    public Map<String,Object> thread(CurrentUser supplied,long conversation,long cursor,boolean older){
        require(cursor>=0,400,"Mốc tin nhắn không hợp lệ.");
        return safe(()->db.read(h->{var actor=actor(h,supplied,false);var dao=new ChatDao(h);
            var info=dao.info(conversation,actor.id()).orElseThrow(()->missing());
            var rows=new ArrayList<>(dao.messages(conversation,cursor,older));boolean more=rows.size()>50;
            if(more) rows.remove(50);if(older) Collections.reverse(rows);
            var result=new HashMap<String,Object>(inbox(h,actor));result.put("conversation",conversation(info));
            result.put("messages",rows.stream().map(r->message(r,actor.id())).toList());result.put("more",more);return result;
        }));
    }
    public ChatMessage send(CurrentUser supplied,long conversation,String raw,String nonce){
        String body=bounded(raw,1,MAX_LENGTH,"Tin nhắn");
        require(nonce!=null && nonce.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"),400,"Mã gửi tin không hợp lệ. Hãy mở lại trang.");
        return safe(()->db.transaction(h->{var actor=actor(h,supplied,true);var dao=new ChatDao(h);
            var chat=dao.lock(conversation,actor.id()).orElseThrow(()->missing());
            var previous=dao.retry(conversation,actor.id(),nonce);
            if(previous.isPresent()){require(body.equals(previous.get().get("body")),409,"Mã gửi đã dùng cho nội dung khác. Hãy mở lại trang.");return message(previous.get(),actor.id());}
            long peer=actor.id()==id(chat,"buyer_id")?id(chat,"seller_id"):id(chat,"buyer_id");
            // Không khóa peer ngược chiều actor để hai người trả lời đồng thời không deadlock.
            boolean active=h.createQuery("SELECT status='ACTIVE' FROM users WHERE id=:id").bind("id",peer).mapTo(Boolean.class).one();
            require(active,409,"Người còn lại đã ngừng hoạt động; bạn vẫn xem được lịch sử.");
            long id=dao.insert(conversation,actor.id(),nonce,body);dao.touch(conversation);
            return message(dao.retry(conversation,actor.id(),nonce).orElseThrow(),actor.id());
        }));
    }
    public long read(CurrentUser supplied,long conversation,long through){require(through>=0,400,"Mốc đã đọc không hợp lệ.");return safe(()->db.transaction(h->{
        var actor=actor(h,supplied,true);var dao=new ChatDao(h);dao.lock(conversation,actor.id()).orElseThrow(()->missing());
        if(through>0){require(dao.hasMessage(conversation,through),400,"Mốc đã đọc không thuộc cuộc trò chuyện.");dao.read(conversation,actor.id(),through);}
        return dao.unread(actor.id());
    }));}
    private Map<String,Object> conversation(Map<String,Object> row){
        boolean visible=ProductService.publiclyVisible(row);var result=new HashMap<String,Object>();
        result.put("id",Long.toString(id(row,"id")));result.put("productId",Long.toString(id(row,"product_id")));
        result.put("otherName",text(row,"other_name"));result.put("title",visible?text(row,"title"):text(row,"product_title_snapshot"));
        result.put("imagePath",visible && row.get("image_id")!=null?"/media/product?asset="+row.get("image_id"):"/assets/images/product-placeholder.svg");
        result.put("productPath",visible?"/products/detail?id="+id(row,"product_id"):"");
        result.put("available",visible);result.put("canSend","ACTIVE".equals(row.get("other_status")));
        result.put("lastBody",text(row,"last_body"));result.put("when",time(row.get("updated_at")));result.put("unread",row.get("unread"));return result;
    }
    private static ChatMessage message(Map<String,Object> row,long actor){return new ChatMessage(Long.toString(id(row,"id")),text(row,"body"),time(row.get("created_at")),id(row,"sender_id")==actor);}
    private static String time(Object value){
        LocalDateTime utc=value instanceof java.sql.Timestamp t?t.toLocalDateTime():(LocalDateTime)value;
        return TIME.format(utc.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")));
    }
    private static ShopException missing(){return new ShopException(404,"Không tìm thấy cuộc trò chuyện của bạn.");}
}
