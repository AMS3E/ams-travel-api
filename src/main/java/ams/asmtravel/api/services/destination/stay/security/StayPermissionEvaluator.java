package ams.asmtravel.api.services.destination.stay.security;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_DELETE;
import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_DRAFT_EDIT_OWN;
import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_EDIT_ANY;
import static ams.asmtravel.api.services.authorization.domain.Permissions.FULL_CONTROL;
import static ams.asmtravel.api.services.authorization.domain.Permissions.LISTING_DELETE_OWN;
import static ams.asmtravel.api.services.authorization.domain.Permissions.LISTING_EDIT_OWN;

/**
 * Who reaches which stays. Editors reach all and alone publish;
 * owners ({@code LISTING_EDIT_OWN}) write their own and delete them while unpublished.
 */
@Component
@RequiredArgsConstructor
public class StayPermissionEvaluator {

    private final UserService userService;

    public boolean isEditor(Authentication authentication) {
        return Authorities.hasAny(authentication, FULL_CONTROL, DESTINATION_EDIT_ANY);
    }

    /** Only an editor may write a stay with no owner. */
    public boolean canWrite(UUID ownerUserId, Authentication authentication) {
        return isEditor(authentication)
                || (ownerUserId != null && Authorities.has(authentication, LISTING_EDIT_OWN)
                        && isOwner(ownerUserId, authentication));
    }

    /** A contributor cannot change a published stay; a partner still can. */
    public boolean canChange(ListingStatus status, Authentication authentication) {
        return isEditor(authentication) || status != ListingStatus.PUBLISHED
                || !Authorities.has(authentication, DESTINATION_DRAFT_EDIT_OWN);
    }

    public boolean canDelete(UUID ownerUserId, ListingStatus status, Authentication authentication) {
        if (isEditor(authentication) || Authorities.has(authentication, DESTINATION_DELETE)) {
            return true;
        }
        return ownerUserId != null && status != ListingStatus.PUBLISHED
                && Authorities.has(authentication, LISTING_DELETE_OWN) && isOwner(ownerUserId, authentication);
    }

    private boolean isOwner(UUID ownerUserId, Authentication authentication) {
        return userService.findUserId(authentication).map(ownerUserId::equals).orElse(false);
    }
}
