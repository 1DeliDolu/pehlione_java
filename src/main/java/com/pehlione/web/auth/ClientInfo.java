package com.pehlione.web.auth;

import jakarta.servlet.http.HttpServletRequest;

public record ClientInfo(String ip, String userAgent) {

    public static ClientInfo from(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        String ip = (xff != null && !xff.isBlank())
                ? xff.split(",")[0].trim()
                : req.getRemoteAddr();
        String ua = req.getHeader("User-Agent");
        if (ua != null && ua.length() > 255) {
            ua = ua.substring(0, 255);
        }
        return new ClientInfo(ip, ua);
    }
}
