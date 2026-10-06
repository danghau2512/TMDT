package com.example.demo.service;

import com.example.demo.config.Database;
import com.example.demo.dao.*;
import com.example.demo.dto.CheckoutForm;
import com.example.demo.model.CurrentUser;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import static com.example.demo.service.ShopRules.*;

public final class OrderService {
    private final Database db;
    public OrderService(Database db) { this.db=db; }
    public long checkout(CurrentUser supplied,CheckoutForm input) {
        String name=bounded(input.name(),2,100,"Tên người nhận"),phone=bounded(input.phone(),9,16,"Điện thoại"),address=bounded(input.address(),10,500,"Địa chỉ"),note=bounded(input.note(),0,1000,"Ghi chú");
        require(phone.matches("\\+?[0-9]{9,15}"),400,"Số điện thoại không hợp lệ.");
        require(Set.of("COD","VNPAY_SANDBOX").contains(input.method()),400,"Phương thức thanh toán không hợp lệ.");
        try { require(UUID.fromString(input.key()).toString().equals(input.key()),400,"Mã đặt hàng không hợp lệ."); }
        catch(IllegalArgumentException | NullPointerException exception) { throw new com.example.demo.exception.ShopException(400,"Mã đặt hàng không hợp lệ. Hãy mở lại trang đặt hàng."); }
        require(input.quote()!=null && input.quote().matches("[a-f0-9]{64}"),400,"Cần xem lại giỏ hàng trước khi đặt.");
        var form=new CheckoutForm(name,phone,address,note,input.method(),input.key(),input.quote());
        String hash=digest(List.of(name,phone,address,note,form.method(),form.quote()).stream().map(v->v.length()+":"+v).collect(Collectors.joining("|")));
        return safe(()->db.transaction(h->{ var buyer=actor(h,supplied,true); var orders=new OrderDao(h); var previous=orders.batch(buyer.id(),form.key());
            if(previous.isPresent()) { require(hash.equals(previous.get().get("request_hash")),409,"Mã đặt hàng đã dùng cho nội dung khác."); return id(previous.get(),"id"); }
            var cartDao=new CartDao(h); var cart=cartDao.cart(buyer.id(),true).orElseThrow(()->new com.example.demo.exception.ShopException(400,"Giỏ hàng đang trống."));
            long cartId=id(cart,"id"); var initial=cartDao.items(cartId); require(!initial.isEmpty() && initial.size()<=50,400,"Giỏ hàng đang trống hoặc vượt giới hạn.");
            var products=new CatalogDao(h);
            // Khóa sản phẩm theo ID tăng dần, tránh hai checkout khóa ngược thứ tự.
            for(var p:initial) products.lock(id(p,"id"));
            var items=cartDao.items(cartId); var summary=CartService.summary(items,id(cart,"version"),buyer.id());
            require(summary.quote().equals(form.quote()),409,"Giá, nội dung hoặc giỏ hàng đã thay đổi. Hãy xem lại trước khi đặt.");
            for(var p:items) CartService.purchasable(p,buyer.id(),number(p,"quantity"));
            long batch=orders.createBatch(buyer.id(),form.key(),hash);
            var groups=new LinkedHashMap<Long,List<Map<String,Object>>>();
            for(var p:items) groups.computeIfAbsent(id(p,"seller_id"),ignored->new ArrayList<>()).add(p);
            for(var group:groups.values()) {
                BigDecimal total=group.stream().map(p->money(p,"line_total")).reduce(BigDecimal.ZERO,BigDecimal::add);
                if("VNPAY_SANDBOX".equals(form.method())) {
                    try { com.example.demo.payment.VnpayProtocol.amount(total); }
                    catch(ArithmeticException | IllegalArgumentException e) { throw new com.example.demo.exception.ShopException(400,"Một đơn vượt giới hạn VNPAY Sandbox (tối đa 9.999.999.999 đồng). Hãy giảm giỏ hoặc chọn COD."); }
                }
                long order=orders.createOrder(batch,buyer,group.get(0),form,total);
                for(var p:group) {
                    long item=orders.item(order,p); orders.snapshotImages(item,id(p,"id"));
                    int before=number(p,"stock_quantity"),after=before-number(p,"quantity");
                    products.stock(id(p,"id"),after); products.ledger(id(p,"id"),item,"ORDER_HOLD",before,after,buyer.id(),"Giữ số lượng cho đơn hàng");
                }
                orders.orderHistory(order,null,"PENDING",buyer,note.isEmpty()?"Đặt hàng":"Ghi chú của người mua: "+note);
                long payment=orders.createPayment(order,form.method(),total);
                orders.paymentHistory(payment,null,"COD".equals(form.method())?"UNPAID":"PENDING_CONFIRMATION",buyer,"COD".equals(form.method())?"Chờ thu COD khi giao hàng":"Chờ thanh toán VNPAY Sandbox – môi trường thử nghiệm");
            }
            cartDao.clear(cartId); return batch;
        }));
    }
    public List<Map<String,Object>> receipt(CurrentUser supplied,long batch) { return safe(()->db.read(h->{ var actor=actor(h,supplied,false);
        require(h.createQuery("SELECT COUNT(*) FROM checkout_batches WHERE id=:id AND buyer_id=:buyer").bind("id",batch).bind("buyer",actor.id()).mapTo(Integer.class).one()==1,404,"Không tìm thấy lần đặt hàng của bạn.");
        return new OrderDao(h).batchOrders(batch); })); }
    public List<Map<String,Object>> list(CurrentUser supplied,String view,String status) {
        require(status.isEmpty() || Set.of("PENDING","CONFIRMED","SHIPPED","DELIVERED","COMPLETED","CANCELLED").contains(status),400,"Trạng thái đơn không hợp lệ.");
        return safe(()->db.read(h->{ var actor=actor(h,supplied,false); checkView(actor,view); return new OrderDao(h).list(actor.id(),view,status); }));
    }
    public Map<String,Object> detail(CurrentUser supplied,long order,String view) { return safe(()->db.read(h->{ var actor=actor(h,supplied,false); var dao=new OrderDao(h); var o=dao.order(order,false); access(actor,o,view);
        var payment=dao.payment(order,false); String status=text(o,"status"); var actions=new ArrayList<String>();
        var complaint=new ComplaintDao(h).referenceByOrder(order).orElse(Map.of());
        boolean blocked=!complaint.isEmpty() && Set.of("RECEIVED","PROCESSING").contains(complaint.get("status"));
        if("buyer".equals(view)) { if(Set.of("PENDING","CONFIRMED").contains(status)) actions.add("CANCEL"); if("DELIVERED".equals(status) && !blocked) actions.add("COMPLETE"); }
        else { if("PENDING".equals(status)) { actions.add("CONFIRM"); actions.add("CANCEL"); } if("CONFIRMED".equals(status)) { actions.add("SHIP"); if("admin".equals(view)) actions.add("CANCEL"); } if("SHIPPED".equals(status)) actions.add("DELIVER"); }
        if(Set.of("PENDING","CONFIRMED").contains(status) && "BANK_TRANSFER_SIMULATED".equals(payment.get("method")) && "PENDING_CONFIRMATION".equals(payment.get("status"))) actions.add("PAY");
        var data=new HashMap<String,Object>();
        data.putAll(Map.of("order",o,"payment",payment,"items",dao.items(order),"images",dao.images(order),"history",dao.history(order),"paymentHistory",dao.paymentHistory(order),"actions",actions,"reviewsByItem","buyer".equals(view)?new ReviewDao(h).byOrder(order):Map.of(),"complaint",complaint,"completionBlocked",blocked));
        var attempts="VNPAY_SANDBOX".equals(payment.get("method"))?new VnpayDao(h).history(order):List.<Map<String,Object>>of();
        data.put("attempts",attempts);data.put("canVnpay", "buyer".equals(view) && Set.of("PENDING","CONFIRMED").contains(status) && "VNPAY_SANDBOX".equals(payment.get("method")) && "PENDING_CONFIRMATION".equals(payment.get("status")) && attempts.stream().noneMatch(a->"NEEDS_REVIEW".equals(a.get("status"))));
        return data; })); }
    public void action(CurrentUser supplied,long order,String view,String action,String inputReason) {
        require(Set.of("CONFIRM","SHIP","DELIVER","COMPLETE","CANCEL","PAY").contains(action),400,"Hành động không hợp lệ.");
        safe(()->db.transaction(h->{ var actor=actor(h,supplied,true); var dao=new OrderDao(h); var o=dao.order(order,true); access(actor,o,view); var payment=dao.payment(order,true);
            String status=text(o,"status"),reason=bounded(AccountValidation.text(inputReason),0,500,"Lý do xử lý");
            if("admin".equals(view) || "CANCEL".equals(action)) reason=bounded(reason,1,500,"Lý do xử lý");
            else if(reason.isEmpty()) reason="Thao tác "+action;
            if("PAY".equals(action)) {
                require("BANK_TRANSFER_SIMULATED".equals(payment.get("method")),409,"COD được ghi thanh toán khi giao hàng.");
                if("PAID".equals(payment.get("status"))) return null;
                require(Set.of("PENDING","CONFIRMED").contains(status) && "PENDING_CONFIRMATION".equals(payment.get("status")),409,"Đơn không đủ điều kiện thanh toán mô phỏng.");
                pay(dao,payment,actor,"PAID","Thanh toán chuyển khoản mô phỏng; không chuyển tiền thật. "+reason);
                if("admin".equals(view)) new AuditDao(h).order(actor.id(),order,"PAYMENT_SIMULATE",reason,"{\"payment\":\"PENDING_CONFIRMATION\"}","{\"payment\":\"PAID\"}");
                return null;
            }
            String target=switch(action) { case "CONFIRM"->"CONFIRMED"; case "SHIP"->"SHIPPED"; case "DELIVER"->"DELIVERED"; case "COMPLETE"->"COMPLETED"; default->"CANCELLED"; };
            if("COMPLETE".equals(action)) require("buyer".equals(view) && actor.id()==id(o,"buyer_id"),403,"Chỉ người mua được xác nhận nhận hàng.");
            else if(!"CANCEL".equals(action)) require(!"buyer".equals(view),403,"Chỉ người bán hoặc Admin xử lý giao hàng.");
            if(status.equals(target)) return null;
            switch(action) {
                case "CONFIRM" -> require("PENDING".equals(status),409,"Chỉ xác nhận đơn đang chờ xác nhận.");
                case "SHIP" -> { require("CONFIRMED".equals(status),409,"Đơn phải được xác nhận trước khi giao."); require("COD".equals(payment.get("method")) || "PAID".equals(payment.get("status")),409,"Cần xác nhận thanh toán trước khi giao đơn trả trước."); }
                case "DELIVER" -> { require("SHIPPED".equals(status),409,"Đơn chưa trong trạng thái đang giao."); if("COD".equals(payment.get("method"))) pay(dao,payment,actor,"PAID","Mô phỏng thu COD khi giao hàng"); }
                case "COMPLETE" -> { require("DELIVERED".equals(status) && "PAID".equals(payment.get("status")),409,"Chỉ xác nhận nhận hàng khi đã giao và thanh toán."); require(dao.openComplaints(order)==0,409,"Đơn đang có khiếu nại cần xử lý trước."); }
                case "CANCEL" -> {
                    require("PENDING".equals(status) || "CONFIRMED".equals(status) && !"seller".equals(view),409,"Không được hủy ở trạng thái này.");
                    var products=new CatalogDao(h);
                    for(var item:dao.items(order)) { long product=id(item,"product_id"); var p=products.lock(product); int before=number(p,"stock_quantity"); long after=(long)before+number(item,"quantity");
                        require(after<=Integer.MAX_VALUE,409,"Số lượng hoàn vượt giới hạn."); products.stock(product,(int)after); products.ledger(product,id(item,"id"),"CANCEL_RELEASE",before,(int)after,actor.id(),reason); }
                    String refund="VNPAY_SANDBOX".equals(payment.get("method"))?"REFUND_PENDING":"REFUND_SIMULATED";
                    pay(dao,payment,actor,"PAID".equals(payment.get("status"))?refund:"VOIDED","Hủy đơn: "+reason+("VNPAY_SANDBOX".equals(payment.get("method"))?". Chưa thực hiện hoàn tiền qua VNPAY.":""));
                }
                default -> throw new IllegalStateException("Hành động không hợp lệ.");
            }
            dao.status(order,target); dao.orderHistory(order,status,target,actor,reason);
            if("admin".equals(view)) new AuditDao(h).order(actor.id(),order,"ORDER_"+action,reason,"{\"status\":\""+status+"\"}","{\"status\":\""+target+"\"}");
            return null; }));
    }
    private static void pay(OrderDao dao,Map<String,Object> payment,CurrentUser actor,String next,String note) {
        String previous=text(payment,"status"); if(previous.equals(next)) return;
        dao.payStatus(id(payment,"id"),next); dao.paymentHistory(id(payment,"id"),previous,next,actor,note);
    }
    private static void checkView(CurrentUser actor,String view) { require(Set.of("buyer","seller","admin").contains(view),400,"Loại danh sách không hợp lệ."); require(!"admin".equals(view) || actor.isAdmin(),403,"Bạn không có quyền quản trị."); }
    private static void access(CurrentUser actor,Map<String,Object> o,String view) { checkView(actor,view); require("admin".equals(view) || actor.id()==id(o,"buyer".equals(view)?"buyer_id":"seller_id"),404,"Không tìm thấy đơn hàng của bạn."); }
}
