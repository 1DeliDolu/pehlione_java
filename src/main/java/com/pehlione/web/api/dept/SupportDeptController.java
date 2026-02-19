package com.pehlione.web.api.dept;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.user.AppRole;

/**
 * Support Department API – {@code /api/v1/dept/support/**}
 * Access: {@code ROLE_DEPT_SUPPORT} or {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/dept/support")
@PreAuthorize("hasAnyRole('" + AppRole.ADMIN_BARE + "', '" + AppRole.DEPT_SUPPORT_BARE + "')")
public class SupportDeptController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "department", "SUPPORT",
                "sections", new String[] {
                        "Open Tickets",
                        "Escalations",
                        "Refund Requests",
                        "Customer Contact Log",
                        "Knowledge Base"
                }));
    }
}
