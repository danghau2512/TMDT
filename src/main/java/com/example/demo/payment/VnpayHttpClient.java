package com.example.demo.payment;

import com.example.demo.config.VnpayConfig;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import javax.net.ssl.SSLParameters;

public final class VnpayHttpClient implements VnpayGateway {
    private final VnpayConfig config;
    private final HttpClient client;
    public VnpayHttpClient(VnpayConfig config){
        this.config=config;
        var builder=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER);
        if(config.useWindowsRoot()) builder.sslContext(VnpayTls.windowsRoot());
        var parameters=new SSLParameters();parameters.setEndpointIdentificationAlgorithm("HTTPS");
        client=builder.sslParameters(parameters).build();
    }
    /** Chỉ kiểm tra HTTPS; không gửi secret, querydr hoặc dữ liệu thanh toán. */
    public int checkTls() throws Exception {
        var request=HttpRequest.newBuilder(URI.create(config.queryUrl())).timeout(Duration.ofSeconds(15))
                .method("HEAD",HttpRequest.BodyPublishers.noBody()).build();
        return client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode();
    }
    @Override public Map<String,String> query(Map<String,String> fields) throws Exception {
        var req=HttpRequest.newBuilder(URI.create(config.queryUrl())).timeout(Duration.ofSeconds(15)).header("Content-Type","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(fields))).build();
        var reply=client.send(req,HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        if(reply.statusCode()!=200 || reply.body().length()>65536) throw new IllegalStateException("Gateway không khả dụng.");
        return parse(reply.body());
    }
    static Map<String,String> parse(String json) throws Exception {
        var fields=new HashMap<String,String>();
        try(var r=new JsonReader(new StringReader(json))){r.setStrictness(com.google.gson.Strictness.STRICT);r.beginObject();
            while(r.hasNext()){String key=r.nextName(); if(fields.containsKey(key))throw new IllegalArgumentException("Duplicate gateway field");
                String value; if(r.peek()==com.google.gson.stream.JsonToken.NULL){r.nextNull();value="";}else value=r.nextString();if(key.length()>64 || value.length()>1024)throw new IllegalArgumentException("Gateway field too long");fields.put(key,value);}
            r.endObject();if(r.peek()!=com.google.gson.stream.JsonToken.END_DOCUMENT)throw new IllegalArgumentException("Gateway JSON dư dữ liệu");}
        return fields;
    }
}
