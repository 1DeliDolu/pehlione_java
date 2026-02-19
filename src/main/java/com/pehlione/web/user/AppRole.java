package com.pehlione.web.user;

/**
 * Canonical constants for all application roles.
 *
 * <h3>Structure</h3>
 * <ul>
 * <li>{@code ROLE_ADMIN} – super-admin, full access</li>
 * <li>{@code ROLE_USER} – registered customer (default after sign-up)</li>
 * <li>{@code ROLE_WORKER / ROLE_EMPLOYEE / ROLE_STAFF} – fulfilment / ops
 * workers</li>
 * <li>Department roles – one per business unit, prefix {@code ROLE_DEPT_}</li>
 * <li>Customer tier roles – loyalty level, prefix {@code ROLE_TIER_}</li>
 * </ul>
 *
 * Usage in {@code @PreAuthorize}:
 * 
 * <pre>{@code
 * @PreAuthorize("hasRole('"+AppRole.DEPT_HR_BARE+"')")
 * }</pre>
 *
 * Or in {@code SecurityConfig}:
 * 
 * <pre>{@code
 * .hasRole(AppRole.DEPT_HR_BARE)   // Spring strips ROLE_ prefix automatically
 * }</pre>
 */
public final class AppRole {

    private AppRole() {
    }

    // ── Core roles ────────────────────────────────────────────────────────────
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String USER = "ROLE_USER";
    public static final String WORKER = "ROLE_WORKER";
    public static final String EMPLOYEE = "ROLE_EMPLOYEE";
    public static final String STAFF = "ROLE_STAFF";

    // Bare names (without ROLE_ prefix) used with hasRole() / .hasRole()
    public static final String ADMIN_BARE = "ADMIN";
    public static final String USER_BARE = "USER";
    public static final String WORKER_BARE = "WORKER";

    // ── Department roles ──────────────────────────────────────────────────────
    public static final String DEPT_HR = "ROLE_DEPT_HR";
    public static final String DEPT_IT = "ROLE_DEPT_IT";
    public static final String DEPT_PROCESS = "ROLE_DEPT_PROCESS";
    public static final String DEPT_MARKETING = "ROLE_DEPT_MARKETING";
    public static final String DEPT_FINANCE = "ROLE_DEPT_FINANCE";
    public static final String DEPT_SUPPORT = "ROLE_DEPT_SUPPORT";

    // Bare department names (no prefix)
    public static final String DEPT_HR_BARE = "DEPT_HR";
    public static final String DEPT_IT_BARE = "DEPT_IT";
    public static final String DEPT_PROCESS_BARE = "DEPT_PROCESS";
    public static final String DEPT_MARKETING_BARE = "DEPT_MARKETING";
    public static final String DEPT_FINANCE_BARE = "DEPT_FINANCE";
    public static final String DEPT_SUPPORT_BARE = "DEPT_SUPPORT";

    // ── Customer tier roles ───────────────────────────────────────────────────
    public static final String TIER_BRONZE = "ROLE_TIER_BRONZE";
    public static final String TIER_SILVER = "ROLE_TIER_SILVER";
    public static final String TIER_GOLD = "ROLE_TIER_GOLD";
    public static final String TIER_PLATINUM = "ROLE_TIER_PLATINUM";

    public static final String TIER_BRONZE_BARE = "TIER_BRONZE";
    public static final String TIER_SILVER_BARE = "TIER_SILVER";
    public static final String TIER_GOLD_BARE = "TIER_GOLD";
    public static final String TIER_PLATINUM_BARE = "TIER_PLATINUM";

    // Convenience arrays
    public static final String[] ALL_DEPT_ROLES = {
            DEPT_HR, DEPT_IT, DEPT_PROCESS, DEPT_MARKETING, DEPT_FINANCE, DEPT_SUPPORT
    };

    public static final String[] ALL_TIER_ROLES = {
            TIER_BRONZE, TIER_SILVER, TIER_GOLD, TIER_PLATINUM
    };
}
