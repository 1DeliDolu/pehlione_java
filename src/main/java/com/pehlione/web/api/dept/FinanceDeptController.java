package com.pehlione.web.api.dept;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.user.AppRole;

/**
 * Finance Department API – {@code /api/v1/dept/finance/**}
 * Access: {@code ROLE_DEPT_FINANCE} or {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/dept/finance")
@PreAuthorize("hasAnyRole('" + AppRole.ADMIN_BARE + "', '" + AppRole.DEPT_FINANCE_BARE + "')")
public class FinanceDeptController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "department", "FINANCE",
                "sections", new String[] {
                        "Revenue Overview",
                        "Invoice Management",
                        "Refund Reconciliation",
                        "Budget Tracker",
                        "Tax Reports"
                }));
    }
}
