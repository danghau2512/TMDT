package com.example.demo.dto;

import java.math.BigDecimal;
public record CatalogFilter(String keyword, long category, BigDecimal minPrice, BigDecimal maxPrice, String condition, int page) { }
