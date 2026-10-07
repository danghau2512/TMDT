package com.example.demo.config;

import java.nio.file.Path;
import java.util.*;

/** Đọc QR cục bộ, chỉ cần cấu hình nơi lưu giấy tờ riêng tư. */
public record VerificationConfig(Path root) {
    public static VerificationConfig defaults(Path uploads){return new VerificationConfig(uploads.resolveSibling("seller-documents"));}
    public static VerificationConfig from(Properties p,Map<String,String> env,Path uploads) {
        Path root;
        try {String value=env.getOrDefault("VERIFICATION_ROOT",p.getProperty("verification.root",""));root=value.isBlank()?uploads.resolveSibling("seller-documents"):Path.of(value);}
        catch(Exception e){throw new ConfigurationException("VERIFICATION_ROOT / verification.root không hợp lệ.");}
        if(!root.isAbsolute() || root.normalize().startsWith(uploads.normalize()) || uploads.normalize().startsWith(root.normalize()))throw new ConfigurationException("verification.root phải tuyệt đối, riêng biệt với upload.root và ngoài WAR.");
        return new VerificationConfig(root.normalize());
    }
}
