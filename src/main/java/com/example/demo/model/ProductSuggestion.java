package com.example.demo.model;

import java.math.BigDecimal;

/** Chỉ chứa dữ liệu công khai cần cho gợi ý; ID string không mất chính xác trên JS. */
public record ProductSuggestion(String id,String title,BigDecimal price,String imagePath,String productPath) { }
