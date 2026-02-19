package com.pehlione.web.api.dept;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pehlione.web.user.AppRole;
import com.pehlione.web.user.CustomerTier;
import com.pehlione.web.user.Department;

/**
 * Department-agnostic endpoints:
 *
 * <ul>
 * <li>{@code GET /api/v1/dept/me} – which departments the caller belongs
 * to</li>
 * <li>{@code GET /api/v1/dept/list} – static list of all departments (admin
 * only)</li>
 * <li>{@code GET /api/v1/tier/me} – caller's customer-tier info</li>
 * <li>{@code GET /api/v1/tier/benefits} – all tier benefit table
 * (authenticated)</li>
 * </ul>
 */
@RestController
public class DeptInfoController {

    // ── Department discovery ──────────────────────────────────────────────────

    /** Returns the department(s) the authenticated user belongs to. */
    @GetMapping("/api/v1/dept/me")
    public ResponseEntity<Map<String, Object>> myDepartments(Authentication auth) {
        List<String> deptRoles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_DEPT_"))
                .toList();

        List<Map<String, String>> departments = deptRoles.stream()
                .map(roleName -> {
                    Department d = Department.fromRoleName(roleName);
                    return d != null
                            ? Map.of("role", roleName, "slug", d.getSlug(), "description", d.getDescription())
                            : Map.of("role", roleName, "slug", "unknown", "description", "");
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "email", auth.getName(),
                "departments", departments));
    }

    /** Admin-only – lists all available departments with metadata. */
    @GetMapping("/api/v1/dept/list")
    @PreAuthorize("hasRole('" + AppRole.ADMIN_BARE + "')")
    public ResponseEntity<List<Map<String, String>>> listDepartments() {
        List<Map<String, String>> result = Arrays.stream(Department.values())
                .map(d -> Map.of(
                        "slug", d.getSlug(),
                        "role", d.getRoleName(),
                        "description", d.getDescription()))
                .toList();
        return ResponseEntity.ok(result);
    }

    // ── Customer tier info ────────────────────────────────────────────────────

    /** Returns the authenticated user's customer-tier information. */
    @GetMapping("/api/v1/tier/me")
    public ResponseEntity<Map<String, Object>> myTier(Authentication auth) {
        String tierRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_TIER_"))
                .findFirst()
                .orElse(null);

        CustomerTier tier = CustomerTier.fromRoleName(tierRole);

        if (tier == null) {
            return ResponseEntity.ok(Map.of(
                    "tier", "NONE",
                    "message", "No customer tier assigned"));
        }

        return ResponseEntity.ok(Map.of(
                "tier", tier.name(),
                "role", tier.getRoleName(),
                "minOrders", tier.getMinOrders(),
                "maxDiscountPct", tier.getMaxDiscountPct(),
                "supportSlaHours", tier.getSupportSlaHours()));
    }

    /** Returns the full tier benefit table (any authenticated user may view). */
    @GetMapping("/api/v1/tier/benefits")
    public ResponseEntity<List<Map<String, Object>>> tierBenefits() {
        List<Map<String, Object>> result = Arrays.stream(CustomerTier.values())
                .map(t -> Map.<String, Object>of(
                        "tier", t.name(),
                        "role", t.getRoleName(),
                        "minOrders", t.getMinOrders(),
                        "maxDiscountPct", t.getMaxDiscountPct(),
                        "supportSlaHours", t.getSupportSlaHours()))
                .toList();
        return ResponseEntity.ok(result);
    }
}
