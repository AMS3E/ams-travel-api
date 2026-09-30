package ams.asmtravel.api.services.verification.security;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.publishing.service.PublishingService.Desk;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_VERIFY;
import static ams.asmtravel.api.services.authorization.domain.Permissions.FULL_CONTROL;

/** Who may verify: BDO and admin only, for places of every desk. */
@Component
public class VerifierDesks {

    /** Whether this account verifies at all. */
    public boolean verifies(Authentication authentication) {
        return Authorities.has(authentication, FULL_CONTROL) || Authorities.has(authentication, DESTINATION_VERIFY);
    }

    /** Desks this account verifies: all or none. */
    public Set<Desk> desksOf(Authentication authentication) {
        return verifies(authentication) ? EnumSet.allOf(Desk.class) : Set.of();
    }

    /** A place's desk from its author's roles; staff if there is no author. */
    public Desk deskOf(Set<RoleCode> authorRoles) {
        return Desk.of(authorRoles == null ? Set.of() : authorRoles);
    }
}
