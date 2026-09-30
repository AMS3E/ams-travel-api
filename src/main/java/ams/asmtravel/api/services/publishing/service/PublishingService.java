package ams.asmtravel.api.services.publishing.service;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.publishing.domain.PublishingStatus;
import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
import ams.asmtravel.api.services.publishing.dto.request.ApprovePublishRequest;
import ams.asmtravel.api.services.publishing.dto.request.RejectPublishRequest;
import ams.asmtravel.api.services.publishing.dto.request.SubmitPublishRequest;
import ams.asmtravel.api.services.publishing.dto.response.PublishingHistoryResponse;
import ams.asmtravel.api.services.publishing.dto.response.PublishingResponse;
import org.springframework.security.core.Authentication;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ams.asmtravel.api.services.authorization.domain.Permissions.CONTRIBUTOR_PUBLISH_APPROVE;
import static ams.asmtravel.api.services.authorization.domain.Permissions.FULL_CONTROL;
import static ams.asmtravel.api.services.authorization.domain.Permissions.PARTNER_PUBLISH_APPROVE;

/** Putting listings forward and answering them: Partner → BDO, Contributor → Editor, Admin has full control. */
public interface PublishingService {

    /** Who answers a request, from its requester's roles. */
    enum Desk {
        /** Partner listings; a BDO's desk. */
        PARTNER,
        /** Contributor drafts; an editor's desk. */
        CONTRIBUTOR,
        /** Editors', BDOs' and administrators' own content; an administrator's desk. */
        STAFF;

        private static final List<RoleCode> PAGE_ORDER =
                List.of(RoleCode.ADMIN, RoleCode.EDITOR, RoleCode.BDO, RoleCode.CONTRIBUTOR, RoleCode.PARTNER);

        /** The requester's desk, highest role first. */
        public static Desk of(Set<RoleCode> requesterRoles) {
            if (requesterRoles.contains(RoleCode.ADMIN) || requesterRoles.contains(RoleCode.EDITOR)
                    || requesterRoles.contains(RoleCode.BDO)) {
                return STAFF;
            }
            if (requesterRoles.contains(RoleCode.CONTRIBUTOR)) {
                return CONTRIBUTOR;
            }
            if (requesterRoles.contains(RoleCode.PARTNER)) {
                return PARTNER;
            }
            return STAFF;
        }

        /** The requester's highest back-office role (which desk page lists it), or null. */
        public static RoleCode roleOf(Set<RoleCode> requesterRoles) {
            return PAGE_ORDER.stream().filter(requesterRoles::contains).findFirst().orElse(null);
        }

        /** The desks this reviewer answers for. */
        public static Set<Desk> reviewedBy(Authentication authentication) {
            if (Authorities.has(authentication, FULL_CONTROL)) {
                return EnumSet.allOf(Desk.class);
            }
            Set<Desk> desks = EnumSet.noneOf(Desk.class);
            if (Authorities.has(authentication, PARTNER_PUBLISH_APPROVE)) {
                desks.add(PARTNER);
            }
            if (Authorities.has(authentication, CONTRIBUTOR_PUBLISH_APPROVE)) {
                desks.add(CONTRIBUTOR);
            }
            return desks;
        }
    }

    /** Published on every answer; notifications tell the requester. */
    record PublishingDecidedEvent(UUID requestId, PublishingTargetType targetType, UUID targetId, String targetName,
                                  UUID requesterId, PublishingStatus status, String note) {
    }

    /** Only the listing's author (or full control), only while it is not live and has no request under way. */
    PublishingResponse submit(SubmitPublishRequest request, UUID requesterId);

    /** PENDING_REVIEW → APPROVED: the request's desk, or an editor or BDO for their own listing. */
    PublishingResponse approve(UUID id, ApprovePublishRequest request, UUID reviewerId);

    /** PENDING_REVIEW → REJECTED, or back to DRAFT for its author to fix; a reason is required. */
    PublishingResponse reject(UUID id, RejectPublishRequest request, UUID reviewerId);

    /** APPROVED → PUBLISHED: the listing goes live. Same people as approving. */
    PublishingResponse publish(UUID id, UUID reviewerId);

    /** PUBLISHED → UNPUBLISHED: the listing comes off the site. Needs the unpublish grant. */
    PublishingResponse unpublish(UUID id, UUID reviewerId);

    /** A request the caller made or may answer. */
    PublishingResponse findById(UUID id, UUID callerId);

    /** A listing's latest request; 404 when it was never put forward. */
    PublishingResponse findCurrent(PublishingTargetType targetType, UUID targetId, UUID callerId);

    /** A listing's requests, newest first; for its requesters and the desk that answers them. */
    List<PublishingHistoryResponse> findHistory(PublishingTargetType targetType, UUID targetId, UUID callerId);

    /** The caller's desks' requests, newest first; null status lists every one. */
    List<PublishingResponse> findAll(PublishingStatus status);

    /** The caller's desks' requests waiting for a decision or to be published. */
    List<PublishingResponse> findPending();

    /** What a person put forward, newest first; the caller's own, or anyone's for full control. */
    List<PublishingResponse> findByCreator(UUID creatorId, UUID callerId);
}
