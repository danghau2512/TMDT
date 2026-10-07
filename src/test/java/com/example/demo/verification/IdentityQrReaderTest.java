package com.example.demo.verification;

import com.example.demo.dto.VerificationForm;
import com.example.demo.config.*;
import com.example.demo.exception.ShopException;
import java.time.LocalDate;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentityQrReaderTest {
    @Test void sevenFieldsKeepVietnameseEmptyOldNumberAndStrictDates(){var parsed=IdentityParser.qr(QrFixtures.A).orElseThrow();assertEquals("HỌ TÊN GIẢ DEMO",parsed.name());assertEquals("000000000021",parsed.number());assertEquals(LocalDate.of(2000,2,29),parsed.birthDate());assertEquals("Nữ",parsed.gender());assertEquals(LocalDate.of(2021,1,1),parsed.issueDate());assertTrue(parsed.residence().contains("phường"));assertTrue(IdentityParser.qr(QrFixtures.B).isPresent());}
    @Test void unsupportedOrInvalidFormatsNeverInventIdentity(){for(String raw:List.of("https://example.com/",QrFixtures.A.replace("29022000","29022001"),QrFixtures.A.replace("01012021","31022021"),QrFixtures.A+"|extra",QrFixtures.A.replace("||","|bad|")))assertTrue(IdentityParser.qr(raw).isEmpty());assertThrows(ShopException.class,()->IdentityParser.form(new VerificationForm("GIẢ DEMO","000000000021","29/02/2001","Nữ","Địa chỉ giả","01/01/2021")));}
    @Test void readsEitherFaceAndSimpleRotations()throws Exception {var reader=new IdentityQrReader();assertEquals("SUCCESS",reader.read(QrFixtures.qr(QrFixtures.A),QrFixtures.blank()).status());assertEquals("SUCCESS",reader.read(QrFixtures.jpeg(QrFixtures.qr(QrFixtures.A)),QrFixtures.blank()).status());var back=reader.read(QrFixtures.blank(),QrFixtures.rotate(QrFixtures.qr(QrFixtures.A)));assertEquals("SUCCESS",back.status());assertEquals("HỌ TÊN GIẢ DEMO",back.details().name());}
    @Test void duplicatesAcceptedButConflictingIdentitiesBlocked()throws Exception {var reader=new IdentityQrReader();assertEquals("SUCCESS",reader.read(QrFixtures.qr(QrFixtures.A),QrFixtures.qr(QrFixtures.A)).status());assertEquals("CONFLICT",reader.read(QrFixtures.qr(QrFixtures.A),QrFixtures.qr(QrFixtures.B)).status());assertEquals("CONFLICT",reader.read(QrFixtures.multiple(),QrFixtures.blank()).status());}
    @Test void absentAndUnrelatedQrReturnDistinctManualFallbacks()throws Exception {var reader=new IdentityQrReader();assertEquals("NOT_FOUND",reader.read(QrFixtures.blank(),QrFixtures.blank()).status());assertEquals("UNSUPPORTED",reader.read(QrFixtures.qr("https://example.com/"),QrFixtures.blank()).status());}
    @Test void oldOcrConfigurationIgnoredAndPrivateRootProtected(){var p=new Properties();p.setProperty("ocr.apiKey","unused-fixture-key");p.setProperty("ocr.endpoint","not-a-url");var root=Path.of(System.getProperty("java.io.tmpdir"),"qr-products");assertNotNull(VerificationConfig.from(p,Map.of("OCR_API_KEY","unused"),root));p.setProperty("verification.root",root.toString());assertThrows(ConfigurationException.class,()->VerificationConfig.from(p,Map.of(),root));}
}
