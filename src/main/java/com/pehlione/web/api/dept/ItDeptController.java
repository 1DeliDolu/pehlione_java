package com.pehlione.web.api.dept;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.user.AppRole;

/**
 * IT Department API – {@code /api/v1/dept/it/**}
 * Access: {@code ROLE_DEPT_IT} or {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/dept/it")
@PreAuthorize("hasAnyRole('" + AppRole.ADMIN_BARE + "', '" + AppRole.DEPT_IT_BARE + "')")
public class ItDeptController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "department", "IT",
                "sections", new String[] {
                        "Infrastructure Status",
                        "Deployment Pipeline",
                        "Helpdesk Tickets",
                        "Security Alerts",
                        "API Health"
                }));
    }
}
