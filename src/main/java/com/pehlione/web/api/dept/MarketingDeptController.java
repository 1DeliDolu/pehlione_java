package com.pehlione.web.api.dept;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.user.AppRole;

/**
 * Marketing Department API – {@code /api/v1/dept/marketing/**}
 * Access: {@code ROLE_DEPT_MARKETING} or {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/dept/marketing")
@PreAuthorize("hasAnyRole('" + AppRole.ADMIN_BARE + "', '" + AppRole.DEPT_MARKETING_BARE + "')")
public class MarketingDeptController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "department", "MARKETING",
                "sections", new String[] {
                        "Campaign Manager",
                        "Customer Segments",
                        "Email Campaigns",
                        "Analytics & Reports",
                        "Promotion Builder"
                }));
    }
}
