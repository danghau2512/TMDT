package com.example.demo.dao;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.jdbi.v3.core.Handle;

public final class VnpayDao {
    private final Handle h;
    public VnpayDao(Handle h){this.h=h;}
    public Optional<Map<String,Object>> byRef(String ref,boolean lock){return h.createQuery("SELECT * FROM vnpay_attempts WHERE txn_ref=:ref"+(lock?" FOR UPDATE":"")).bind("ref",ref).mapToMap().findOne();}
    public Optional<Map<String,Object>> active(long order){return h.createQuery("SELECT * FROM vnpay_attempts WHERE order_id=:id AND status='PENDING' FOR UPDATE").bind("id",order).mapToMap().findOne();}
    public List<Map<String,Object>> history(long order){return h.createQuery("SELECT txn_ref,status,expected_amount,created_at,expires_at,gateway_transaction_no,bank_code,gateway_pay_date,response_code,transaction_status,result_source FROM vnpay_attempts WHERE order_id=:id ORDER BY id DESC").bind("id",order).mapToMap().list();}
    public boolean expired(String ref){return h.createQuery("SELECT expires_at<=UTC_TIMESTAMP(6) FROM vnpay_attempts WHERE txn_ref=:ref").bind("ref",ref).mapTo(Boolean.class).one();}
    public void state(String ref,String status){h.createUpdate("UPDATE vnpay_attempts SET status=:s,updated_at=UTC_TIMESTAMP(6) WHERE txn_ref=:ref").bind("s",status).bind("ref",ref).execute();}
    public Map<String,Object> insert(long order,String ref,String merchant,BigDecimal amount,String created,String expires,Instant deadline,String ip,String returnUrl){
        h.createUpdate("INSERT INTO vnpay_attempts(order_id,txn_ref,merchant_code,expected_amount,vnp_create_date,vnp_expire_date,expires_at,client_ip,return_url) VALUES(:o,:r,:m,:a,:c,:e,:t,:ip,:url)")
            .bind("o",order).bind("r",ref).bind("m",merchant).bind("a",amount).bind("c",created).bind("e",expires).bind("t",LocalDateTime.ofInstant(deadline,ZoneOffset.UTC)).bind("ip",ip).bind("url",returnUrl).execute();
        return byRef(ref,false).orElseThrow();
    }
    public void result(String ref,String status,Map<String,String> fields,String source,boolean success){
        String txn=fields.getOrDefault("vnp_TransactionNo","");if(!txn.matches("[0-9]{1,15}") || new java.math.BigInteger(txn).signum()==0)txn=null;
        h.createUpdate("""
            UPDATE vnpay_attempts SET status=:s,gateway_transaction_no=:txn,bank_code=:bank,gateway_pay_date=:date,
            response_code=:response,transaction_status=:ts,result_source=:source,confirmed_at=CASE WHEN :success=1 THEN UTC_TIMESTAMP(6) ELSE confirmed_at END,updated_at=UTC_TIMESTAMP(6) WHERE txn_ref=:r
            """).bind("s",status).bind("txn",txn).bind("bank",fields.get("vnp_BankCode")).bind("date",fields.get("vnp_PayDate"))
            .bind("response",fields.get("vnp_ResponseCode")).bind("ts",fields.get("vnp_TransactionStatus")).bind("source",source).bind("success",success?1:0).bind("r",ref).execute();
    }
    public void supersede(long order,String successfulRef){h.createUpdate("UPDATE vnpay_attempts SET status='SUPERSEDED',updated_at=UTC_TIMESTAMP(6) WHERE order_id=:o AND txn_ref<>:r AND status='PENDING'").bind("o",order).bind("r",successfulRef).execute();}
    public void systemPaymentHistory(long payment,String from,String to,String note){h.createUpdate("INSERT INTO payment_status_history(payment_id,from_status,to_status,actor_id,actor_role_snapshot,note) VALUES(:p,:f,:t,NULL,'SYSTEM',:n)").bind("p",payment).bind("f",from).bind("t",to).bind("n",note).execute();}
    public boolean reserveQuery(String ref){return h.createUpdate("UPDATE vnpay_attempts SET last_query_at=UTC_TIMESTAMP(6) WHERE txn_ref=:r AND (last_query_at IS NULL OR last_query_at<UTC_TIMESTAMP(6)-INTERVAL 30 SECOND)").bind("r",ref).execute()==1;}
}
