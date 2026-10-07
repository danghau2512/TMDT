package com.example.demo.model;

import java.time.LocalDate;

/** DTO riêng tư sáu trường; tránh toString mặc định vô tình ghi giấy tờ vào log. */
public record IdentityDetails(String number,String name,LocalDate birthDate,String gender,String residence,LocalDate issueDate) {
    @Override public String toString(){return "IdentityDetails[private]";}
}
