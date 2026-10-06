package com.example.demo.service;

import com.example.demo.config.Database;
import com.example.demo.dao.*;
import com.example.demo.model.*;
import java.math.BigDecimal;
import java.util.*;
import static com.example.demo.service.ShopRules.*;

public final class CartService {
    private final Database db;
    public CartService(Database db) { this.db=db; }
    public CartSummary view(CurrentUser supplied) { return safe(()->db.read(h->{var actor=actor(h,supplied,false); var dao=new CartDao(h); var cart=dao.cart(actor.id(),false); return cart.isEmpty()?new CartSummary(List.of(),BigDecimal.ZERO,digest("empty")):summary(dao.items(id(cart.get(),"id")),id(cart.get(),"version"),actor.id());})); }
    public void change(CurrentUser supplied,long product,int quantity,String action) {
        change(supplied,product,quantity,action,false);
    }
    /** Trả snapshot trong cùng transaction ghi, tránh tổng tiền từ một request đọc khác. */
    public CartSummary changeAndView(CurrentUser supplied,long product,int quantity,String action) {
        return change(supplied,product,quantity,action,true);
    }
    private CartSummary change(CurrentUser supplied,long product,int quantity,String action,boolean snapshot) {
        require(Set.of("ADD","UPDATE","REMOVE").contains(action),400,"Thao tác giỏ hàng không hợp lệ.");
        require("REMOVE".equals(action) || quantity>=1 && quantity<=999,400,"Số lượng từ 1 đến 999.");
        return safe(()->db.transaction(h->{ var actor=actor(h,supplied,true); var cartDao=new CartDao(h); long cart=cartDao.ensure(actor.id());
            if("REMOVE".equals(action))cartDao.remove(cart,product);
            else {
                var productDao=new CatalogDao(h); productDao.lock(product); var p=productDao.product(product);
                int old=cartDao.quantity(cart,product); int next="ADD".equals(action)?old+quantity:quantity;
                require(!"UPDATE".equals(action) || old>0,404,"Sản phẩm không có trong giỏ của bạn.");
                require(old>0 || cartDao.items(cart).size()<50,400,"Giỏ hàng tối đa 50 sản phẩm.");
                purchasable(p,actor.id(),next); cartDao.set(cart,product,next);
            }
            if(!snapshot)return null;
            long version=id(cartDao.cart(actor.id(),false).orElseThrow(),"version");
            return summary(cartDao.items(cart),version,actor.id()); }));
    }
    public static void purchasable(Map<String,Object> p,long buyer,int quantity) {
        require(id(p,"seller_id")!=buyer,400,"Bạn không thể mua sản phẩm của chính mình.");
        require(ProductService.publiclyVisible(p),409,"Sản phẩm không còn được bán. Hãy bỏ sản phẩm khỏi giỏ.");
        require(quantity>=1 && quantity<=999 && number(p,"stock_quantity")>=quantity,409,"Số lượng yêu cầu vượt quá số lượng còn bán.");
    }
    public static CartSummary summary(List<Map<String,Object>> items,long version,long buyer) {
        BigDecimal total=BigDecimal.ZERO; StringBuilder fingerprint=new StringBuilder().append(version);
        for(var p:items) { int qty=number(p,"quantity"); BigDecimal line=money(p,"price").multiply(BigDecimal.valueOf(qty)); p.put("line_total",line);
            p.put("available",ProductService.publiclyVisible(p) && id(p,"seller_id")!=buyer && number(p,"stock_quantity")>=qty);
            p.put("max_quantity",Math.min(999,number(p,"stock_quantity")));
            p.put("can_change_quantity",ProductService.publiclyVisible(p) && id(p,"seller_id")!=buyer && number(p,"stock_quantity")>0);
            total=total.add(line); fingerprint.append('|').append(id(p,"id")).append(':').append(qty).append(':').append(id(p,"listing_version")).append(':').append(money(p,"price").toPlainString()); }
        return new CartSummary(items,total,digest(fingerprint.toString()));
    }
}
