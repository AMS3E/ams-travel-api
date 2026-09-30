package ams.asmtravel.api.services.destination.foodmenu.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.domain.FoodSavedEvent;
import ams.asmtravel.api.services.destination.food.domain.FoodVocabularies;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.foodmenu.domain.FoodMenuItem;
import ams.asmtravel.api.services.destination.foodmenu.dto.request.FoodMenuItemRequest;
import ams.asmtravel.api.services.destination.foodmenu.dto.response.FoodMenuItemResponse;
import ams.asmtravel.api.services.destination.foodmenu.exception.FoodMenuItemNotFoundException;
import ams.asmtravel.api.services.destination.foodmenu.mapper.FoodMenuItemMapper;
import ams.asmtravel.api.services.destination.foodmenu.repository.FoodMenuItemRepository;
import ams.asmtravel.api.services.destination.foodmenu.service.FoodMenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodMenuItemServiceImpl implements FoodMenuItemService {

    private final FoodMenuItemRepository menuRepository;
    private final FoodMenuItemMapper mapper;
    private final FoodService foodService;

    @Override
    @Transactional(readOnly = true)
    public List<FoodMenuItemResponse> getAll(UUID foodId) {
        boolean everything = Authorities.isBackoffice(Authorities.current());
        List<FoodMenuItem> items = foodId == null
                ? menuRepository.findAllByOrderBySortOrderAscCreatedAtAsc()
                : menuRepository.findByFoodIdOrderBySortOrderAscCreatedAtAsc(foodId);
        return items.stream().filter(item -> everything || onMenu(item)).map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FoodMenuItemResponse findById(UUID id) {
        FoodMenuItem item = find(id);
        if (!onMenu(item) && !Authorities.isBackoffice(Authorities.current())) {
            throw new FoodMenuItemNotFoundException();
        }
        return mapper.toResponse(item);
    }

    @Override
    @Transactional
    public FoodMenuItemResponse create(FoodMenuItemRequest request) {
        Food food = writable(request.foodId());
        FoodMenuItem item = new FoodMenuItem();
        item.setFood(food);
        item.setSortOrder(menuRepository.findByFoodIdOrderBySortOrderAscCreatedAtAsc(food.getId()).size());
        apply(item, request);
        item = menuRepository.saveAndFlush(item);
        refreshPrice(food.getId());
        return mapper.toResponse(item);
    }

    @Override
    @Transactional
    public FoodMenuItemResponse update(UUID id, FoodMenuItemRequest request) {
        FoodMenuItem item = find(id);
        writable(item.getFood().getId());
        if (!item.getFood().getId().equals(request.foodId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A dish stays on the menu it was made for");
        }
        apply(item, request);
        item = menuRepository.saveAndFlush(item);
        refreshPrice(item.getFood().getId());
        return mapper.toResponse(item);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        FoodMenuItem item = find(id);
        UUID foodId = writable(item.getFood().getId()).getId();
        menuRepository.delete(item);
        menuRepository.flush();
        refreshPrice(foodId);
    }

    /** After a listing edit, the menu's "from" price wins over what the form sent. */
    @EventListener
    public void onFoodSaved(FoodSavedEvent event) {
        refreshPrice(event.foodId());
    }

    private void apply(FoodMenuItem item, FoodMenuItemRequest request) {
        item.setName(request.name().trim());
        item.setLocalName(blankToNull(request.localName()));
        item.setPronunciation(blankToNull(request.pronunciation()));
        item.setCourse(request.course());
        item.setDescription(blankToNull(request.description()));
        item.setPrice(request.price());
        item.setPortion(blankToNull(request.portion()));
        item.setServes(orDefault(request.serves(), 1));
        item.setSpiceLevel(orDefault(request.spiceLevel(), 0));

        /* Fixed list so labels can be filtered. */
        List<String> labels = request.labels() == null ? List.of()
                : request.labels().stream().map(String::trim).filter(one -> !one.isEmpty()).distinct().toList();
        List<String> unknown = FoodVocabularies.unknown(labels, FoodVocabularies.DISH_LABELS);
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such dish label: " + String.join(", ", unknown));
        }
        item.setLabels(new ArrayList<>(labels));

        item.setPhotoKeys(new ArrayList<>(request.photoKeys() == null ? List.of()
                : request.photoKeys().stream().map(String::trim).distinct().toList()));
        item.setIsSignature(Boolean.TRUE.equals(request.isSignature()));
        if (request.sortOrder() != null) {
            item.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            item.setIsActive(request.isActive());
        }
    }

    /** Cheapest dish sets the "from" price; with no priced dish the listing keeps its own. */
    private void refreshPrice(UUID foodId) {
        menuRepository.findByFoodIdOrderBySortOrderAscCreatedAtAsc(foodId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                .map(FoodMenuItem::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .ifPresent(cheapest -> foodService.setPriceFrom(foodId, cheapest));
    }

    private static boolean onMenu(FoodMenuItem item) {
        return Boolean.TRUE.equals(item.getIsActive()) && item.getFood().getStatus() == ListingStatus.PUBLISHED;
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Food writable(UUID foodId) {
        if (!foodService.exists(foodId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such food listing");
        }
        if (!foodService.canWrite(foodId, Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change this listing's menu");
        }
        return foodService.getEntity(foodId);
    }

    private FoodMenuItem find(UUID id) {
        return menuRepository.findById(id).orElseThrow(FoodMenuItemNotFoundException::new);
    }
}
