package com.example.demo.dao;

import org.jdbi.v3.core.Handle;
public final class AuditDao {
    private final Handle h;
    public AuditDao(Handle h) { this.h=h; }
    public void product(long actor,long product,String action,String reason,String before,String after) { h.createUpdate("INSERT INTO admin_audit_log(actor_id,action,reason,product_id,before_data,after_data) VALUES(:actor,:action,:reason,:product,:before,:after)").bind("actor",actor).bind("action",action).bind("reason",reason).bind("product",product).bind("before",before).bind("after",after).execute(); }
    public void order(long actor,long order,String action,String reason,String before,String after) { h.createUpdate("INSERT INTO admin_audit_log(actor_id,action,reason,order_id,before_data,after_data) VALUES(:actor,:action,:reason,:order,:before,:after)").bind("actor",actor).bind("action",action).bind("reason",reason).bind("order",order).bind("before",before).bind("after",after).execute(); }
    public void complaint(long actor,long complaint,String action,String reason,String from,String to) {
        h.createUpdate("INSERT INTO admin_audit_log(actor_id,action,reason,complaint_id,before_data,after_data) VALUES(:actor,:action,:reason,:complaint,:before,:after)")
                .bind("actor",actor).bind("action",action).bind("reason",reason).bind("complaint",complaint)
                .bind("before","{\"status\":\""+from+"\"}").bind("after","{\"status\":\""+to+"\"}").execute();
    }
}
