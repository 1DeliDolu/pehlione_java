package com.pehlione.web.api.dept;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.user.AppRole;

/**
 * HR Department API – {@code /api/v1/dept/hr/**}
 *
 * Access: {@code ROLE_DEPT_HR} or {@code ROLE_ADMIN}.
 * The @PreAuthorize on the class acts as the default guard;
 * individual methods may further restrict with stricter roles.
 */
@RestController
@RequestMapping("/api/v1/dept/hr")
@PreAuthorize("hasAnyRole('" + AppRole.ADMIN_BARE + "', '" + AppRole.DEPT_HR_BARE + "')")
public class HrDeptController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "department", "HR",
                "sections", new String[] {
                        "Employee Management",
                        "Recruitment Pipeline",
                        "Payroll Overview",
                        "Leave Requests",
                        "Performance Reviews"
                }));
    }
}
