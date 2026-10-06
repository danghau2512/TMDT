package com.example.demo.dto;

public record CheckoutForm(String name, String phone, String address, String note, String method,
                           String key, String quote) {
    @Override public String toString() { return "CheckoutForm[redacted]"; }
}
