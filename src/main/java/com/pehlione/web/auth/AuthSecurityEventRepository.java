package com.pehlione.web.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSecurityEventRepository extends JpaRepository<AuthSecurityEvent, Long> {
}
