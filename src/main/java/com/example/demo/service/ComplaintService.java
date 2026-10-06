package com.example.demo.service;

import com.example.demo.config.Database;
import com.example.demo.dao.*;
import com.example.demo.model.*;
import com.example.demo.storage.ImageStorage;
import java.util.*;
import static com.example.demo.service.ShopRules.*;

public final class ComplaintService {
    private static final Map<String,String> REASONS = Map.of(
            "NOT_AS_DESCRIBED","Hàng không đúng mô tả", "NOT_RECEIVED","Chưa nhận được hàng",
            "DAMAGED","Hàng bị lỗi", "OTHER","Lý do khác");
    private final Database database;
    private final ImageStorage storage;
    public ComplaintService(Database database, ImageStorage storage) { this.database=database; this.storage=storage; }

    public Map<String,Object> form(CurrentUser supplied, long order) {
        return safe(() -> database.read(handle -> {
            var buyer=actor(handle,supplied,false);
            var orderData=new OrderDao(handle).order(order,false);
            require(id(orderData,"buyer_id")==buyer.id(),404,"Không tìm thấy đơn mua của bạn.");
            return Map.of("order",orderData,"complaint",new ComplaintDao(handle).byOrder(order).orElse(Map.of()));
        }));
    }
    public ComplaintResult create(CurrentUser supplied, long order, String reason, String input, List<StoredImage> images) {
        try {
            require(REASONS.containsKey(reason),400,"Lý do khiếu nại không hợp lệ.");
            String content=bounded(input,10,5000,"Nội dung khiếu nại");
            require(images.size()<=5,400,"Tối đa 5 ảnh cho một lần gửi.");
            var result=safe(() -> database.transaction(handle -> {
                var buyer=actor(handle,supplied,true);
                // Khóa order trước complaint; COMPLETE cũng giữ khóa order này.
                var orderData=new OrderDao(handle).order(order,true);
                require(id(orderData,"buyer_id")==buyer.id(),404,"Không tìm thấy đơn mua của bạn.");
                var dao=new ComplaintDao(handle);
                var previous=dao.byOrder(order);
                if(previous.isPresent()) return new ComplaintResult(id(previous.get(),"id"),false);
                long complaint=dao.insert(order,buyer,reason,REASONS.get(reason),content);
                new MediaDao(handle).addEvidence(complaint,buyer.id(),images);
                dao.history(complaint,null,"RECEIVED",buyer,"Người mua gửi khiếu nại",null,null);
                return new ComplaintResult(complaint,true);
            }));
            if(!result.created()) storage.discard(images);
            return result;
        } catch(RuntimeException exception) { storage.discard(images); throw exception; }
    }
    public List<Map<String,Object>> list(CurrentUser supplied, boolean admin, String status) {
        return (List<Map<String,Object>>)center(supplied,admin,status,"").get("complaints");
    }
    public Map<String,Object> center(CurrentUser supplied,boolean admin,String status,String input) {
        require(status.isEmpty() || Set.of("RECEIVED","PROCESSING","RESOLVED").contains(status),400,"Trạng thái khiếu nại không hợp lệ.");
        String query=bounded(AccountValidation.text(input),0,100,"Mã đơn tìm kiếm");
        return safe(() -> database.read(handle -> {
            var user=actor(handle,supplied,false); access(user,null,admin);
            var dao=new ComplaintDao(handle);return Map.of("complaints",dao.list(user.id(),admin,status,query),"complaintStats",dao.stats(user.id(),admin));
        }));
    }
    public Map<String,Object> detail(CurrentUser supplied, long complaint, boolean admin) {
        return safe(() -> database.read(handle -> {
            var user=actor(handle,supplied,false); var dao=new ComplaintDao(handle);
            var data=dao.get(complaint,false); access(user,data,admin);
            var orders=new OrderDao(handle); long order=id(data,"order_id");
            return Map.of("complaint",data,"order",orders.order(order,false),"payment",orders.payment(order,false),
                    "items",orders.items(order),"images",orders.images(order),"orderHistory",orders.history(order),
                    "paymentHistory",orders.paymentHistory(order),"evidence",dao.evidence(complaint),
                    "messages",dao.messages(complaint),"complaintHistory",dao.history(complaint));
        }));
    }
    public void supplement(CurrentUser supplied, long complaint, String input, List<StoredImage> images) {
        try {
            String body=bounded(input,1,5000,"Nội dung bổ sung");
            require(images.size()<=5,400,"Tối đa 5 ảnh cho một lần gửi.");
            safe(() -> database.transaction(handle -> {
                var buyer=actor(handle,supplied,true); var dao=new ComplaintDao(handle);
                var reference=dao.get(complaint,false); access(buyer,reference,false);
                long order=id(reference,"order_id"); new OrderDao(handle).order(order,true);
                var data=dao.get(complaint,true); access(buyer,data,false);
                require(!"RESOLVED".equals(data.get("status")),409,"Hồ sơ đã xử lý; chỉ bổ sung khi hồ sơ đang mở.");
                require(dao.evidenceCount(complaint)+images.size()<=20,400,"Mỗi hồ sơ tối đa 20 ảnh minh chứng.");
                dao.message(complaint,buyer,"BUYER_MESSAGE",body);
                new MediaDao(handle).addEvidence(complaint,buyer.id(),images); dao.touched(complaint);
                return null;
            }));
        } catch(RuntimeException exception) { storage.discard(images); throw exception; }
    }
    public void act(CurrentUser supplied, long complaint, String action, String input, String resolution, String inputSummary) {
        require(Set.of("PROCESS","RESPOND","RESOLVE","REOPEN").contains(action),400,"Thao tác khiếu nại không hợp lệ.");
        String response=bounded(input,1,5000,"Phản hồi / lý do xử lý");
        String summary=AccountValidation.text(inputSummary);
        if("RESOLVE".equals(action)) {
            require(Set.of("SELLER_CONTACTED","ORDER_CANCELLED","NO_ACTION","OTHER").contains(resolution),400,"Chọn kết quả xử lý.");
            summary=bounded(summary,1,5000,"Kết quả xử lý");
        }
        String outcome=summary;
        safe(() -> database.transaction(handle -> {
            var admin=actor(handle,supplied,true); require(admin.isAdmin(),403,"Bạn không có quyền xử lý khiếu nại.");
            var dao=new ComplaintDao(handle); long order=id(dao.get(complaint,false),"order_id");
            var orderData=new OrderDao(handle).order(order,true); var data=dao.get(complaint,true);
            String previous=text(data,"status"),next=previous,type="ADMIN_RESPONSE";
            String code=null,result=null;
            switch(action) {
                case "PROCESS" -> {
                    if("PROCESSING".equals(previous)) return null;
                    require("RECEIVED".equals(previous),409,"Chỉ tiếp nhận xử lý từ hồ sơ đã tiếp nhận."); next="PROCESSING";
                }
                case "RESPOND" -> require(!"RESOLVED".equals(previous),409,"Cần mở lại hồ sơ trước khi phản hồi thêm.");
                case "RESOLVE" -> {
                    if("RESOLVED".equals(previous)) return null;
                    require("PROCESSING".equals(previous),409,"Cần chuyển sang đang xử lý trước khi kết thúc.");
                    require(!"ORDER_CANCELLED".equals(resolution) || "CANCELLED".equals(orderData.get("status")),409,"Chỉ ghi kết quả đã hủy khi đơn thực sự đã hủy.");
                    next="RESOLVED"; type="RESOLUTION"; code=resolution; result=outcome;
                }
                case "REOPEN" -> {
                    if("PROCESSING".equals(previous)) return null;
                    require("RESOLVED".equals(previous),409,"Chỉ mở lại hồ sơ đã xử lý."); next="PROCESSING"; type="REOPEN";
                }
                default -> throw new IllegalStateException("Thao tác không hợp lệ.");
            }
            dao.message(complaint,admin,type,response);
            if(!previous.equals(next)) {
                // Giữ kết quả cũ trong history khi mở lại; cột hiện tại tuân theo CHECK của V001.
                String historyCode="REOPEN".equals(action)?text(data,"resolution_code"):code;
                String historySummary="REOPEN".equals(action)?text(data,"resolution_summary"):result;
                dao.history(complaint,previous,next,admin,response,historyCode,historySummary);
            }
            dao.state(complaint,admin,next,code,result);
            new AuditDao(handle).complaint(admin.id(),complaint,"COMPLAINT_"+action,
                    "Xử lý hồ sơ khiếu nại: "+action,previous,next);
            // Đổi complaint không đổi order/payment/stock; buyer tự xác nhận nhận hàng.
            return null;
        }));
    }
    public Map<String,Object> evidence(CurrentUser supplied, long complaint, long asset) {
        return safe(() -> database.read(handle -> {
            var user=actor(handle,supplied,false); var data=new ComplaintDao(handle).get(complaint,false);
            require(user.isAdmin() || user.id()==id(data,"buyer_id"),404,"Không tìm thấy ảnh minh chứng của bạn.");
            return new MediaDao(handle).evidence(asset,complaint).orElseThrow(() -> new com.example.demo.exception.ShopException(404,"Không tìm thấy ảnh minh chứng."));
        }));
    }
    private static void access(CurrentUser user, Map<String,Object> data, boolean admin) {
        require(!admin || user.isAdmin(),403,"Bạn không có quyền quản trị.");
        if(data!=null) require(admin || user.id()==id(data,"buyer_id"),404,"Không tìm thấy khiếu nại của bạn.");
    }
}
