package com.example.demo.security;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

class CsrfTokensTest {
    private static HttpSession session() {
        var attributes = new HashMap<String, Object>();
        return (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(), new Class[]{HttpSession.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getAttribute" -> attributes.get(args[0]);
            case "setAttribute" -> { attributes.put((String) args[0], args[1]); yield null; }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }
    @Test void requiresExistingSessionAndExactToken() {
        var session = session();
        assertFalse(CsrfTokens.valid(null, "token"));
        assertFalse(CsrfTokens.valid(session, null));
        String token = CsrfTokens.token(session);
        assertEquals(token, CsrfTokens.token(session));
        assertTrue(CsrfTokens.valid(session, token));
        assertFalse(CsrfTokens.valid(session, "x".repeat(43)));
        assertFalse(CsrfTokens.valid(session, token + "x"));
        assertFalse(CsrfTokens.valid(session(), token));
    }
    @Test void rotationInvalidatesPriorToken() {
        var session = session();
        String previous = CsrfTokens.token(session);
        String current = CsrfTokens.rotate(session);
        assertNotEquals(previous, current);
        assertFalse(CsrfTokens.valid(session, previous));
        assertTrue(CsrfTokens.valid(session, current));
    }
}
