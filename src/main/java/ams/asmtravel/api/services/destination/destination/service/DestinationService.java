package ams.asmtravel.api.services.destination.destination.service;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.domain.NearbyDestination;
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

    /** Published places matching the search, a page at a time in name order: the app's search and filtered lists. */
    Page<DestinationResponse> searchPublished(DestinationSearch search, Pageable pageable);

    /** The published places among these ids, in no particular order; unknown and unpublished ids are left out. */
    List<DestinationResponse> findPublished(Collection<UUID> ids);

    /** Up to {@code limit} published places whose English or Khmer name contains the text, A to Z. */
    List<DestinationResponse> suggestPublished(String text, int limit);

    /** Published for anyone; unpublished only for desks that reach it; 404 otherwise. */
    DestinationResponse findById(UUID id);

    /** Published places within {@code radiusKm}, nearest first. */
    List<DestinationResponse> findNearby(double latitude, double longitude, double radiusKm, int limit);

    /** {@link #findNearby} with how far each place is from the point, in metres. */
    List<NearbyDestination> findNearbyWithDistance(double latitude, double longitude, double radiusKm, int limit);

    /** Creates a DRAFT. */
    DestinationResponse create(DestinationRequest request, UUID authorId);

    DestinationResponse update(UUID id, DestinationRequest request);

    /** Deletes it with its listings, gallery and stops; see {@code DestinationDeletingEvent}. */
    void delete(UUID id);

    /** Refused with 409 while any filled English field has no Khmer. */
    DestinationResponse publish(UUID id);

    /** The same 409 as {@link #publish}, for a reviewer about to approve it. */
    void requireKhmer(UUID id);

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
