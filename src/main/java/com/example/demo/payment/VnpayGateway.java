package com.example.demo.payment;
import java.util.Map;
/** Có thể inject transport trong test; source production chỉ gọi endpoint Sandbox cố định. */
@FunctionalInterface public interface VnpayGateway { Map<String,String> query(Map<String,String> request) throws Exception; }
