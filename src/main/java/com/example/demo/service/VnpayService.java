package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.dao.*;
import com.example.demo.model.*;
import com.example.demo.payment.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.example.demo.service.ShopRules.*;

public final class VnpayService {
    private final Database db;
    private final Optional<VnpayConfig> config;
    private final VnpayGateway gateway;
    public VnpayService(Database db,Optional<VnpayConfig> config){this(db,config,config.<VnpayGateway>map(VnpayHttpClient::new).orElse(null));}
    public VnpayService(Database db,Optional<VnpayConfig> config,VnpayGateway gateway){this.db=db;this.config=config;this.gateway=gateway;}
    public boolean configured(){return config.isPresent();}
    private VnpayConfig configuredConfig(){require(configured(),503,"Chưa cấu hình VNPAY Sandbox phía backend. Kiểm tra file local của Tomcat.");return config.orElseThrow();}
    private static void buyer(CurrentUser actor,Map<String,Object> order){require(actor.id()==id(order,"buyer_id"),404,"Không tìm thấy giao dịch của bạn.");}
    private static void reference(String ref){require(ref!=null && ref.matches("[a-f0-9]{32}"),400,"Mã tham chiếu không hợp lệ.");}
    public String create(CurrentUser supplied,long order,String ip){
        var cfg=configuredConfig();require(ip!=null && ip.matches("[0-9a-fA-F:.]{3,45}"),400,"Địa chỉ IP không hợp lệ.");
        var attempt=safe(()->db.transaction(h->{var actor=actor(h,supplied,true);var orders=new OrderDao(h);var o=orders.order(order,true);buyer(actor,o);var p=orders.payment(order,true);
            require("VNPAY_SANDBOX".equals(p.get("method")),409,"Đơn này không dùng VNPAY Sandbox.");
            require(Set.of("PENDING","CONFIRMED").contains(text(o,"status")) && "PENDING_CONFIRMATION".equals(p.get("status")),409,"Đơn đã thanh toán, đã hủy hoặc không còn đủ điều kiện thanh toán.");
            require(money(o,"grand_total").compareTo(money(p,"amount"))==0,409,"Số tiền đơn và thanh toán không khớp.");
            try{VnpayProtocol.amount(money(p,"amount"));}catch(ArithmeticException | IllegalArgumentException e){throw new com.example.demo.exception.ShopException(400,"Số tiền đơn vượt giới hạn VNPAY Sandbox (tối đa 9.999.999.999 đồng).");}
            var dao=new VnpayDao(h);var active=dao.active(order);
            if(active.isPresent()){
                var a=active.get();if(!dao.expired(text(a,"txn_ref"))){require(cfg.merchant().equals(a.get("merchant_code")),409,"Lần thanh toán đang mở thuộc cấu hình merchant trước. Cần kiểm tra trước khi thử lại.");return a;}
                dao.state(text(a,"txn_ref"),"EXPIRED");
            }
            require(dao.history(order).stream().noneMatch(a->"NEEDS_REVIEW".equals(a.get("status"))),409,"Có giao dịch cần đối chiếu. Vui lòng liên hệ Admin trước khi thanh toán lại.");
            Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS),deadline=now.plusSeconds(cfg.expiryMinutes()*60L);
            return dao.insert(order,UUID.randomUUID().toString().replace("-",""),cfg.merchant(),money(p,"amount"),VnpayProtocol.date(now),VnpayProtocol.date(deadline),deadline,ip,cfg.returnUrl());
        }));return VnpayProtocol.paymentUrl(cfg,attempt);
    }
    public Map<String,Object> view(CurrentUser supplied,String ref){reference(ref);return safe(()->db.read(h->{var actor=actor(h,supplied,false);var dao=new VnpayDao(h);var a=dao.byRef(ref,false).orElseThrow(()->new com.example.demo.exception.ShopException(404,"Không tìm thấy giao dịch."));
        var orders=new OrderDao(h);var o=orders.order(id(a,"order_id"),false);buyer(actor,o);return model(o,orders.payment(id(o,"id"),false),a,dao.history(id(o,"id")));}));}
    private Map<String,Object> model(Map<String,Object> o,Map<String,Object> p,Map<String,Object> a,List<Map<String,Object>> history){
        boolean unpaid="PENDING_CONFIRMATION".equals(p.get("status")),eligible=Set.of("PENDING","CONFIRMED").contains(text(o,"status"));
        return Map.of("order",o,"payment",p,"attempt",a,"attempts",history,"canPay",configured() && unpaid && eligible && history.stream().noneMatch(x->"NEEDS_REVIEW".equals(x.get("status"))),"canQuery",configured() && !Set.of("SUCCEEDED","NEEDS_REVIEW").contains(text(a,"status")));
    }
    public void validateReturn(Map<String,String> params){var cfg=configuredConfig();require(VnpayProtocol.verify(cfg,params),400,"Chữ ký kết quả thanh toán không hợp lệ. Hãy mở đơn để kiểm tra trực tiếp.");require(cfg.merchant().equals(params.get("vnp_TmnCode")),400,"Merchant của kết quả không khớp.");reference(params.get("vnp_TxnRef"));}
    public VnpayAcknowledgement ipn(Map<String,String> params){
        try{var cfg=configuredConfig();if(!VnpayProtocol.verify(cfg,params) || !cfg.merchant().equals(params.get("vnp_TmnCode")))return VnpayAcknowledgement.of("97");return apply(params,"IPN",null);}
        catch(RuntimeException e){return VnpayAcknowledgement.of("99");}
    }
    public String query(CurrentUser supplied,String ref,String serverIp){
        var cfg=configuredConfig();reference(ref);
        // Không giữ Handle/khóa DB trong lúc chờ HTTP gateway; kiểm quyền lại khi áp kết quả.
        var attempt=safe(()->db.transaction(h->{var actor=actor(h,supplied,true);var dao=new VnpayDao(h);var a=dao.byRef(ref,false).orElseThrow(()->new com.example.demo.exception.ShopException(404,"Không tìm thấy giao dịch."));
            var o=new OrderDao(h).order(id(a,"order_id"),true);buyer(actor,o);require(cfg.merchant().equals(a.get("merchant_code")),409,"Merchant cấu hình không khớp lần thanh toán.");
            require(!Set.of("SUCCEEDED","NEEDS_REVIEW").contains(text(a,"status")),409,"Giao dịch đã xác nhận; xem trạng thái lưu trong đơn.");
            require(dao.reserveQuery(ref),429,"Vui lòng đợi 30 giây trước khi kiểm tra lại.");return a;}));
        Map<String,String> response;
        try{response=gateway.query(VnpayProtocol.queryRequest(cfg,attempt,serverIp,Instant.now()));}
        catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();if(e instanceof javax.net.ssl.SSLHandshakeException)throw new com.example.demo.exception.ShopException(503,"Java chưa xác thực được chứng chỉ HTTPS VNPAY. Kiểm tra truststore theo hướng dẫn demo; thanh toán được giữ nguyên.");throw new com.example.demo.exception.ShopException(503,"Chưa kết nối được API truy vấn VNPAY. Trạng thái đơn được giữ nguyên; thử lại sau.");}
        // Sandbox có thể trả lỗi API (ví dụ 94) chỉ gồm code/message, không có checksum.
        // Chỉ đưa thông báo tĩnh; không dùng phản hồi lỗi này làm kết quả tài chính.
        String apiCode=response.getOrDefault("vnp_ResponseCode","");
        if(Set.of("02","03","91","94","97","99").contains(apiCode))return "API VNPAY chưa xác nhận (mã "+apiCode+"). "+("94".equals(apiCode)?"Gateway giới hạn truy vấn lặp; đợi vài phút rồi kiểm tra lại. ":"")+"Không cập nhật thanh toán.";
        require(VnpayProtocol.verifyQuery(cfg,response),502,"Chưa nhận được phản hồi querydr có chữ ký hợp lệ; không cập nhật thanh toán.");
        require(cfg.merchant().equals(response.get("vnp_TmnCode")),502,"Merchant phản hồi querydr không khớp.");
        if(!"00".equals(response.get("vnp_ResponseCode")))return "VNPAY chưa xác nhận giao dịch (API mã "+response.getOrDefault("vnp_ResponseCode","không xác định").replaceAll("[^0-9]","")+ "). Không thay đổi thanh toán; hãy thử kiểm tra lại sau.";
        require(ref.equals(response.get("vnp_TxnRef")) && "querydr".equals(response.get("vnp_Command")) && "01".equals(response.get("vnp_TransactionType")),502,"Phản hồi querydr không khớp giao dịch thanh toán.");
        var ack=safe(()->apply(response,"QUERYDR",supplied));require(Set.of("00","02").contains(ack.RspCode()),502,"Không thể đối chiếu phản hồi querydr với số tiền/giao dịch đã lưu.");
        return "Đã đối chiếu kết quả từ VNPAY. Trạng thái bên dưới lấy từ database.";
    }
    private static boolean fieldsValid(Map<String,String> f){
        if(!f.getOrDefault("vnp_ResponseCode","").matches("[0-9]{2}") || !f.getOrDefault("vnp_TransactionStatus","").matches("[0-9]{2}"))return false;
        if(!f.getOrDefault("vnp_TxnRef","").matches("[a-f0-9]{32}"))return false;
        String bank=f.getOrDefault("vnp_BankCode",""),date=f.getOrDefault("vnp_PayDate",""),txn=f.getOrDefault("vnp_TransactionNo","");
        return bank.matches("[A-Za-z0-9]{0,20}") && (date.isEmpty() || VnpayProtocol.validDate(date)) && txn.matches("[0-9]{0,15}");
    }
    private VnpayAcknowledgement apply(Map<String,String> f,String source,CurrentUser supplied){
        if(!fieldsValid(f))return VnpayAcknowledgement.of("99");
        return db.transaction(h->{if(supplied!=null)actor(h,supplied,true);var attempts=new VnpayDao(h);var found=attempts.byRef(f.get("vnp_TxnRef"),false);if(found.isEmpty())return VnpayAcknowledgement.of("01");
            var orders=new OrderDao(h);var order=orders.order(id(found.get(),"order_id"),true);if(supplied!=null)buyer(supplied,order);var payment=orders.payment(id(order,"id"),true);var a=attempts.byRef(f.get("vnp_TxnRef"),true).orElseThrow();
            if(!Objects.equals(a.get("merchant_code"),f.get("vnp_TmnCode")) || !"VNPAY_SANDBOX".equals(payment.get("method")))return VnpayAcknowledgement.of("97");
            String amount=f.getOrDefault("vnp_Amount","");if(!amount.matches("[0-9]{1,12}") || new BigDecimal(amount).compareTo(new BigDecimal(VnpayProtocol.amount(money(a,"expected_amount"))))!=0 || money(a,"expected_amount").compareTo(money(payment,"amount"))!=0 || money(payment,"amount").compareTo(money(order,"grand_total"))!=0)return VnpayAcknowledgement.of("04");
            String ref=text(a,"txn_ref"),state=text(a,"status"),previous=text(payment,"status"),ts=f.get("vnp_TransactionStatus");
            if(Set.of("SUCCEEDED","NEEDS_REVIEW").contains(state))return VnpayAcknowledgement.of("02");
            boolean success="00".equals(f.get("vnp_ResponseCode")) && "00".equals(ts);
            if(success){
                if(!f.getOrDefault("vnp_TransactionNo","").matches("[0-9]{1,15}") || new java.math.BigInteger(f.get("vnp_TransactionNo")).signum()==0 || !VnpayProtocol.validDate(f.getOrDefault("vnp_PayDate","")))return VnpayAcknowledgement.of("99");
                boolean cancelled="CANCELLED".equals(order.get("status")),extra=Set.of("PAID","REFUND_PENDING").contains(previous);
                String next=cancelled?"REFUND_PENDING":"PAID";
                attempts.result(ref,cancelled || extra?"NEEDS_REVIEW":"SUCCEEDED",f,source,true);attempts.supersede(id(order,"id"),ref);
                if(!extra){orders.payStatus(id(payment,"id"),next);attempts.systemPaymentHistory(id(payment,"id"),previous,next,cancelled?"VNPAY xác nhận thành công sau hủy; chờ xử lý hoàn tiền, không khôi phục đơn.":"VNPAY Sandbox xác nhận thanh toán qua "+source);h.createUpdate("UPDATE orders SET version=version+1,updated_at=UTC_TIMESTAMP(6) WHERE id=:id").bind("id",id(order,"id")).execute();}
            }else{
                if(!"PENDING".equals(state) && !("EXPIRED".equals(state) && !"01".equals(ts)))return VnpayAcknowledgement.of("02");
                // 04 (đảo giao dịch), 07 (nghi ngờ) hoặc kết quả mâu thuẫn cần đối chiếu, không cho thu tiếp.
                // Techspec PAY 2.1.0 mục 2.5.7.2: 08 hết thời gian, 11 khách hủy.
                String next="01".equals(ts)?state:Set.of("02","11").contains(ts)?"FAILED":"08".equals(ts)?"EXPIRED":"NEEDS_REVIEW";
                attempts.result(ref,next,f,source,false);
            }
            return VnpayAcknowledgement.of("00");
        });
    }
}
