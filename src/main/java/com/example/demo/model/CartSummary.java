package com.example.demo.model;

import java.math.BigDecimal;
import java.util.*;
public record CartSummary(List<Map<String,Object>> items, BigDecimal total, String quote) {
    public List<Map<String,Object>> getItems() { return items; }
    public BigDecimal getTotal() { return total; }
    public String getQuote() { return quote; }
}
