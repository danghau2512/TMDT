package com.example.demo.verification;

import com.example.demo.model.IdentityDetails;
import com.example.demo.dto.VerificationForm;
import com.example.demo.exception.ShopException;
import java.time.*;
import java.time.format.*;
import java.text.Normalizer;
import java.util.*;

public final class IdentityParser {
    private static final DateTimeFormatter QR_DATE=DateTimeFormatter.ofPattern("ddMMuuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DISPLAY_DATE=DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private IdentityParser() { }
    public static Optional<IdentityDetails> qr(String raw) {
        if(raw==null||raw.length()>2048)return Optional.empty();
        String[] f=raw.strip().split("\\|",-1);
        if(f.length!=7||!f[0].strip().matches("[0-9]{12}")||!f[1].strip().matches("([0-9]{9}|[0-9]{12})?"))return Optional.empty();
        try {return Optional.of(details(f[0],f[2],date(f[3],QR_DATE),f[4],f[5],date(f[6],QR_DATE)));}
        catch(RuntimeException e){return Optional.empty();}
    }
    public static IdentityDetails form(VerificationForm f) {
        try {return details(f.number(),f.name(),date(f.birthDate(),DISPLAY_DATE),f.gender(),f.residence(),date(f.issueDate(),DISPLAY_DATE));}
        catch(ShopException e){throw e;}
        catch(RuntimeException e){throw new ShopException(400,"Ngày sinh và ngày cấp phải là ngày hợp lệ theo dd/MM/yyyy.");}
    }
    private static IdentityDetails details(String number,String name,LocalDate birth,String gender,String residence,LocalDate issued) {
        number=text(number,12,12,"Số định danh");if(!number.matches("[0-9]{12}"))throw new ShopException(400,"Số định danh gồm 12 chữ số.");
        name=text(name,2,120,"Họ tên");residence=text(residence,3,500,"Nơi cư trú");
        String normalized=text(gender,2,10,"Giới tính");gender=List.of("Nam","Nữ","Khác").stream().filter(x->x.equalsIgnoreCase(normalized)).findFirst().orElseThrow(()->new ShopException(400,"Chọn giới tính Nam, Nữ hoặc Khác."));
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if(birth.getYear()<1900||birth.isAfter(today)||issued.isBefore(birth)||issued.isAfter(today))throw new ShopException(400,"Kiểm tra ngày sinh và ngày cấp; ngày cấp không trước ngày sinh hoặc trong tương lai.");
        return new IdentityDetails(number,name,birth,gender,residence,issued);
    }
    private static LocalDate date(String value,DateTimeFormatter format){String normalized=Objects.requireNonNull(value).strip();if(!(format==QR_DATE?normalized.matches("[0-9]{8}"):normalized.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}")))throw new DateTimeParseException("Invalid date format","",0);return LocalDate.parse(normalized,format);}
    private static String text(String value,int min,int max,String label){String s=Normalizer.normalize(value==null?"":value.strip(),Normalizer.Form.NFC);int size=s.codePointCount(0,s.length());if(size<min||size>max||s.codePoints().anyMatch(c->Character.isISOControl(c)||c==0xfffd))throw new ShopException(400,label+" không hợp lệ hoặc vượt giới hạn.");return s;}
}
