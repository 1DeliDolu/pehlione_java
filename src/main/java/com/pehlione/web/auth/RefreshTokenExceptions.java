package com.pehlione.web.auth;

public class RefreshTokenExceptions {

    public static class RefreshNotFound extends RuntimeException {
    }

    public static class RefreshRevoked extends RuntimeException {
    }

    public static class RefreshExpired extends RuntimeException {
    }

    public static class RefreshReuseDetected extends RuntimeException {
        private final Long userId;
        private final String userEmail;

        public RefreshReuseDetected(Long userId, String userEmail) {
            this.userId = userId;
            this.userEmail = userEmail;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUserEmail() {
            return userEmail;
        }
    }
}
