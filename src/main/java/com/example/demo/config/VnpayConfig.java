package com.example.demo.config;

import java.net.URI;
import java.util.*;

/** Backend-only secret; deliberately no record/default toString that could reveal it. */
public final class VnpayConfig {
    private final String merchant, secret, payUrl, queryUrl, returnUrl, ipnUrl;
    private final int expiryMinutes;
    private final boolean useWindowsRoot;
    public VnpayConfig(String merchant,String secret,String payUrl,String queryUrl,String returnUrl,String ipnUrl,int expiryMinutes) {
        this(merchant,secret,payUrl,queryUrl,returnUrl,ipnUrl,expiryMinutes,false);
    }
    public VnpayConfig(String merchant,String secret,String payUrl,String queryUrl,String returnUrl,String ipnUrl,int expiryMinutes,boolean useWindowsRoot) {
        if(merchant==null || !merchant.matches("[A-Za-z0-9]{8}")) fail("vnpay.tmnCode");
        if(secret==null || secret.isBlank() || secret.length()<16 || secret.contains("CHANGE_ME")) fail("vnpay.hashSecret");
        if(!"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html".equals(payUrl)) fail("vnpay.payUrl (chỉ Sandbox)");
        if(!"https://sandbox.vnpayment.vn/merchant_webapi/api/transaction".equals(queryUrl)) fail("vnpay.queryUrl (chỉ Sandbox)");
        endpoint(returnUrl,"/payments/vnpay/return","vnpay.returnUrl");
        endpoint(ipnUrl,"/payments/vnpay/ipn","vnpay.ipnUrl");
        if(expiryMinutes<5 || expiryMinutes>30) fail("vnpay.expiryMinutes (5–30)");
        this.merchant=merchant;this.secret=secret;this.payUrl=payUrl;this.queryUrl=queryUrl;this.returnUrl=returnUrl;this.ipnUrl=ipnUrl;this.expiryMinutes=expiryMinutes;
        this.useWindowsRoot=useWindowsRoot;
    }
    private static void fail(String field) { throw new ConfigurationException("Thiếu hoặc sai cấu hình "+field+". Không hiển thị giá trị."); }
    private static void endpoint(String value,String suffix,String field) {
        try { var u=URI.create(value); if(!Set.of("https","http").contains(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null || u.getQuery()!=null || u.getFragment()!=null || !u.getPath().endsWith(suffix) || value.length()>255) fail(field); }
        catch(IllegalArgumentException | NullPointerException e) { fail(field); }
    }
    public static Optional<VnpayConfig> from(Properties p,Map<String,String> env) {
        String enabled=value(p,env,"VNPAY_ENABLED","vnpay.enabled","false");
        if(!Set.of("true","false").contains(enabled)) fail("vnpay.enabled (true/false)");
        if(!Boolean.parseBoolean(enabled)) return Optional.empty();
        String windowsRoot=value(p,env,"VNPAY_TLS_USE_WINDOWS_ROOT","vnpay.tls.useWindowsRoot","false");
        if(!Set.of("true","false").contains(windowsRoot)) fail("vnpay.tls.useWindowsRoot (true/false)");
        int minutes;
        try { minutes=Integer.parseInt(value(p,env,"VNPAY_EXPIRY_MINUTES","vnpay.expiryMinutes","15")); }
        catch(NumberFormatException e) { fail("vnpay.expiryMinutes"); return Optional.empty(); }
        return Optional.of(new VnpayConfig(value(p,env,"VNPAY_TMN_CODE","vnpay.tmnCode",""),value(p,env,"VNPAY_HASH_SECRET","vnpay.hashSecret",""),
            value(p,env,"VNPAY_PAY_URL","vnpay.payUrl","https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"),
            value(p,env,"VNPAY_QUERY_URL","vnpay.queryUrl","https://sandbox.vnpayment.vn/merchant_webapi/api/transaction"),
            value(p,env,"VNPAY_RETURN_URL","vnpay.returnUrl",""),value(p,env,"VNPAY_IPN_URL","vnpay.ipnUrl",""),minutes,Boolean.parseBoolean(windowsRoot)));
    }
    private static String value(Properties p,Map<String,String> e,String env,String key,String fallback) { return e.containsKey(env)?e.get(env):p.getProperty(key,fallback); }
    public String merchant(){return merchant;} public String secret(){return secret;} public String payUrl(){return payUrl;}
    public String queryUrl(){return queryUrl;} public String returnUrl(){return returnUrl;} public String ipnUrl(){return ipnUrl;} public int expiryMinutes(){return expiryMinutes;}
    public boolean useWindowsRoot(){return useWindowsRoot;}
    @Override public String toString(){return "VnpayConfig[Sandbox, credential hidden]";}
}
