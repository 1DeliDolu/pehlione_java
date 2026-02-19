package com.pehlione.web.user;

/**
 * Company departments.
 * Each department maps 1-to-1 with a {@code ROLE_DEPT_*} Spring Security role
 * and surfaces its own API endpoint namespace under
 * {@code /api/v1/dept/{slug}/}.
 */
public enum Department {

    HR("hr", AppRole.DEPT_HR, AppRole.DEPT_HR_BARE,
            "Human Resources – employee management, payroll, recruitment"),
    IT("it", AppRole.DEPT_IT, AppRole.DEPT_IT_BARE,
            "Information Technology – infrastructure, devops, helpdesk"),
    PROCESS("process", AppRole.DEPT_PROCESS, AppRole.DEPT_PROCESS_BARE,
            "Process & Quality – workflow, compliance, audit"),
    MARKETING("marketing", AppRole.DEPT_MARKETING, AppRole.DEPT_MARKETING_BARE,
            "Marketing – campaigns, analytics, content"),
    FINANCE("finance", AppRole.DEPT_FINANCE, AppRole.DEPT_FINANCE_BARE,
            "Finance – accounting, budgeting, invoicing"),
    SUPPORT("support", AppRole.DEPT_SUPPORT, AppRole.DEPT_SUPPORT_BARE,
            "Customer Support – tickets, escalations, refunds");

    /** URL slug used in API paths, e.g. {@code /api/v1/dept/hr/}. */
    private final String slug;

    /** Full Spring Security role name (includes {@code ROLE_} prefix). */
    private final String roleName;

    /** Bare Spring Security authority (without {@code ROLE_} prefix). */
    private final String bareRole;

    /** Human-readable description. */
    private final String description;

    Department(String slug, String roleName, String bareRole, String description) {
        this.slug = slug;
        this.roleName = roleName;
        this.bareRole = bareRole;
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getBareRole() {
        return bareRole;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Looks up a {@link Department} by its URL slug (case-insensitive).
     * Returns {@code null} when not found.
     */
    public static Department fromSlug(String slug) {
        if (slug == null)
            return null;
        for (Department d : values()) {
            if (d.slug.equalsIgnoreCase(slug))
                return d;
        }
        return null;
    }

    /**
     * Looks up a {@link Department} by the full role name.
     */
    public static Department fromRoleName(String roleName) {
        if (roleName == null)
            return null;
        for (Department d : values()) {
            if (d.roleName.equals(roleName))
                return d;
        }
        return null;
    }
}
