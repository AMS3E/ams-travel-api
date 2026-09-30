package ams.asmtravel.api.services.authorization.role.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Roles a person can hold, lowest first. Also the whitelist: no other realm role
 * may be handed out through this API.
 */
public enum RoleCode {
    USER,
    PARTNER,
    CONTRIBUTOR,
    EDITOR,
    BDO,
    ADMIN;

    /** True for staff-realm roles. */
    public boolean isBackoffice() {
        return this != USER;
    }

    /** Parses a role name; empty if unknown. */
    public static Optional<RoleCode> parse(String value) {
        return Arrays.stream(values()).filter(code -> code.name().equals(value)).findFirst();
    }
}
