package com.example.demo.model;

import java.math.BigDecimal;
import java.util.List;

/** Không serialize p.* từ giỏ; chỉ dữ liệu cần đồng bộ giao diện của chủ giỏ. */
public record CartResponse(List<Line> items,BigDecimal total) {
    public record Line(String id,int quantity,BigDecimal price,BigDecimal lineTotal,int maxQuantity,boolean available,boolean canChangeQuantity) { }
    public static CartResponse from(CartSummary cart) {
        return new CartResponse(cart.items().stream().map(p->new Line(p.get("id").toString(),((Number)p.get("quantity")).intValue(),(BigDecimal)p.get("price"),(BigDecimal)p.get("line_total"),((Number)p.get("max_quantity")).intValue(),Boolean.TRUE.equals(p.get("available")),Boolean.TRUE.equals(p.get("can_change_quantity")))).toList(),cart.total());
    }
}
