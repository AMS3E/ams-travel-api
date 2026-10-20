package ams.asmtravel.api.services.destination.stay.service;

import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.destination.stay.dto.request.StayRequest;
import org.springframework.security.core.Authentication;
import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Places to stay, found by their map pin near destinations. */
public interface StayService {

    /** Published ones (all statuses for the back office), optionally by destination. */
    List<StayResponse> getAll(UUID destinationId);

    /** Published stays near a destination's pin, nearest first; empty when it has no pin. */
    List<StayResponse> getNear(UUID destinationId);

    StayResponse findById(UUID id);

    StayResponse create(StayRequest request);

    StayResponse update(UUID id, StayRequest request);

    void delete(UUID id);

    /* For other services. */

    boolean isPublished(UUID id);

    /** Publishes on approval; the publishing service checks the authority. */
    StayResponse publish(UUID id);

    /** Refused with 409 while any filled English field has no Khmer: for a reviewer about to approve it. */
    void requireKhmer(UUID id);

    /** Takes it off the site; it stays as a draft. */
    StayResponse unpublish(UUID id);

    long count();

    /** @throws ams.asmtravel.api.services.destination.stay.exception.StayNotFoundException when there is none */
    Stay getEntity(UUID id);

    boolean exists(UUID id);

    /** Whether the caller may write this stay and its gallery; false if it does not exist. */
    boolean canWrite(UUID id, Authentication authentication);

    /** Null clears the cover. */
    void setCover(UUID id, String coverKey);

    /** The cheapest active room sets the "from" price. */
    void setPriceFrom(UUID id, BigDecimal priceFrom);
}
