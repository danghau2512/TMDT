package com.example.demo.payment;

import com.example.demo.config.ConfigurationException;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Truststore chỉ cho client VNPAY; không thay SSLContext mặc định của JVM. */
final class VnpayTls {
    private VnpayTls() { }

    static SSLContext windowsRoot() {
        try {
            KeyStore roots = KeyStore.getInstance("Windows-ROOT");
            roots.load(null, null);
            if (roots.size() == 0) throw new IllegalStateException("Empty Windows truststore");
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(roots);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, factory.getTrustManagers(), null);
            return context;
        } catch (Exception exception) {
            // Không fallback sang trust-all khi OS/provider không hỗ trợ.
            throw new ConfigurationException("Không mở được Windows-ROOT cho vnpay.tls.useWindowsRoot. "
                    + "Cần JDK Windows hỗ trợ SunMSCAPI và kho CA hợp lệ của tài khoản chạy Tomcat; "
                    + "trên hệ điều hành khác đặt false và cấu hình truststore JDK hợp lệ.");
        }
    }
}
