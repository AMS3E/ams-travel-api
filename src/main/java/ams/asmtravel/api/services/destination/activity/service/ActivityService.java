package ams.asmtravel.api.services.destination.activity.service;

import ams.asmtravel.api.services.destination.activity.domain.Activity;
import ams.asmtravel.api.services.destination.activity.dto.request.ActivityRequest;
import ams.asmtravel.api.services.destination.activity.dto.response.ActivityResponse;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

/** Activities and experiences: the fourth kind of destination. */
public interface ActivityService {

    /** Published ones (all statuses for the back office), optionally by the attraction they run at, or by slug. */
    List<ActivityResponse> getAll(UUID destinationId, String slug);

    /** Published activities near a destination's pin, nearest first; empty when it has no pin. */
    List<ActivityResponse> getNear(UUID destinationId);

    ActivityResponse findById(UUID id);

    ActivityResponse create(ActivityRequest request, UUID authorId);

    ActivityResponse update(UUID id, ActivityRequest request);

    void delete(UUID id);


    /* For other services. */

    boolean isPublished(UUID id);

    /** Publishes on approval; the publishing service checks the authority. */
    ActivityResponse publish(UUID id);

    /** Takes it off the site; it stays as a draft. */
    ActivityResponse unpublish(UUID id);

    long count();

    /** @throws org.springframework.web.server.ResponseStatusException 404 when there is none */
    Activity getEntity(UUID id);

    boolean exists(UUID id);

    /** Counts one more view: a page's first open of the day, as the popular service decides. */
    void recordView(UUID id);

    /** Whether the caller may write this activity and its gallery; false if it does not exist. */
    boolean canWrite(UUID id, Authentication authentication);

    /** Null clears the cover. */
    void setCover(UUID id, String coverKey);
}
