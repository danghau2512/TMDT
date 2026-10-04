package com.example.demo.controller;

import com.example.demo.config.ApplicationRuntime;
import com.example.demo.exception.AccountUnavailableException;
import com.example.demo.service.HealthService;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class AccountSupportTest {
    private static ServletContext context(ApplicationRuntime runtime) {
        return (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(), new Class[]{ServletContext.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName())) return ApplicationRuntime.CONTEXT_KEY.equals(args[0]) ? runtime : null;
                    throw new UnsupportedOperationException(method.getName());
                });
    }
    @Test void missingDatabaseHasSpecificServerDiagnosticAndSafePublicMessage() {
        var runtime = new ApplicationRuntime(true, false, Optional.empty(), new HealthService(Optional.empty()));
        var error = assertThrows(AccountUnavailableException.class, () -> AccountSupport.service(context(runtime)));
        assertEquals(AccountUnavailableException.Reason.DATABASE_NOT_CONFIGURED, error.reason());
        assertNull(error.getCause());
        assertFalse(error.getMessage().contains("APP_CONFIG_FILE"));
    }
    @Test void invalidConfigurationAndMissingRuntimeAreDistinguished() {
        var runtime = new ApplicationRuntime(false, false, Optional.empty(), new HealthService(Optional.empty()));
        assertEquals(AccountUnavailableException.Reason.DATABASE_CONFIGURATION_INVALID,
                assertThrows(AccountUnavailableException.class, () -> AccountSupport.service(context(runtime))).reason());
        assertEquals(AccountUnavailableException.Reason.SERVICE_NOT_INITIALIZED,
                assertThrows(AccountUnavailableException.class, () -> AccountSupport.service(context(null))).reason());
    }
}
