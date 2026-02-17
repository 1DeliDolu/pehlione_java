package com.pehlione.web.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository repo;

    public RefreshTokenCleanupJob(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    @Scheduled(fixedDelayString = "PT6H")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(14, ChronoUnit.DAYS);
        repo.deleteOld(now, cutoff);
    }
}
