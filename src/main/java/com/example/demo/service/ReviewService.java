package com.example.demo.service;

import com.example.demo.config.Database;
import com.example.demo.dao.*;
import com.example.demo.model.CurrentUser;
import java.util.*;
import static com.example.demo.service.ShopRules.*;

public final class ReviewService {
    private final Database database;
    public ReviewService(Database database) { this.database = database; }

    public Map<String,Object> form(CurrentUser supplied, long order, long item) {
        return safe(() -> database.read(handle -> {
            var buyer = actor(handle,supplied,false);
            var orderData = new OrderDao(handle).order(order,false);
            require(id(orderData,"buyer_id")==buyer.id(),404,"Không tìm thấy đơn mua của bạn.");
            var dao = new ReviewDao(handle);
            var itemData = dao.item(order,item);
            var existing = dao.existing(item);
            require("COMPLETED".equals(orderData.get("status")),409,"Chỉ đánh giá sau khi đơn đã hoàn thành.");
            return Map.of("order",orderData,"item",itemData,"review",existing.orElse(Map.of()),"snapshotImages",new OrderDao(handle).images(order));
        }));
    }
    public void create(CurrentUser supplied, long order, long item, int rating, String input) {
        create(supplied,order,item,rating,input,List.of());
    }
    public Map<String,Object> image(long review,long asset) {
        return safe(()->database.read(h->new ReviewDao(h).publicImage(review,asset).orElseThrow(()->new com.example.demo.exception.ShopException(404,"Không tìm thấy ảnh đánh giá công khai."))));
    }
    public void create(CurrentUser supplied,long order,long item,int rating,String input,List<com.example.demo.model.StoredImage> images) {
        require(images.size()<=3,400,"Tối đa 3 ảnh thực tế cho một đánh giá.");
        require(rating>=1 && rating<=5,400,"Số sao phải từ 1 đến 5.");
        String comment = bounded(input,1,2000,"Nhận xét");
        safe(() -> database.transaction(handle -> {
            var buyer = actor(handle,supplied,true);
            // Cùng khóa order với COMPLETE/complaint, rồi mới kiểm trạng thái và insert.
            var orderData = new OrderDao(handle).order(order,true);
            require(id(orderData,"buyer_id")==buyer.id(),404,"Không tìm thấy đơn mua của bạn.");
            require("COMPLETED".equals(orderData.get("status")),409,"Chỉ đánh giá sau khi đơn đã hoàn thành.");
            var dao = new ReviewDao(handle);
            dao.item(order,item);
            require(dao.existing(item).isEmpty(),409,"Bạn đã đánh giá sản phẩm trong đơn này.");
            long review=dao.insert(order,item,buyer,rating,comment);
            dao.attachImages(review,buyer.id(),images);
            return null;
        }));
    }
}
