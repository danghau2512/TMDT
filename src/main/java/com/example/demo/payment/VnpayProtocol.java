package com.example.demo.payment;

import com.example.demo.config.VnpayConfig;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class VnpayProtocol {
    private VnpayProtocol() { }
    public static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(java.time.format.ResolverStyle.STRICT);
    public static String date(Instant instant){return FORMAT.format(instant.atZone(ZONE));}
    public static boolean validDate(String date){try{return date.length()==14 && LocalDateTime.parse(date,FORMAT)!=null;}catch(RuntimeException e){return false;}}
    public static String amount(BigDecimal value) {
        var n=value.multiply(new BigDecimal("100")).toBigIntegerExact();
        if(n.signum()<=0 || n.toString().length()>12) throw new IllegalArgumentException("Số tiền vượt giới hạn VNPAY Sandbox.");
        return n.toString();
    }
    public static String hmac(String secret,String data) {
        try { var mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.GeneralSecurityException e){throw new IllegalStateException("Không thể ký VNPAY.");}
    }
    /** Servlet đã decode một lần. Encode US_ASCII một lần, sắp tên tăng dần theo Java sample VNPAY 2.1.0. */
    public static String canonical(Map<String,String> values) {
        var fields=new TreeMap<>(values);fields.remove("vnp_SecureHash");fields.remove("vnp_SecureHashType");
        var parts=new ArrayList<String>();
        fields.forEach((key,value)->{if(key.startsWith("vnp_") && value!=null && !value.isEmpty()) parts.add(key+"="+URLEncoder.encode(value,StandardCharsets.US_ASCII));});
        return String.join("&",parts);
    }
    private static boolean equalsHash(String expected,String received) {
        if(received==null || !received.matches("[a-fA-F0-9]{128}")) return false;
        return MessageDigest.isEqual(HexFormat.of().parseHex(expected),HexFormat.of().parseHex(received));
    }
    public static boolean verify(VnpayConfig cfg,Map<String,String> fields){return equalsHash(hmac(cfg.secret(),canonical(fields)),fields.get("vnp_SecureHash"));}
    public static String paymentUrl(VnpayConfig cfg,Map<String,Object> attempt) {
        Map<String,String> p=new TreeMap<>();p.put("vnp_Version","2.1.0");p.put("vnp_Command","pay");p.put("vnp_TmnCode",attempt.get("merchant_code").toString());
        p.put("vnp_Amount",amount((BigDecimal)attempt.get("expected_amount")));p.put("vnp_CreateDate",attempt.get("vnp_create_date").toString());p.put("vnp_ExpireDate",attempt.get("vnp_expire_date").toString());
        p.put("vnp_CurrCode","VND");p.put("vnp_IpAddr",attempt.get("client_ip").toString());p.put("vnp_Locale","vn");p.put("vnp_OrderType","other");
        p.put("vnp_OrderInfo","Thanh toan don hang "+attempt.get("order_id"));p.put("vnp_TxnRef",attempt.get("txn_ref").toString());p.put("vnp_ReturnUrl",attempt.get("return_url").toString());
        String data=canonical(p);return cfg.payUrl()+"?"+data+"&vnp_SecureHash="+hmac(cfg.secret(),data);
    }
    private static final List<String> QUERY_REQUEST=List.of("vnp_RequestId","vnp_Version","vnp_Command","vnp_TmnCode","vnp_TxnRef","vnp_TransactionDate","vnp_CreateDate","vnp_IpAddr","vnp_OrderInfo");
    private static final List<String> QUERY_RESPONSE=List.of("vnp_ResponseId","vnp_Command","vnp_ResponseCode","vnp_Message","vnp_TmnCode","vnp_TxnRef","vnp_Amount","vnp_BankCode","vnp_PayDate","vnp_TransactionNo","vnp_TransactionType","vnp_TransactionStatus","vnp_OrderInfo","vnp_PromotionCode","vnp_PromotionAmount");
    public static String queryData(Map<String,String> p,boolean response){return String.join("|",(response?QUERY_RESPONSE:QUERY_REQUEST).stream().map(k->p.getOrDefault(k,"")).toList());}
    public static boolean verifyQuery(VnpayConfig c,Map<String,String> p){return equalsHash(hmac(c.secret(),queryData(p,true)),p.get("vnp_SecureHash"));}
    public static Map<String,String> queryRequest(VnpayConfig c,Map<String,Object> attempt,String ip,Instant now){
        var p=new LinkedHashMap<String,String>();p.put("vnp_RequestId",UUID.randomUUID().toString().replace("-",""));p.put("vnp_Version","2.1.0");p.put("vnp_Command","querydr");
        p.put("vnp_TmnCode",c.merchant());p.put("vnp_TxnRef",attempt.get("txn_ref").toString());p.put("vnp_TransactionDate",attempt.get("vnp_create_date").toString());
        p.put("vnp_CreateDate",date(now));p.put("vnp_IpAddr",ip);p.put("vnp_OrderInfo","Kiem tra thanh toan don hang "+attempt.get("order_id"));p.put("vnp_SecureHash",hmac(c.secret(),queryData(p,false)));return p;
    }
}
