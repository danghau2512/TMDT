package com.example.demo.security;

import com.example.demo.config.ApplicationRuntime;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class LocalDiagnostics {
    private LocalDiagnostics() { }
    public static boolean allowed(HttpServletRequest request, ApplicationRuntime runtime) {
        if (runtime == null || !runtime.diagnosticsEnabled()) return false;
        try { return InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress(); }
        catch (UnknownHostException exception) { return false; }
    }
}
