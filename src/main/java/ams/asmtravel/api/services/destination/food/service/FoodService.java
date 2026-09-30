package ams.asmtravel.api.services.destination.food.service;

import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.dto.request.FoodRequest;
import ams.asmtravel.api.services.destination.food.dto.response.FoodResponse;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Places to eat near a destination. */
public interface FoodService {

    /** Published ones (all statuses for the back office), optionally by destination or slug. */
    List<FoodResponse> getAll(UUID destinationId, String slug);

    /** Published listings near a destination's pin, nearest first; empty when it has no pin. */
    List<FoodResponse> getNear(UUID destinationId);

    FoodResponse findById(UUID id);

    FoodResponse create(FoodRequest request, UUID authorId);

    FoodResponse update(UUID id, FoodRequest request);

    void delete(UUID id);


    /* For other services. */

    boolean isPublished(UUID id);

    /** Publishes on approval; the publishing service checks the authority. */
    FoodResponse publish(UUID id);

    /** Takes it off the site; it stays as a draft. */
    FoodResponse unpublish(UUID id);

    /** Rating from visible reviews; {@code average} is null when there are none. */
    void updateRating(UUID id, BigDecimal average, int count);

    long count();

    /** @throws org.springframework.web.server.ResponseStatusException 404 when there is none */
    Food getEntity(UUID id);

    boolean exists(UUID id);

    /** Whether the caller may write this listing and its gallery; false if it does not exist. */
    boolean canWrite(UUID id, Authentication authentication);

    /** Null clears the cover. */
    void setCover(UUID id, String coverKey);

    /** The cheapest dish sets the "from" price. */
    void setPriceFrom(UUID id, BigDecimal priceFrom);
}
