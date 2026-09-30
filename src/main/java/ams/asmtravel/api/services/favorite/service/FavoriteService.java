package ams.asmtravel.api.services.favorite.service;

import ams.asmtravel.api.services.favorite.domain.FavoriteTarget;
import ams.asmtravel.api.services.favorite.dto.request.FavoriteRequest;
import ams.asmtravel.api.services.favorite.dto.response.FavoriteResponse;

import java.util.List;
import java.util.UUID;

/** What visitors saved. */
public interface FavoriteService {

    /** Newest first. */
    List<FavoriteResponse> findMine(UUID userId);

    /** @throws org.springframework.web.server.ResponseStatusException 409 when it is saved already */
    FavoriteResponse add(UUID userId, FavoriteRequest request);

    /** @throws org.springframework.web.server.ResponseStatusException 404 when it was not saved */
    void remove(UUID userId, FavoriteTarget targetType, UUID targetId);

    long count();
}
