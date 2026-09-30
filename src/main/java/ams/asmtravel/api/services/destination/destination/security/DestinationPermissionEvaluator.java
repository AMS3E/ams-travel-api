package ams.asmtravel.api.services.destination.destination.security;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_DELETE;
import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_DRAFT_DELETE_OWN;
import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_DRAFT_EDIT_OWN;
import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_EDIT_ANY;
import static ams.asmtravel.api.services.authorization.domain.Permissions.FULL_CONTROL;
import static ams.asmtravel.api.services.authorization.domain.Permissions.LISTING_DELETE_OWN;
import static ams.asmtravel.api.services.authorization.domain.Permissions.LISTING_EDIT_OWN;
import static ams.asmtravel.api.services.authorization.domain.Permissions.PARTNER_LISTING_DELETE;
import static ams.asmtravel.api.services.authorization.domain.Permissions.PARTNER_LISTING_EDIT;
import static ams.asmtravel.api.services.authorization.domain.Permissions.PARTNER_PUBLISH_APPROVE;

/**
 * Which destinations each desk reaches: editors all; a BDO partners' unpublished ones plus their own;
 * a partner their own; a contributor their own drafts.
 */
@Component("destinationPermission")
@RequiredArgsConstructor
public class DestinationPermissionEvaluator {

    private final DestinationRepository destinationRepository;
    private final UserService userService;

    public boolean canReadAll(Authentication authentication) {
        return has(authentication, FULL_CONTROL) || has(authentication, DESTINATION_EDIT_ANY);
    }

    /** A BDO's desk: what partners write. */
    public boolean canReadPartnerListings(Authentication authentication) {
        return has(authentication, PARTNER_PUBLISH_APPROVE);
    }

    /** Whether the caller works on rows they wrote themselves. */
    public boolean canReadOwn(Authentication authentication) {
        return has(authentication, LISTING_EDIT_OWN) || has(authentication, DESTINATION_DRAFT_EDIT_OWN);
    }

    public boolean canRead(Destination destination, Authentication authentication) {
        if (canReadAll(authentication)) {
            return true;
        }
        if (canReadPartnerListings(authentication) && writtenByPartner(destination)) {
            return true;
        }
        return canReadOwn(authentication) && isOwner(destination, authentication);
    }

    public boolean canEdit(UUID destinationId, Authentication authentication) {
        return destinationRepository.findById(destinationId)
                .map(destination -> canEdit(destination, authentication))
                .orElse(canReadAll(authentication));
    }

    public boolean canEdit(Destination destination, Authentication authentication) {
        if (canReadAll(authentication)) {
            return true;
        }
        if (has(authentication, PARTNER_LISTING_EDIT) && !destination.isPublished() && writtenByPartner(destination)) {
            return true;
        }
        if (!isOwner(destination, authentication)) {
            return false;
        }
        // Draft writers cannot touch a published place, whatever else they hold.
        if (has(authentication, DESTINATION_DRAFT_EDIT_OWN)) {
            return !destination.isPublished();
        }
        return has(authentication, LISTING_EDIT_OWN);
    }

    public boolean canDelete(UUID destinationId, Authentication authentication) {
        if (has(authentication, FULL_CONTROL) || has(authentication, DESTINATION_DELETE)) {
            return true;
        }
        Destination destination = destinationRepository.findById(destinationId).orElse(null);
        if (destination == null || destination.isPublished()) {
            return false;
        }
        if (has(authentication, PARTNER_LISTING_DELETE) && writtenByPartner(destination)) {
            return true;
        }
        return (has(authentication, LISTING_DELETE_OWN) || has(authentication, DESTINATION_DRAFT_DELETE_OWN))
                && isOwner(destination, authentication);
    }

    /** Only the editorial desk may feature. */
    public boolean canFeature(Authentication authentication) {
        return canReadAll(authentication);
    }

    private boolean writtenByPartner(Destination destination) {
        return destination.getCreatedBy() != null && userService.findSummary(destination.getCreatedBy())
                .map(author -> author.roles().contains(RoleCode.PARTNER))
                .orElse(false);
    }

    private boolean isOwner(Destination destination, Authentication authentication) {
        Optional<UUID> caller = userService.findUserId(authentication);
        return caller.isPresent() && Objects.equals(destination.getCreatedBy(), caller.get());
    }

    static boolean has(Authentication authentication, String authority) {
        return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
