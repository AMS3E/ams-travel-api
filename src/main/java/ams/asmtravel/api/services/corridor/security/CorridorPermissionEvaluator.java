package ams.asmtravel.api.services.corridor.security;

import ams.asmtravel.api.services.authorization.domain.Permissions;
import ams.asmtravel.api.services.corridor.domain.Corridor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/** Row-level read check: drafts are visible only to corridor curators. */
@Component
public class CorridorPermissionEvaluator {

    public boolean canCurate(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> Permissions.CORRIDOR_CURATE.equals(authority) || Permissions.FULL_CONTROL.equals(authority));
    }

    public boolean canSee(Corridor corridor, Authentication authentication) {
        return corridor.isPublished() || canCurate(authentication);
    }
}
