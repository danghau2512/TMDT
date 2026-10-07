package com.example.demo.verification;

import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Sáu trường và số định danh hoàn toàn giả, không dùng căn cước thật. */
public final class QrFixtures {
    public static final String A="000000000021||HỌ TÊN GIẢ DEMO|29022000|Nữ|Địa chỉ giả, phường thử nghiệm|01012021";
    public static final String B="000000000022|000000000|HỌ TÊN GIẢ KHÁC|01012001|Nam|Nơi cư trú giả để kiểm thử|02022022";
    private QrFixtures() { }
    public static byte[] qr(String raw)throws Exception{return png(MatrixToImageWriter.toBufferedImage(new QRCodeWriter().encode(raw,BarcodeFormat.QR_CODE,600,600,Map.of(EncodeHintType.CHARACTER_SET,"UTF-8"))));}
    public static byte[] blank()throws Exception{return png(new BufferedImage(640,400,BufferedImage.TYPE_INT_RGB));}
    public static byte[] jpeg(byte[] png)throws Exception {var input=ImageIO.read(new ByteArrayInputStream(png));var image=new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{g.drawImage(input,0,0,null);}finally{g.dispose();}var out=new ByteArrayOutputStream();ImageIO.write(image,"jpeg",out);return out.toByteArray();}
    public static byte[] rotate(byte[] bytes)throws Exception {var input=ImageIO.read(new ByteArrayInputStream(bytes));var result=new BufferedImage(input.getHeight(),input.getWidth(),BufferedImage.TYPE_INT_RGB);var g=result.createGraphics();try{g.translate(input.getHeight(),0);g.rotate(Math.PI/2);g.drawImage(input,0,0,null);}finally{g.dispose();}return png(result);}
    public static byte[] multiple()throws Exception {var combined=new BufferedImage(1280,680,BufferedImage.TYPE_INT_RGB);var g=combined.createGraphics();try{g.setColor(Color.WHITE);g.fillRect(0,0,1280,680);g.drawImage(ImageIO.read(new ByteArrayInputStream(qr(A))),20,40,null);g.drawImage(ImageIO.read(new ByteArrayInputStream(qr(B))),660,40,null);}finally{g.dispose();}return png(combined);}
    private static byte[] png(BufferedImage image)throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}
}
