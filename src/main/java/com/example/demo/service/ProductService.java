package com.example.demo.service;

import com.example.demo.config.Database;
import com.example.demo.dao.*;
import com.example.demo.dto.*;
import com.example.demo.model.*;
import com.example.demo.storage.ImageStorage;
import java.math.BigDecimal;
import java.util.*;
import static com.example.demo.service.ShopRules.*;

public final class ProductService {
    private final Database db;
    private final ImageStorage storage;
    public ProductService(Database db,ImageStorage storage) { this.db=db; this.storage=storage; }
    public List<ProductSuggestion> suggestions(String rawKeyword) {
        String keyword=AccountValidation.text(rawKeyword);
        require(keyword.length()<=100,400,"Từ khóa tối đa 100 ký tự.");
        if(keyword.isEmpty())return List.of();
        return safe(()->db.read(h->new CatalogDao(h).suggestions(keyword).stream().map(p->{
            String productId=Long.toString(id(p,"id"));
            String image=p.get("image_id")==null?"/assets/images/product-placeholder.svg":"/media/product?asset="+id(p,"image_id");
            return new ProductSuggestion(productId,text(p,"title"),money(p,"price"),image,"/products/detail?id="+productId);
        }).toList()));
    }
    public Map<String,Object> catalog(CatalogFilter f) {
        require(f.page()>0 && f.page()<=100000,400,"Trang không hợp lệ.");
        require(f.category()>=0 && f.keyword().length()<=100,400,"Bộ lọc không hợp lệ.");
        require(f.condition().isEmpty() || Set.of("NEW","LIKE_NEW","USED").contains(f.condition()),400,"Tình trạng không hợp lệ.");
        require((f.minPrice()==null || f.minPrice().signum()>=0) && (f.maxPrice()==null || f.maxPrice().signum()>=0)
            && (f.minPrice()==null || f.maxPrice()==null || f.minPrice().compareTo(f.maxPrice())<=0),400,"Khoảng giá không hợp lệ.");
        return safe(()->db.read(h->{var dao=new CatalogDao(h); int count=dao.count(f);
            return Map.of("products",dao.list(f),"categories",dao.categories(),"pages",Math.max(1,(count+11)/12),"resultCount",count);}));
    }
    public Map<String,Object> detail(long product,CurrentUser supplied,boolean management) {
        return detail(product,supplied,management,0,false,1);
    }
    public Map<String,Object> detail(long product,CurrentUser supplied,boolean management,int stars,boolean photos,int page) {
        require(stars>=0 && stars<=5 && page>=1 && page<=100000,400,"Bộ lọc đánh giá không hợp lệ.");
        return safe(()->db.read(h->{ var dao=new CatalogDao(h); var p=dao.product(product);
            if(management) { var actor=actor(h,supplied,false); ownership(actor,p); }
            else require(publiclyVisible(p),404,"Sản phẩm không còn được công khai.");
            var reviews=new ReviewDao(h);
            return Map.of("product",p,"images",dao.images(product),"ratingSummary",reviews.summary(product),"productReviews",reviews.publicReviews(product,stars,photos,page),"reviewPages",Math.max(1,(reviews.count(product,stars,photos)+7)/8),"reviewPage",page); }));
    }
    public Map<String,Object> managed(CurrentUser supplied,boolean admin,String status) {
        require(status.isEmpty() || Set.of("PENDING","APPROVED","REJECTED").contains(status),400,"Trạng thái duyệt không hợp lệ.");
        return safe(()->db.read(h->{ var actor=actor(h,supplied,false); require(!admin || actor.isAdmin(),403,"Bạn không có quyền quản trị."); var dao=new CatalogDao(h);
            return Map.of("products",dao.managed(actor.id(),admin,status),"categories",dao.categories(),"sellers",actor.isAdmin()?dao.sellers():List.of()); }));
    }
    public Map<String,Object> compare(List<Long> ids) {
        require(ids.size()<=3 && ids.stream().allMatch(i->i>0),400,"Chỉ chọn tối đa 3 sản phẩm.");
        return safe(()->db.read(h->{var dao=new CatalogDao(h);var products=new ArrayList<Map<String,Object>>();var unavailable=new ArrayList<Long>();
            for(long id:new LinkedHashSet<>(ids)) {
                Map<String,Object> p;
                try {p=dao.product(id);}catch(com.example.demo.exception.ShopException e){if(e.status()!=404)throw e;unavailable.add(id);continue;}
                if(!publiclyVisible(p)){unavailable.add(id);continue;}
                // Public DTO: never serialize p.* (contact, rejection notes, private fields).
                var dto=new LinkedHashMap<String,Object>();
                for(String key:List.of("id","category_id","category_name","title","price","image_id","condition_code","appearance_code","operation_code","known_defects","repair_code","repair_details","accessories","seller_name","seller_id","stock_quantity"))dto.put(key,p.get(key));
                var sellerRating=new ReviewDao(h).sellerSummary(id(p,"seller_id"));
                dto.put("seller_rating",sellerRating.get("average_rating"));dto.put("seller_review_count",sellerRating.get("review_count"));
                products.add(dto);
            }
            boolean sameCategory=products.stream().map(p->p.get("category_id")).distinct().count()<=1;
            var rows=new ArrayList<Map<String,Object>>();
            var fields=new LinkedHashMap<String,String>();fields.put("price","Giá");fields.put("condition_code","Tình trạng tổng quát");fields.put("appearance_code","Ngoại hình");fields.put("operation_code","Hoạt động");fields.put("known_defects","Lỗi đã biết");fields.put("repair_code","Lịch sử sửa chữa");fields.put("repair_details","Sửa chữa / linh kiện");fields.put("accessories","Phụ kiện");fields.put("seller_name","Người bán");
            for(var field:fields.entrySet()) {var values=products.stream().map(p->p.get(field.getKey())).toList();rows.add(Map.of("key",field.getKey(),"label",field.getValue(),"different",values.stream().distinct().count()>1));}
            BigDecimal min=products.stream().map(p->money(p,"price")).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            rows.add(Map.of("key","seller_rating","label","Điểm giao dịch người bán","different",products.stream().map(p->p.get("seller_rating")).distinct().count()>1));
            return Map.of("comparedProducts",products,"unavailableIds",unavailable,"sameCategory",sameCategory,"comparisonRows",rows,"lowestPrice",min);
        }));
    }
    public long save(CurrentUser supplied,Long product,ProductForm raw,List<StoredImage> images,boolean adminRoute) {
        try {
            require(images.size()<=5,400,"Tối đa 5 ảnh cho một tin.");
            String title=bounded(raw.title(),2,200,"Tên sản phẩm"),description=bounded(raw.description(),1,10000,"Mô tả");
            require(raw.price()!=null && raw.price().signum()>0 && raw.price().compareTo(new BigDecimal("999999999999"))<=0 && raw.price().stripTrailingZeros().scale()<=0,400,"Giá phải là số nguyên dương, tối đa 999.999.999.999 đồng.");
            require(raw.stock()>=0 && raw.stock()<=1_000_000,400,"Số lượng từ 0 đến 1.000.000.");
            require(Set.of("NEW","LIKE_NEW","USED").contains(raw.condition()) && Set.of("PUBLIC","HIDDEN").contains(raw.visibility()),400,"Tình trạng hoặc chế độ hiển thị không hợp lệ.");
            var declaration=ConditionDeclaration.validate(raw.declaration(),images.size());
            var form=new ProductForm(title,raw.categoryId(),description,raw.price(),raw.condition(),raw.stock(),raw.visibility(),raw.sellerId(),raw.listingVersion(),raw.stockVersion(),raw.reason(),declaration);
            return safe(()->db.transaction(h->{ var actor=actor(h,supplied,true); require(!adminRoute || actor.isAdmin(),403,"Bạn không có quyền quản trị."); var dao=new CatalogDao(h);
                require(h.createQuery("SELECT COUNT(*) FROM categories WHERE id=:id AND status='ACTIVE'").bind("id",form.categoryId()).mapTo(Integer.class).one()==1,400,"Danh mục không hoạt động.");
                Map<String,Object> old=product==null?null:dao.lock(product);
                if(old!=null) { ownership(actor,old); require(id(old,"listing_version")==form.listingVersion() && id(old,"stock_version")==form.stockVersion(),409,"Tin hoặc tồn kho đã thay đổi. Hãy mở lại biểu mẫu."); }
                long seller=old==null?(adminRoute?form.sellerId():actor.id()):id(old,"seller_id");
                require(h.createQuery("SELECT COUNT(*) FROM users WHERE id=:id AND status='ACTIVE'").bind("id",seller).mapTo(Integer.class).one()==1,400,"Chọn người bán đang hoạt động.");
                String reason=bounded(AccountValidation.text(form.reason()),0,500,"Lý do điều chỉnh");
                if(actor.isAdmin()) reason=bounded(reason,1,500,"Lý do quản trị");
                if(old!=null && number(old,"stock_quantity")!=form.stock()) require(!reason.isEmpty(),400,"Cần lý do điều chỉnh số lượng.");
                boolean declarationChanged=false;
                if(declaration!=null) {
                    var oldImages=old==null?List.<Map<String,Object>>of():dao.images(product);
                    var assetIds=new HashSet<Long>();var oldDefects=new HashSet<Long>();
                    for(var image:oldImages) { long asset=id(image,"asset_id");assetIds.add(asset);if(Boolean.TRUE.equals(image.get("is_defect")) || "1".equals(String.valueOf(image.get("is_defect"))))oldDefects.add(asset); }
                    require(assetIds.containsAll(declaration.defectAssets()),400,"Ảnh khuyết điểm không thuộc sản phẩm.");
                    declarationChanged=old==null || ConditionDeclaration.values(declaration).entrySet().stream().anyMatch(e->!Objects.equals(old.get(e.getKey()),e.getValue()))
                        || (images.isEmpty() && !oldDefects.equals(declaration.defectAssets()));
                }
                long result;
                if(old==null) { result=dao.insert(form,seller); dao.ledger(result,null,"INITIAL",0,form.stock(),actor.id(),"Số lượng khi đăng tin"); dao.event(result,actor.id(),null,"PENDING",1,"Tin mới chờ duyệt"); }
                else {
                    result=product;
                    boolean changed=!title.equals(old.get("title")) || !description.equals(old.get("description")) || form.price().compareTo(money(old,"price"))!=0
                        || id(old,"category_id")!=form.categoryId() || !form.condition().equals(old.get("condition_code")) || !images.isEmpty() || declarationChanged;
                    dao.edit(result,form,changed);
                    if(changed) dao.event(result,actor.id(),text(old,"moderation_status"),"PENDING",id(old,"listing_version")+1,"Nội dung thay đổi, chờ duyệt lại");
                    int before=number(old,"stock_quantity"); if(before!=form.stock()) dao.ledger(result,null,"ADJUSTMENT",before,form.stock(),actor.id(),reason);
                }
                if(!images.isEmpty()) new MediaDao(h).replace(result,actor.id(),images,title);
                if(declaration!=null) { dao.declaration(result,declaration);dao.defectImages(result,declaration.defectAssets(),declaration.defectIndexes(),!images.isEmpty()); }
                if(actor.isAdmin()) new AuditDao(h).product(actor.id(),result,old==null?"PRODUCT_CREATE":"PRODUCT_EDIT",reason,old==null?null:audit(old),audit(dao.lock(result)));
                return result;
            }));
        } catch(RuntimeException exception) { storage.discard(images); throw exception; }
    }
    public void action(CurrentUser supplied,long product,String action,long version,String reason,boolean adminRoute) {
        safe(()->db.transaction(h->{ var actor=actor(h,supplied,true); require(!adminRoute || actor.isAdmin(),403,"Bạn không có quyền quản trị."); var dao=new CatalogDao(h); var p=dao.lock(product); ownership(actor,p);
            require(id(p,"listing_version")==version,409,"Tin đã thay đổi. Hãy tải lại trang trước khi xử lý.");
            String note=bounded(AccountValidation.text(reason),0,500,"Lý do xử lý"); if(actor.isAdmin()) note=bounded(note,1,500,"Lý do quản trị");
            if("HIDE".equals(action)) dao.hide(product);
            else { require(adminRoute && actor.isAdmin() && Set.of("APPROVE","REJECT").contains(action),403,"Thao tác không được phép.");
                require("PENDING".equals(p.get("moderation_status")),409,"Chỉ xử lý tin đang chờ duyệt.");
                String next="APPROVE".equals(action)?"APPROVED":"REJECTED"; dao.moderation(product,next); dao.event(product,actor.id(),text(p,"moderation_status"),next,version,note); }
            if(actor.isAdmin()) new AuditDao(h).product(actor.id(),product,"PRODUCT_"+action,note,audit(p),audit(dao.lock(product)));
            return null; }));
    }
    public static boolean publiclyVisible(Map<String,Object> p) { return "PUBLIC".equals(p.get("visibility")) && "APPROVED".equals(p.get("moderation_status")) && "ACTIVE".equals(p.get("seller_status")) && "ACTIVE".equals(p.get("category_status")); }
    private static void ownership(CurrentUser actor,Map<String,Object> p) { require(actor.isAdmin() || actor.id()==id(p,"seller_id"),404,"Không tìm thấy sản phẩm của bạn."); }
    private static String audit(Map<String,Object> p) { return "{\"price\":"+money(p,"price")+",\"stock\":"+number(p,"stock_quantity")+",\"version\":"+id(p,"listing_version")+",\"visibility\":\""+text(p,"visibility")+"\",\"moderation\":\""+text(p,"moderation_status")+"\"}"; }
    public Map<String,Object> image(CurrentUser supplied,long asset,Long order) {
        return safe(()->db.read(h->{ var actor=supplied==null?null:actor(h,supplied,false); var dao=new MediaDao(h);
            var result=order==null?dao.productImage(asset,actor==null?0:actor.id(),actor!=null && actor.isAdmin()):dao.orderImage(asset,order,actor==null?0:actor.id(),actor!=null && actor.isAdmin());
            return result.orElseThrow(()->new com.example.demo.exception.ShopException(404,"Không tìm thấy ảnh.")); }));
    }
}
