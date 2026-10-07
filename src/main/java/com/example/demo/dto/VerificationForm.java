package com.example.demo.dto;

public record VerificationForm(String name,String number,String birthDate,String gender,String residence,String issueDate) {
    @Override public String toString(){return "VerificationForm[private]";}
}
