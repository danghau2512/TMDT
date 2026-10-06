package com.example.demo.dao;

import org.jdbi.v3.core.Handle;
import java.util.*;
public final class CartDao {
    private final Handle h;
    public CartDao(Handle h) { this.h=h; }
    public long ensure(long user) { h.createUpdate("INSERT INTO carts(user_id) VALUES(:user) ON DUPLICATE KEY UPDATE user_id=user_id").bind("user",user).execute(); return h.createQuery("SELECT id FROM carts WHERE user_id=:user FOR UPDATE").bind("user",user).mapTo(Long.class).one(); }
    public Optional<Map<String,Object>> cart(long user,boolean lock) { return h.createQuery("SELECT * FROM carts WHERE user_id=:user"+(lock?" FOR UPDATE":"")).bind("user",user).mapToMap().findOne(); }
    public List<Map<String,Object>> items(long cart) { return h.createQuery("""
        SELECT p.*,ci.id cart_item_id,ci.quantity,u.display_name seller_name,u.public_contact seller_contact,u.status seller_status,
        c.name category_name,c.status category_status,
        (SELECT asset_id FROM product_images WHERE product_id=p.id ORDER BY sort_order LIMIT 1) image_id
        FROM cart_items ci JOIN products p ON p.id=ci.product_id JOIN users u ON u.id=p.seller_id JOIN categories c ON c.id=p.category_id
        WHERE ci.cart_id=:cart ORDER BY p.id
        """).bind("cart",cart).mapToMap().list(); }
    public int quantity(long cart,long product) { return h.createQuery("SELECT quantity FROM cart_items WHERE cart_id=:cart AND product_id=:product").bind("cart",cart).bind("product",product).mapTo(Integer.class).findOne().orElse(0); }
    public void set(long cart,long product,int quantity) { h.createUpdate("INSERT INTO cart_items(cart_id,product_id,quantity) VALUES(:cart,:product,:quantity) ON DUPLICATE KEY UPDATE quantity=:quantity,updated_at=CURRENT_TIMESTAMP(6)").bind("cart",cart).bind("product",product).bind("quantity",quantity).execute(); bump(cart); }
    public void remove(long cart,long product) { h.createUpdate("DELETE FROM cart_items WHERE cart_id=:cart AND product_id=:product").bind("cart",cart).bind("product",product).execute(); bump(cart); }
    public void clear(long cart) { h.createUpdate("DELETE FROM cart_items WHERE cart_id=:cart").bind("cart",cart).execute(); bump(cart); }
    private void bump(long cart) { h.createUpdate("UPDATE carts SET version=version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=:cart").bind("cart",cart).execute(); }
}
