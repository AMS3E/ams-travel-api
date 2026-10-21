package ams.asmtravel.api.services.badge.service;

import ams.asmtravel.api.services.badge.domain.BadgeStatus;
import ams.asmtravel.api.services.badge.domain.BadgeType;
import ams.asmtravel.api.services.badge.dto.request.CreateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.request.UpdateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.response.BadgeProgressResponse;
import ams.asmtravel.api.services.badge.dto.response.BadgeResponse;
import ams.asmtravel.api.services.badge.dto.response.UserBadgeResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Badge definitions and each person's progress and unlocks per season. */
public interface BadgeService {

    /** Someone just unlocked a badge; the notification service tells them. */
    record BadgeEarnedEvent(UUID userId, UUID userBadgeId, String badgeName, String seasonName) {
    }

    // Definitions

    /** Each filter may be left out. */
    List<BadgeResponse> getAll(BadgeType type, BadgeStatus status);

    BadgeResponse findById(UUID id);

    BadgeResponse create(CreateBadgeRequest request);

    BadgeResponse update(UUID id, UpdateBadgeRequest request);

    /** Only a badge nobody has earned; an earned one is set INACTIVE instead, so history stays. */
    void delete(UUID id);

    long count();

    // A person's badges

    /** Ladders and badges in a season, the active one when {@code seasonId} is null. */
    BadgeProgressResponse findProgress(UUID userId, UUID seasonId);

    /** Every badge a person earned, newest first, across all seasons. */
    List<UserBadgeResponse> findEarned(UUID userId);

    /** One earned badge by its share link. */
    UserBadgeResponse findShared(UUID userBadgeId);

    /** Records a destination of any kind opened (once per place per day) and checks the BROWSES ladder. */
    BadgeProgressResponse explore(UUID userId, UUID destinationId);

    /** Badges a person unlocked since then; the stamp service shows what a stamp just earned. */
    List<UserBadgeResponse> findEarnedSince(UUID userId, Instant since);
}
