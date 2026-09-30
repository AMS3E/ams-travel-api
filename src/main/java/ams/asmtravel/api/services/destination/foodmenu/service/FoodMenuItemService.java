package ams.asmtravel.api.services.destination.foodmenu.service;

import ams.asmtravel.api.services.destination.foodmenu.dto.request.FoodMenuItemRequest;
import ams.asmtravel.api.services.destination.foodmenu.dto.response.FoodMenuItemResponse;

import java.util.List;
import java.util.UUID;

/** A food listing's menu. Listing writers write it; the cheapest dish sets the "from" price. */
public interface FoodMenuItemService {

    /** In order. The public sees active dishes of published listings; the back office sees all. */
    List<FoodMenuItemResponse> getAll(UUID foodId);

    FoodMenuItemResponse findById(UUID id);

    FoodMenuItemResponse create(FoodMenuItemRequest request);

    FoodMenuItemResponse update(UUID id, FoodMenuItemRequest request);

    void delete(UUID id);
}
