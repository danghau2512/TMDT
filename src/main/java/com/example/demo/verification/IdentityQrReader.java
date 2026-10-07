package com.example.demo.verification;

import com.example.demo.model.IdentityDetails;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/** Không mở URL, chạy lệnh, gửi mạng hoặc giữ chuỗi QR trong kết quả/log. */
public final class IdentityQrReader {
    public record Outcome(String status,IdentityDetails details) {
        @Override public String toString(){return "QrOutcome["+status+"]";}
    }
    public Outcome read(byte[] front,byte[] back) {
        var values=new HashSet<IdentityDetails>();var texts=new HashSet<String>();
        for(byte[] bytes:new byte[][]{front,back}) {
            BufferedImage image;
            try {image=image(bytes);}catch(IOException e){continue;}
            if(image==null)continue;
            LuminanceSource source=new BufferedImageLuminanceSource(image);
            for(int turn=0;turn<4;turn++) {
                try {var results=new QRCodeMultiReader().decodeMultiple(new BinaryBitmap(new HybridBinarizer(source)),Map.of(DecodeHintType.TRY_HARDER,true,DecodeHintType.CHARACTER_SET,"UTF-8"));
                    for(var result:results){String text=result.getText();texts.add(text);if(texts.size()>32)return new Outcome("CONFLICT",null);IdentityParser.qr(text).ifPresent(values::add);}
                }catch(NotFoundException ignored){ }
                source=source.rotateCounterClockwise();
            }
        }
        if(values.size()>1)return new Outcome("CONFLICT",null);
        if(values.size()==1)return new Outcome("SUCCESS",values.iterator().next());
        return new Outcome(texts.isEmpty()?"NOT_FOUND":"UNSUPPORTED",null);
    }
    private BufferedImage image(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length>5*1024*1024)return null;
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())return null;var reader=readers.next();
            try {reader.setInput(input);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||(long)w*h>20_000_000)return null;var image=reader.read(0);
                if((long)w*h<=4_000_000)return image;
                double ratio=Math.sqrt(4_000_000d/((long)w*h));var scaled=new BufferedImage(Math.max(1,(int)(w*ratio)),Math.max(1,(int)(h*ratio)),BufferedImage.TYPE_INT_RGB);var graphics=scaled.createGraphics();
                try {graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);graphics.drawImage(image,0,0,scaled.getWidth(),scaled.getHeight(),null);}finally{graphics.dispose();}return scaled;
            }finally{reader.dispose();}
        }
    }
}
