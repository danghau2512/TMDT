package com.example.demo.service;

import com.example.demo.config.*;
import com.example.demo.dao.SellerVerificationDao;
import com.example.demo.exception.ShopException;
import com.example.demo.model.*;
import com.example.demo.dto.VerificationForm;
import com.example.demo.storage.ImageStorage;
import com.example.demo.verification.*;
import java.util.*;
import java.time.*;
import static com.example.demo.service.ShopRules.*;

public final class SellerVerificationService {
    private final Database db;
    private final ImageStorage storage;
    private final IdentityQrReader qr=new IdentityQrReader();
    public SellerVerificationService(Database db,VerificationConfig config){this.db=db;this.storage=new ImageStorage(config.root());}
    public ImageStorage storage(){return storage;}
    public boolean approved(CurrentUser supplied){return safe(()->db.read(h->{var user=actor(h,supplied,false);return new SellerVerificationDao(h).approved(user.id(),false);}));}
    public Map<String,Object> own(CurrentUser supplied){return safe(()->db.read(h->{var user=actor(h,supplied,false);var dao=new SellerVerificationDao(h);var profile=dao.profile(user.id(),false);Map<String,Object> submission=Map.of();
        if(profile.isPresent()&&profile.get().get("current_submission_id")!=null)submission=publicView(dao.submission(id(profile.get(),"current_submission_id"),false).orElseThrow(),true);
        return Map.of("verificationStatus",profile.map(p->text(p,"status")).orElse("NOT_SUBMITTED"),"submission",submission);}));}
    public long upload(CurrentUser supplied,StoredImage front,StoredImage back,boolean consent){
        try {require(consent,400,"Cần đồng ý lưu giấy tờ riêng tư để quản trị viên đối chiếu.");require(front!=null&&back!=null,400,"Cần đủ ảnh mặt trước và mặt sau căn cước.");
            return safe(()->db.transaction(h->{var user=actor(h,supplied,true);var dao=new SellerVerificationDao(h);dao.ensure(user.id());var profile=dao.profile(user.id(),true).orElseThrow();
                require(!"PENDING".equals(profile.get("status")),409,"Hồ sơ đang chờ duyệt. Vui lòng chờ quyết định trước khi đổi giấy tờ.");
                if(profile.get("current_submission_id")!=null)dao.supersede(id(profile,"current_submission_id"));
                long id=dao.draft(user.id(),front,back);dao.current(user.id(),id,"NOT_SUBMITTED");return id;}));
        }catch(RuntimeException e){storage.discard(front==null?List.of():back==null?List.of(front):List.of(front,back));throw e;}
    }
    /** Giải mã cục bộ ngoài Handle, chỉ ghi nếu vẫn là draft hiện tại của cùng tài khoản. */
    public String read(CurrentUser supplied,long submission){
        var metadata=safe(()->db.transaction(h->{var user=actor(h,supplied,true);var dao=new SellerVerificationDao(h);var s=draft(dao,user.id(),submission);
            require(!legacy(s)&&s.get("front_key")!=null&&s.get("back_key")!=null,409,"Hồ sơ cũ vẫn được giữ. Tải đủ hai ảnh mới để dùng luồng đọc QR.");
            boolean stale=s.get("qr_started_at")!=null && timeValue(s.get("qr_started_at")).isBefore(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
            require(!"READING".equals(s.get("qr_status"))||stale,409,"Đang đọc mã QR. Vui lòng chờ một chút.");
            dao.startQr(submission);return s;}));
        IdentityQrReader.Outcome result;
        try {result=qr.read(storage.read(text(metadata,"front_key")),storage.read(text(metadata,"back_key")));}
        catch(RuntimeException ignored){result=new IdentityQrReader.Outcome("NOT_FOUND",null);}
        var captured=result;
        safe(()->db.transaction(h->{var user=actor(h,supplied,true);var dao=new SellerVerificationDao(h);var s=draft(dao,user.id(),submission);
            dao.qr(submission,captured.status(),captured.details());return null;}));
        return qrMessage(result.status());
    }
    public void submit(CurrentUser supplied,long submission,VerificationForm form){
        safe(()->db.transaction(h->{var user=actor(h,supplied,true);var dao=new SellerVerificationDao(h);var s=draft(dao,user.id(),submission);
            require(!"READING".equals(s.get("qr_status")),409,"Đợi đọc QR hoàn tất trước khi gửi.");
            require(!"CONFLICT".equals(s.get("qr_status")),409,qrMessage("CONFLICT"));
            IdentityDetails d;
            if(legacy(s)){String name=bounded(form.name(),2,120,"Họ tên"),number=AccountValidation.text(form.number());require(number.matches("([0-9]{9}|[0-9]{12})"),400,"Số giấy tờ cũ gồm 9 hoặc 12 chữ số.");d=new IdentityDetails(number,name,null,null,null,null);}
            else {require(s.get("front_key")!=null&&s.get("back_key")!=null,400,"Cần đủ ảnh mặt trước và mặt sau căn cước.");d=IdentityParser.form(form);}
            dao.submit(submission,d);dao.current(user.id(),submission,"PENDING");return null;}));
    }
    public List<Map<String,Object>> queue(CurrentUser supplied,String status,int page){require(Set.of("","PENDING","APPROVED","REJECTED").contains(status)&&page>=1&&page<=100000,400,"Bộ lọc hồ sơ không hợp lệ.");return safe(()->db.read(h->{require(actor(h,supplied,false).isAdmin(),403,"Bạn không có quyền quản trị.");var rows=new SellerVerificationDao(h).queue(status,page);for(var row:rows){row.put("submitted_at",displayTime(row.get("submitted_at")));row.put("reviewed_at",displayTime(row.get("reviewed_at")));}return rows;}));}
    public Map<String,Object> adminDetail(CurrentUser supplied,long id){return safe(()->db.read(h->{var admin=actor(h,supplied,false);require(admin.isAdmin(),403,"Bạn không có quyền quản trị.");var dao=new SellerVerificationDao(h);var s=dao.submission(id,false).orElseThrow(()->missing());require(!Set.of("DRAFT","PURGED").contains(text(s,"status")),404,"Hồ sơ chưa gửi hoặc giấy tờ đã xóa.");
        var result=publicView(s,s.get("reviewer_id")!=null&&id(s,"reviewer_id")==admin.id());result.put("claimable","PENDING".equals(s.get("status"))&&s.get("reviewer_id")==null);return result;}));}
    public void claim(CurrentUser supplied,long submission){safe(()->db.transaction(h->{var admin=actor(h,supplied,true);require(admin.isAdmin(),403,"Bạn không có quyền quản trị.");var dao=new SellerVerificationDao(h);
        var row=dao.submission(submission,false).orElseThrow(()->missing());var profile=dao.profile(id(row,"user_id"),true).orElseThrow();var s=dao.submission(submission,true).orElseThrow();
        require(profile.get("current_submission_id")!=null&&id(profile,"current_submission_id")==submission&&"PENDING".equals(s.get("status")),409,"Hồ sơ không còn chờ duyệt.");
        require(s.get("reviewer_id")==null||id(s,"reviewer_id")==admin.id(),409,"Hồ sơ đã có quản trị viên phụ trách.");dao.claim(submission,admin.id());return null;}));}
    public void decide(CurrentUser supplied,long submission,String action,String rawReason){require(Set.of("APPROVE","REJECT").contains(action),400,"Quyết định không hợp lệ.");String reason="REJECT".equals(action)?bounded(rawReason,5,1000,"Lý do từ chối"):null;
        safe(()->db.transaction(h->{var admin=actor(h,supplied,true);require(admin.isAdmin(),403,"Bạn không có quyền quản trị.");var dao=new SellerVerificationDao(h);var row=dao.submission(submission,false).orElseThrow(()->missing());long owner=id(row,"user_id");
            // Profile → submission cũng là thứ tự của upload/submit/purge, không khóa ngược user người bán.
            var profile=dao.profile(owner,true).orElseThrow();var s=dao.submission(submission,true).orElseThrow();require(s.get("reviewer_id")!=null&&id(s,"reviewer_id")==admin.id(),403,"Chỉ quản trị viên phụ trách được quyết định.");
            require(profile.get("current_submission_id")!=null&&id(profile,"current_submission_id")==submission&&"PENDING".equals(s.get("status")),409,"Hồ sơ đã thay đổi hoặc đã có quyết định.");
            String status="APPROVE".equals(action)?"APPROVED":"REJECTED";dao.decide(submission,status,reason);dao.current(owner,submission,status);return null;}));
    }
    public Map<String,Object> image(CurrentUser supplied,long submission,String side){require(Set.of("front","back").contains(side),404,"Không tìm thấy ảnh.");return safe(()->db.read(h->{var user=actor(h,supplied,false);var s=new SellerVerificationDao(h).submission(submission,false).orElseThrow(()->missing());
        require(user.id()==id(s,"user_id") || user.isAdmin()&&s.get("reviewer_id")!=null&&user.id()==id(s,"reviewer_id"),404,"Không tìm thấy ảnh của bạn.");
        require(!"PURGED".equals(s.get("status"))&&s.get(side+"_key")!=null,404,"Không tìm thấy ảnh.");return Map.of("key",s.get(side+"_key"),"mime",s.get(side+"_mime"));}));}
    public void purge(CurrentUser supplied){safe(()->db.transaction(h->{var user=actor(h,supplied,true);var dao=new SellerVerificationDao(h);dao.ensure(user.id());dao.profile(user.id(),true);
        for(var row:dao.documents(user.id()))for(String side:List.of("front","back"))if(row.get(side+"_key")!=null)storage.deletePrivate(text(row,side+"_key"));
        dao.purge(user.id());return null;}));}
    private Map<String,Object> draft(SellerVerificationDao dao,long user,long submission){var owner=dao.submission(submission,false).orElseThrow(()->missing());require(id(owner,"user_id")==user,404,"Không tìm thấy hồ sơ của bạn.");var profile=dao.profile(user,true).orElseThrow(()->missing());require(profile.get("current_submission_id")!=null&&id(profile,"current_submission_id")==submission,409,"Hồ sơ đã thay đổi. Hãy tải lại trang.");var s=dao.submission(submission,true).orElseThrow(()->missing());require(id(s,"user_id")==user&&"DRAFT".equals(s.get("status")),409,"Chỉ được sửa hồ sơ chưa gửi. Sau từ chối hoặc khi đổi thông tin, tải giấy tờ và gửi lại.");return s;}
    public static String qrMessage(String status){return switch(status){
        case "SUCCESS" -> "Đã đọc thông tin. Vui lòng kiểm tra trước khi gửi";
        case "UNSUPPORTED" -> "Đã thấy mã QR nhưng định dạng hoặc dữ liệu căn cước chưa được hỗ trợ. Bạn có thể nhập thủ công để quản trị viên kiểm tra.";
        case "CONFLICT" -> "Các mã QR trên ảnh có thông tin mâu thuẫn. Kiểm tra và tải lại hai mặt của cùng một căn cước trước khi gửi.";
        case "READING" -> "Đang đọc mã QR…";
        default -> "Không đọc được QR trên hai ảnh. Chụp rõ mã QR, tải lại hoặc nhập thủ công để quản trị viên kiểm tra.";
    };}
    private static boolean legacy(Map<String,Object> s){return Set.of("OCR_LEGACY","MANUAL_LEGACY").contains(text(s,"data_source"));}
    private static Map<String,Object> publicView(Map<String,Object> s,boolean visible){var r=new HashMap<String,Object>();for(String key:List.of("id","status"))r.put(key,s.get(key));r.put("canView",visible);
        for(String key:List.of("submitted_at","reviewed_at","created_at"))r.put(key,displayTime(s.get(key)));
        r.put("legacy",legacy(s));
        if(visible){
            for(String key:List.of("full_name","id_number","gender","residence","rejection_reason","reviewer_name","data_source","qr_status"))r.put(key,s.get(key));
            // Bản ứng dụng cũ có thể gửi thêm hồ sơ trước khi restart; vẫn nhận biết baseline OCR của chúng.
            r.put("sourceLabel",switch(text(s,"data_source")){case "QR"->"Đọc từ QR";case "OCR_LEGACY"->"OCR (hồ sơ cũ)";case "MANUAL_LEGACY"->"SUCCESS".equals(s.get("ocr_status"))?"OCR (hồ sơ cũ)":"Nhập thủ công (hồ sơ cũ)";default->"Nhập thủ công";});
            r.put("birth_date",displayDate(s.get("birth_date")));r.put("issue_date",displayDate(s.get("issue_date")));
            var fields=new ArrayList<Map<String,Object>>();
            String[][] keys={{"id_number","Số định danh","qr_id_number","ocr_number"},{"full_name","Họ và tên","qr_full_name","ocr_name"},{"birth_date","Ngày sinh","qr_birth_date",""},{"gender","Giới tính","qr_gender",""},{"residence","Nơi cư trú","qr_residence",""},{"issue_date","Ngày cấp","qr_issue_date",""}};
            for(var key:keys){Object actual=s.get(key[0]),original=legacy(s)?(key[3].isEmpty()?null:s.get(key[3])):s.get(key[2]);String value=key[0].endsWith("date")?displayDate(actual):actual==null?"":actual.toString();String baseline=key[0].endsWith("date")?displayDate(original):original==null?"":original.toString();r.put("prefill_"+key[0],baseline);fields.add(Map.of("label",key[1],"value",value,"original",baseline,"changed",original!=null&&!Objects.equals(value,baseline)));}
            r.put("fields",fields);r.put("hasBaseline","QR".equals(s.get("data_source"))||legacy(s)&&"SUCCESS".equals(s.get("ocr_status")));
        }
        r.put("hasFront",visible&&s.get("front_key")!=null);r.put("hasBack",visible&&s.get("back_key")!=null);return r;}
    private static String displayDate(Object value){if(value==null)return "";LocalDate date=value instanceof java.sql.Date d?d.toLocalDate():(LocalDate)value;return date.format(IdentityParser.DISPLAY_DATE);}
    private static LocalDateTime timeValue(Object value){return value instanceof java.sql.Timestamp t?t.toLocalDateTime():(LocalDateTime)value;}
    private static String displayTime(Object value){return value==null?"":timeValue(value).atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy"));}
    private static ShopException missing(){return new ShopException(404,"Không tìm thấy hồ sơ của bạn.");}
}
