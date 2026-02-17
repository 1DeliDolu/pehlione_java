package com.pehlione.web.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("select rt.session.id from RefreshToken rt where rt.tokenHash = :tokenHash")
    Optional<Long> findSessionIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked=true where rt.session.id = :sessionId and rt.revoked=false")
    int revokeAllActiveForSession(@Param("sessionId") Long sessionId);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    @Query(value = "update refresh_tokens set revoked=true where user_id = :userId and revoked=false", nativeQuery = true)
    int revokeAllActiveForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshToken rt where rt.expiresAt < :now or (rt.revoked = true and rt.lastUsedAt < :cutoff)")
    int deleteOld(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
