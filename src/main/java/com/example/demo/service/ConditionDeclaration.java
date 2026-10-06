package com.example.demo.service;
import com.example.demo.dto.ConditionForm;
import java.util.*;
import static com.example.demo.service.ShopRules.*;

public final class ConditionDeclaration {
    private ConditionDeclaration() { }
    public static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("LIKE_NEW","Như mới"), Map.entry("LIGHT_SCRATCHES","Trầy xước nhẹ"),Map.entry("WORN","Dấu hiệu sử dụng rõ"),
        Map.entry("NORMAL","Hoạt động bình thường"),Map.entry("FAULTY","Có lỗi"),Map.entry("NEEDS_REPAIR","Cần sửa chữa"),
        Map.entry("NEVER","Chưa sửa"),Map.entry("REPAIRED","Đã sửa"),Map.entry("UNKNOWN","Không rõ"),Map.entry("NOT_APPLICABLE","Không áp dụng"));
    public static ConditionForm validate(ConditionForm d,int count) {
        if(d==null) return null;
        String appearance=code(d.appearance(),Set.of("LIKE_NEW","LIGHT_SCRATCHES","WORN","NOT_APPLICABLE"),"Ngoại hình");
        String operation=code(d.operation(),Set.of("NORMAL","FAULTY","NEEDS_REPAIR","NOT_APPLICABLE"),"Hoạt động");
        String repair=code(d.repair(),Set.of("NEVER","REPAIRED","UNKNOWN","NOT_APPLICABLE"),"Lịch sử sửa chữa");
        require(d.defectAssets().size()<=5 && d.defectIndexes().size()<=5,400,"Tối đa 5 ảnh khuyết điểm.");
        require(d.defectIndexes().stream().allMatch(i->i>=0 && i<count),400,"Chọn ảnh khuyết điểm trong bộ ảnh đã tải.");
        return new ConditionForm(appearance,operation,optional(d.defects(),"Lỗi đã biết"),repair,
            optional(d.repairDetails(),"Chi tiết sửa chữa"),optional(d.accessories(),"Phụ kiện"),Set.copyOf(d.defectAssets()),Set.copyOf(d.defectIndexes()));
    }
    private static String code(String value,Set<String> allowed,String label) {
        String result=AccountValidation.text(value); require(result.isEmpty() || allowed.contains(result),400,label+" không hợp lệ."); return result.isEmpty()?null:result;
    }
    private static String optional(String value,String label) { String result=bounded(AccountValidation.text(value),0,1000,label);return result.isEmpty()?null:result; }
    public static Map<String,Object> values(ConditionForm d) {
        var values=new LinkedHashMap<String,Object>(); values.put("appearance_code",d.appearance());values.put("operation_code",d.operation());values.put("known_defects",d.defects());
        values.put("repair_code",d.repair());values.put("repair_details",d.repairDetails());values.put("accessories",d.accessories());return values;
    }
}
