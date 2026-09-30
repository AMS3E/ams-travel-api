package ams.asmtravel.api.services.destination.destination.service;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.dto.request.DestinationRequest;
import ams.asmtravel.api.services.destination.destination.dto.response.DestinationResponse;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DestinationService {

    /** Published places by name, or the one a slug names. */
    List<DestinationResponse> getPublished(String slug);

    /** Published for anyone; unpublished only for desks that reach it; 404 otherwise. */
    DestinationResponse findById(UUID id);

    /** Published places within {@code radiusKm}, nearest first. */
    List<DestinationResponse> findNearby(double latitude, double longitude, double radiusKm, int limit);

    /** Creates a DRAFT. */
    DestinationResponse create(DestinationRequest request, UUID authorId);

    DestinationResponse update(UUID id, DestinationRequest request);

    /** Deletes it with its listings, gallery and stops; see {@code DestinationDeletingEvent}. */
    void delete(UUID id);

    DestinationResponse publish(UUID id);

    /** Back to DRAFT, off the public site. */
    DestinationResponse unpublish(UUID id);

    /** Every place the signed-in desk reaches, in any status. */
    List<DestinationResponse> getWorkspace();

    /* For other services. */

    /** The rows among these ids that exist. */
    List<Destination> getEntities(Collection<UUID> ids);

    /** @throws org.springframework.web.server.ResponseStatusException 404 when there is none */
    Destination getEntity(UUID id);

    /** Whether the caller may edit this destination and its gallery. */
    boolean canEdit(UUID id, Authentication authentication);

    /** Anyone when published; its desks otherwise. */
    boolean canRead(UUID id, Authentication authentication);

    /** Null clears the cover. */
    void setCover(UUID id, String coverKey);

    Optional<DestinationSummary> findSummary(UUID id);

    Map<UUID, DestinationSummary> findSummaries(Collection<UUID> ids);

    List<DestinationSummary> findPublishedSummaries();

    /** Every place in any status, by name. */
    List<DestinationSummary> findAllSummaries();

    boolean exists(UUID id);

    boolean isPublished(UUID id);

    /** Counts one visitor today. */
    void recordView(UUID id);

    long count();
}
