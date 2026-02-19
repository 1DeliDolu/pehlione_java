package com.pehlione.web.user;

/**
 * Customer loyalty tier.
 * Tiers unlock progressively higher discounts, support priority, and
 * access to exclusive promotions.
 *
 * <table>
 * <tr>
 * <th>Tier</th>
 * <th>Min. orders</th>
 * <th>Max discount</th>
 * <th>Support SLA</th>
 * </tr>
 * <tr>
 * <td>BRONZE</td>
 * <td>0</td>
 * <td>5 %</td>
 * <td>72 h</td>
 * </tr>
 * <tr>
 * <td>SILVER</td>
 * <td>5</td>
 * <td>10 %</td>
 * <td>48 h</td>
 * </tr>
 * <tr>
 * <td>GOLD</td>
 * <td>15</td>
 * <td>20 %</td>
 * <td>24 h</td>
 * </tr>
 * <tr>
 * <td>PLATINUM</td>
 * <td>50</td>
 * <td>30 %</td>
 * <td>4 h</td>
 * </tr>
 * </table>
 */
public enum CustomerTier {

    BRONZE(0, 5, 72, "ROLE_TIER_BRONZE"),
    SILVER(5, 10, 48, "ROLE_TIER_SILVER"),
    GOLD(15, 20, 24, "ROLE_TIER_GOLD"),
    PLATINUM(50, 30, 4, "ROLE_TIER_PLATINUM");

    /** Minimum lifetime order count to qualify for this tier. */
    private final int minOrders;

    /** Maximum discount percentage applicable for this tier. */
    private final int maxDiscountPct;

    /** Support response SLA in hours. */
    private final int supportSlaHours;

    /** Corresponding Spring Security role name ({@code ROLE_TIER_*}). */
    private final String roleName;

    CustomerTier(int minOrders, int maxDiscountPct, int supportSlaHours, String roleName) {
        this.minOrders = minOrders;
        this.maxDiscountPct = maxDiscountPct;
        this.supportSlaHours = supportSlaHours;
        this.roleName = roleName;
    }

    public int getMinOrders() {
        return minOrders;
    }

    public int getMaxDiscountPct() {
        return maxDiscountPct;
    }

    public int getSupportSlaHours() {
        return supportSlaHours;
    }

    public String getRoleName() {
        return roleName;
    }

    /**
     * Returns the {@link CustomerTier} whose {@code roleName} matches the supplied
     * Spring Security authority string, or {@code null} when not found.
     */
    public static CustomerTier fromRoleName(String roleName) {
        if (roleName == null)
            return null;
        for (CustomerTier t : values()) {
            if (t.roleName.equals(roleName))
                return t;
        }
        return null;
    }

    /**
     * Derives the appropriate {@link CustomerTier} from a lifetime order count.
     */
    public static CustomerTier fromOrderCount(int orderCount) {
        if (orderCount >= PLATINUM.minOrders)
            return PLATINUM;
        if (orderCount >= GOLD.minOrders)
            return GOLD;
        if (orderCount >= SILVER.minOrders)
            return SILVER;
        return BRONZE;
    }
}
