package com.hiip.datastorage.security;

/**
 * Enum representing user roles in the application.
 * Spring Security requires roles to be prefixed with "ROLE_" internally.
 */
public enum UserRole {
    /**
     * Administrator role with full access to all endpoints
     */
    ADMIN("ROLE_ADMIN", "ADMIN"),
    
    /**
     * Regular user role with access to standard endpoints
     */
    USER("ROLE_USER", "USER");

    private final String authority;
    private final String roleName;

    UserRole(String authority, String roleName) {
        this.authority = authority;
        this.roleName = roleName;
    }

    /**
     * Get the full authority string with ROLE_ prefix (used in authorities list)
     * @return The authority string (e.g., "ROLE_ADMIN")
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * Get the role name without prefix (used in hasRole() checks)
     * @return The role name (e.g., "ADMIN")
     */
    public String getRoleName() {
        return roleName;
    }

    @Override
    public String toString() {
        return authority;
    }
}
