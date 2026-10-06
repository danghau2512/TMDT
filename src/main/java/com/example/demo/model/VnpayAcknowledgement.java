package com.example.demo.model;
public record VnpayAcknowledgement(String RspCode,String Message) {
    public static VnpayAcknowledgement of(String code){return new VnpayAcknowledgement(code,switch(code){case "00"->"Confirm Success";case "02"->"Order already confirmed";case "01"->"Order not found";case "04"->"Invalid amount";case "97"->"Invalid signature";default->"Processing error";});}
}
