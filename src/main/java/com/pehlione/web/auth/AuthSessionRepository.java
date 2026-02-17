package com.pehlione.web.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    List<AuthSession> findByUserIdOrderByLastSeenAtDesc(Long userId);

    Optional<AuthSession> findByPublicIdAndUserId(String publicId, Long userId);

    @Transactional
    @Modifying
    @Query("""
            update AuthSession s
            set s.lastSeenAt = :now
            where s.publicId = :publicId
              and (s.lastSeenAt is null or s.lastSeenAt < :staleBefore)
              and s.revoked = false
            """)
    int touchIfStale(@Param("publicId") String publicId,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore);
}
