package com.example.demo.dao;

import com.example.demo.model.StoredImage;
import com.example.demo.model.IdentityDetails;
import org.jdbi.v3.core.Handle;
import java.util.*;

public final class SellerVerificationDao {
    private final Handle h;
    public SellerVerificationDao(Handle h){this.h=h;}
    public void ensure(long user){h.createUpdate("INSERT IGNORE INTO seller_profiles(user_id) VALUES(:u)").bind("u",user).execute();}
    public Optional<Map<String,Object>> profile(long user,boolean lock){return h.createQuery("SELECT * FROM seller_profiles WHERE user_id=:u"+(lock?" FOR UPDATE":"")).bind("u",user).mapToMap().findOne();}
    public Optional<Map<String,Object>> submission(long id,boolean lock){return h.createQuery(lock?"SELECT * FROM seller_verification_submissions WHERE id=:id FOR UPDATE":"SELECT s.*,u.display_name reviewer_name FROM seller_verification_submissions s LEFT JOIN users u ON u.id=s.reviewer_id WHERE s.id=:id").bind("id",id).mapToMap().findOne();}
    public boolean approved(long user,boolean lock){return profile(user,lock).map(p->"APPROVED".equals(p.get("status"))).orElse(false);}
    public long draft(long user,StoredImage front,StoredImage back){return h.createUpdate("""
        INSERT INTO seller_verification_submissions(user_id,front_key,front_mime,back_key,back_mime,data_source,consent_version)
        VALUES(:u,:front,:fm,:back,:bm,'MANUAL','20261006-qr-v1')
        """).bind("u",user).bind("front",front.key()).bind("fm",front.mime()).bind("back",back.key()).bind("bm",back.mime()).executeAndReturnGeneratedKeys("id").mapTo(Long.class).one();}
    public void current(long user,long id,String status){h.createUpdate("UPDATE seller_profiles SET current_submission_id=:id,status=:s,updated_at=CURRENT_TIMESTAMP(6) WHERE user_id=:u").bind("u",user).bind("id",id).bind("s",status).execute();}
    public void supersede(long id){h.createUpdate("UPDATE seller_verification_submissions SET status='SUPERSEDED' WHERE id=:id").bind("id",id).execute();}
    public void qr(long id,String status,IdentityDetails details){h.createUpdate("""
        UPDATE seller_verification_submissions SET qr_status=:s,data_source=:source,
        qr_full_name=:name,qr_id_number=:number,qr_birth_date=:birth,qr_gender=:gender,qr_residence=:residence,qr_issue_date=:issued WHERE id=:id
        """).bind("id",id).bind("s",status).bind("source",details==null?"MANUAL":"QR").bind("name",details==null?null:details.name()).bind("number",details==null?null:details.number()).bind("birth",details==null?null:details.birthDate()).bind("gender",details==null?null:details.gender()).bind("residence",details==null?null:details.residence()).bind("issued",details==null?null:details.issueDate()).execute();}
    public void startQr(long id){h.createUpdate("UPDATE seller_verification_submissions SET qr_status='READING',qr_started_at=CURRENT_TIMESTAMP(6) WHERE id=:id").bind("id",id).execute();}
    public void submit(long id,IdentityDetails d){h.createUpdate("UPDATE seller_verification_submissions SET status='PENDING',full_name=:name,id_number=:number,birth_date=:birth,gender=:gender,residence=:residence,issue_date=:issued,submitted_at=CURRENT_TIMESTAMP(6) WHERE id=:id").bind("id",id).bind("name",d.name()).bind("number",d.number()).bind("birth",d.birthDate()).bind("gender",d.gender()).bind("residence",d.residence()).bind("issued",d.issueDate()).execute();}
    public List<Map<String,Object>> queue(String status,int page){return h.createQuery("""
        SELECT s.id,s.status,s.submitted_at,s.reviewed_at,s.reviewer_id,u.display_name
        FROM seller_profiles p JOIN seller_verification_submissions s ON s.id=p.current_submission_id
        JOIN users u ON u.id=p.user_id WHERE s.status IN ('PENDING','APPROVED','REJECTED')
        AND (:s='' OR s.status=:s) ORDER BY s.submitted_at DESC,s.id DESC LIMIT 20 OFFSET :offset
        """).bind("s",status).bind("offset",(page-1)*20).mapToMap().list();}
    public void claim(long id,long admin){h.createUpdate("UPDATE seller_verification_submissions SET reviewer_id=:a WHERE id=:id").bind("id",id).bind("a",admin).execute();}
    public void decide(long id,String status,String reason){h.createUpdate("UPDATE seller_verification_submissions SET status=:s,rejection_reason=:reason,reviewed_at=CURRENT_TIMESTAMP(6) WHERE id=:id").bind("id",id).bind("s",status).bind("reason",reason).execute();}
    public List<Map<String,Object>> documents(long user){return h.createQuery("SELECT front_key,front_mime,back_key,back_mime FROM seller_verification_submissions WHERE user_id=:u").bind("u",user).mapToMap().list();}
    public void purge(long user){h.createUpdate("""
        UPDATE seller_verification_submissions SET status='PURGED',full_name=NULL,id_number=NULL,
        ocr_name=NULL,ocr_number=NULL,card_type=NULL,front_key=NULL,front_mime=NULL,back_key=NULL,back_mime=NULL,rejection_reason=NULL,
        birth_date=NULL,gender=NULL,residence=NULL,issue_date=NULL,qr_status='NOT_READ',qr_full_name=NULL,qr_id_number=NULL,
        qr_birth_date=NULL,qr_gender=NULL,qr_residence=NULL,qr_issue_date=NULL
        WHERE user_id=:u
        """).bind("u",user).execute();h.createUpdate("UPDATE seller_profiles SET status='NOT_SUBMITTED',current_submission_id=NULL,updated_at=CURRENT_TIMESTAMP(6) WHERE user_id=:u").bind("u",user).execute();}
}
