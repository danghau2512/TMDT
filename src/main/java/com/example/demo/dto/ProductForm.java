package com.example.demo.dto;

import java.math.BigDecimal;
public record ProductForm(String title, long categoryId, String description, BigDecimal price,
                          String condition, int stock, String visibility, long sellerId,
                          long listingVersion, long stockVersion, String reason, ConditionForm declaration) {
    public ProductForm(String title,long categoryId,String description,BigDecimal price,String condition,int stock,String visibility,long sellerId,long listingVersion,long stockVersion,String reason) {
        this(title,categoryId,description,price,condition,stock,visibility,sellerId,listingVersion,stockVersion,reason,null);
    }
}
