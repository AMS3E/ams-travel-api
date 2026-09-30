package ams.asmtravel.api.services.user.user.domain;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import java.util.Set;

/**
 * Who a verified Keycloak token says is signed in.
 *
 * @param provider   the realm that issued the token; subjects are unique per realm only
 * @param subject    the account's id in that realm
 * @param email      trimmed and lower-cased; null when missing
 * @param name       display name, else username; may be null
 * @param roles      the roles the token grants, among {@link RoleCode}
 * @param backoffice whether the staff realm issued it
 */
public record TokenIdentity(
        String provider,
        String subject,
        String email,
        String name,
        Set<RoleCode> roles,
        boolean backoffice
) {

    public TokenIdentity {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    /** Enough to find or make the person: a subject and an email. */
    public boolean isComplete() {
        return subject != null && !subject.isBlank() && email != null;
    }
}
