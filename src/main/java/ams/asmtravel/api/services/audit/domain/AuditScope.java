package ams.asmtravel.api.services.audit.domain;

import ams.asmtravel.api.services.user.user.domain.AuthIdentity;
import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.preference.domain.UserPreferences;
import ams.asmtravel.api.services.badge.domain.UserBadge;
import ams.asmtravel.api.services.point.domain.PointActivity;
import ams.asmtravel.api.services.point.domain.PointTransaction;
import ams.asmtravel.api.services.season.domain.UserSeasonProgress;
import ams.asmtravel.api.services.activity.stamp.domain.DigitalStamp;
import ams.asmtravel.api.services.favorite.domain.Favorite;
import ams.asmtravel.api.services.notification.domain.Notification;

import java.util.Set;
import java.util.UUID;

/**
 * Which writes are logged: rows with their own UUID, so new tables are logged by default.
 * Link rows, the log itself and people's own activity are not logged.
 */
public final class AuditScope {

    private static final Set<Class<?>> PERSONAL = Set.of(
            User.class,
            AuthIdentity.class,
            UserPreferences.class,
            Favorite.class,
            Notification.class,
            DigitalStamp.class,
            UserBadge.class,
            UserSeasonProgress.class,
            PointTransaction.class,
            PointActivity.class);

    private AuditScope() {
    }

    public static boolean isAudited(Class<?> entityType, Object id) {
        return entityType != null && id instanceof UUID && !PERSONAL.contains(entityType);
    }
}
