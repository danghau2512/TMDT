package com.example.demo.payment;

import com.example.demo.config.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VnpayProtocolTest {
    static final String SECRET="test-secret-not-a-credential";
    static VnpayConfig config(){return new VnpayConfig("TEST0001",SECRET,"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html","https://sandbox.vnpayment.vn/merchant_webapi/api/transaction","http://localhost:18080/c2c/payments/vnpay/return","http://localhost:18080/c2c/payments/vnpay/ipn",15);}
    @Test void paymentSignatureMatchesIndependentPythonVectorAndEncodingIsOnce(){
        var fields=new HashMap<>(Map.of("vnp_OrderInfo","A B+C%","vnp_Amount","10000000","vnp_ReturnUrl","http://localhost:18080/c2c/payments/vnpay/return","vnp_SecureHashType","ignored"));
        String data="vnp_Amount=10000000&vnp_OrderInfo=A+B%2BC%25&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A18080%2Fc2c%2Fpayments%2Fvnpay%2Freturn";
        assertEquals(data,VnpayProtocol.canonical(fields));
        String hash="7ce61325179e68c44a1a329d2c21c56826cb84c5e2b183a882db01003f922eb20f7f02c78617ff2746199c82573e957c0340f4b4073921a3b47f5b871998b649";
        assertEquals(hash,VnpayProtocol.hmac(SECRET,data));fields.put("vnp_SecureHash",hash.toUpperCase());assertTrue(VnpayProtocol.verify(config(),fields));
        fields.put("vnp_Amount","10000001");assertFalse(VnpayProtocol.verify(config(),fields));
    }
    @Test void queryUsesOfficialPipeOrderWithEmptyOptionalFields(){
        var req=Map.of("vnp_RequestId","request","vnp_Version","2.1.0","vnp_Command","querydr","vnp_TmnCode","TEST0001","vnp_TxnRef","reference","vnp_TransactionDate","20261005093000","vnp_CreateDate","20261005100000","vnp_IpAddr","127.0.0.1","vnp_OrderInfo","Kiem tra don 1");
        assertEquals("3fb58fe73a4d0d358900c25a4f08c129f25b428d81a82bf21033a93562c87b07ecc0f822a0a9045b66cc72e149d40d858cd4f21ccb46ee37eaae67b23c52a55f",VnpayProtocol.hmac(SECRET,VnpayProtocol.queryData(req,false)));
        var response=new HashMap<String,String>();
        response.putAll(Map.of("vnp_ResponseId","response","vnp_Command","querydr","vnp_ResponseCode","00","vnp_Message","Success","vnp_TmnCode","TEST0001","vnp_TxnRef","reference","vnp_Amount","10000000","vnp_BankCode","NCB","vnp_PayDate","20261005100000","vnp_TransactionNo","123456"));
        response.putAll(Map.of("vnp_TransactionType","01","vnp_TransactionStatus","00","vnp_OrderInfo","Thanh toan don 1"));
        response.put("vnp_SecureHash","b9c07c54f8bc6799429d3d6ec344ac619bd4b8d01ad968062d59727682fff8700094f3ba30519c3518b7a0e318ce6d5ee92a157574d21d6850400f2db11b18a6");
        assertTrue(VnpayProtocol.verifyQuery(config(),response));response.put("vnp_TransactionStatus","01");assertFalse(VnpayProtocol.verifyQuery(config(),response));
    }
    @Test void exactAmountAndVietnamTimeAndInitialTransactionDate(){
        assertEquals("12345678",VnpayProtocol.amount(new BigDecimal("123456.78")));
        assertThrows(ArithmeticException.class,()->VnpayProtocol.amount(new BigDecimal("1.001")));
        assertThrows(IllegalArgumentException.class,()->VnpayProtocol.amount(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,()->VnpayProtocol.amount(new BigDecimal("10000000000")));
        assertEquals("20261005100000",VnpayProtocol.date(Instant.parse("2026-10-05T03:00:00Z")));
        assertFalse(VnpayProtocol.validDate("20260230000000"));
        var req=VnpayProtocol.queryRequest(config(),Map.of("txn_ref","reference","vnp_create_date","20261005093000","order_id",1),"127.0.0.1",Instant.parse("2026-10-05T03:00:00Z"));
        assertEquals("20261005093000",req.get("vnp_TransactionDate"));assertEquals("20261005100000",req.get("vnp_CreateDate"));
    }
    @Test void configurationIsBackendOnlyAndRejectsProductionUrls(){
        assertFalse(config().toString().contains(SECRET));
        assertThrows(ConfigurationException.class,()->new VnpayConfig("TEST0001",SECRET,"https://pay.vnpay.vn/vpcpay.html",config().queryUrl(),config().returnUrl(),config().ipnUrl(),15));
        var p=new Properties();p.setProperty("vnpay.enabled","typo");assertThrows(ConfigurationException.class,()->VnpayConfig.from(p,Map.of()));
    }
    @Test void gatewayJsonRejectsDuplicateFinancialFields() throws Exception {
        assertEquals("00",VnpayHttpClient.parse("{\"vnp_ResponseCode\":\"00\"}").get("vnp_ResponseCode"));
        assertThrows(Exception.class,()->VnpayHttpClient.parse("{\"vnp_Amount\":\"100\",\"vnp_Amount\":\"200\"}"));
    }
    @Test void windowsTruststoreIsOptInAndEnvironmentOverridesFileWithoutLeakingValues() {
        var p=new Properties();
        p.setProperty("vnpay.enabled","true");p.setProperty("vnpay.tmnCode","TEST0001");p.setProperty("vnpay.hashSecret",SECRET);
        p.setProperty("vnpay.returnUrl",config().returnUrl());p.setProperty("vnpay.ipnUrl",config().ipnUrl());
        assertFalse(VnpayConfig.from(p,Map.of()).orElseThrow().useWindowsRoot());
        p.setProperty("vnpay.tls.useWindowsRoot","true");
        assertTrue(VnpayConfig.from(p,Map.of()).orElseThrow().useWindowsRoot());
        assertFalse(VnpayConfig.from(p,Map.of("VNPAY_TLS_USE_WINDOWS_ROOT","false")).orElseThrow().useWindowsRoot());
        var error=assertThrows(ConfigurationException.class,()->VnpayConfig.from(p,Map.of("VNPAY_TLS_USE_WINDOWS_ROOT",SECRET)));
        assertTrue(error.getMessage().contains("vnpay.tls.useWindowsRoot"));assertFalse(error.getMessage().contains(SECRET));
    }
}
