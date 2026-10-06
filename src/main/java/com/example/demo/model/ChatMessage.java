package com.example.demo.model;

/** Chỉ dữ liệu hiển thị; ID dạng chuỗi tránh mất chính xác ở JavaScript. */
public record ChatMessage(String id, String body, String sentAt, boolean mine) {
    public String getId(){return id;}
    public String getBody(){return body;}
    public String getSentAt(){return sentAt;}
    public boolean isMine(){return mine;}
}
