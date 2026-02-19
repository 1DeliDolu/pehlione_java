package com.pehlione.web.api.dept;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.user.AppRole;

/**
 * Process & Quality Department API – {@code /api/v1/dept/process/**}
 * Access: {@code ROLE_DEPT_PROCESS} or {@code ROLE_ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/dept/process")
@PreAuthorize("hasAnyRole('" + AppRole.ADMIN_BARE + "', '" + AppRole.DEPT_PROCESS_BARE + "')")
public class ProcessDeptController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "department", "PROCESS",
                "sections", new String[] {
                        "Workflow Overview",
                        "Compliance Checklist",
                        "Audit Trail",
                        "KPI Dashboard",
                        "Process Improvement Log"
                }));
    }
}
