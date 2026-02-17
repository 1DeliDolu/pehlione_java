package com.pehlione.web.auth;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    public static String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
