package com.example.demo.config;

import com.example.demo.service.*;
import com.example.demo.storage.ImageStorage;
public record ShopServices(ProductService products,CartService cart,OrderService orders,ImageStorage storage,ReviewService reviews,ComplaintService complaints,VnpayService payments,ChatService chat) {
    public static final String KEY=ShopServices.class.getName();
    public ShopServices(Database db,java.nio.file.Path uploadRoot) {
        this(db,uploadRoot,java.util.Optional.empty());
    }
    public ShopServices(Database db,java.nio.file.Path uploadRoot,java.util.Optional<VnpayConfig> vnpay) {
        this(new ProductService(db,new ImageStorage(uploadRoot)),new CartService(db),new OrderService(db),new ImageStorage(uploadRoot),new ReviewService(db),new ComplaintService(db,new ImageStorage(uploadRoot)),new VnpayService(db,vnpay),new ChatService(db));
    }
}
