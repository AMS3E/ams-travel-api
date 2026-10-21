package ams.asmtravel.api.services.favorite.service.impl;

import ams.asmtravel.api.services.corridor.domain.CorridorDeletingEvent;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.location.province.domain.ProvinceDeletingEvent;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.location.region.domain.RegionDeletingEvent;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.favorite.domain.Favorite;
import ams.asmtravel.api.services.favorite.domain.FavoriteId;
import ams.asmtravel.api.services.favorite.domain.FavoriteTarget;
import ams.asmtravel.api.services.favorite.dto.request.FavoriteRequest;
import ams.asmtravel.api.services.favorite.dto.response.FavoriteResponse;
import ams.asmtravel.api.services.favorite.exception.FavoriteNotFoundException;
import ams.asmtravel.api.services.favorite.mapper.FavoriteMapper;
import ams.asmtravel.api.services.favorite.repository.FavoriteRepository;
import ams.asmtravel.api.services.favorite.service.FavoriteService;
import ams.asmtravel.api.services.destination.activity.domain.ActivityDeletingEvent;
import ams.asmtravel.api.services.destination.activity.service.ActivityService;
import ams.asmtravel.api.services.destination.food.domain.FoodDeletingEvent;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.domain.StayDeletingEvent;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final ActivityService activityService;
    private final CorridorService corridorService;
    private final RegionService regionService;
    private final ProvinceService provinceService;
    private final FavoriteMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteResponse> findMine(UUID userId) {
        return favoriteRepository.findByUserId(userId).stream().map(mapper::toFavoriteResponse).toList();
    }

    /** Only something a visitor can see can be saved: a published place, or a corridor. */
    @Override
    @Transactional
    public FavoriteResponse add(UUID userId, FavoriteRequest request) {
        if (!exists(request.targetType(), request.targetId())) {
            throw new FavoriteNotFoundException("Nothing to save there");
        }
        FavoriteId id = new FavoriteId(userId, request.targetType(), request.targetId());
        if (favoriteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already saved");
        }
        return mapper.toFavoriteResponse(favoriteRepository.saveAndFlush(
                new Favorite(userId, request.targetType(), request.targetId())));
    }

    @Override
    @Transactional
    public void remove(UUID userId, FavoriteTarget targetType, UUID targetId) {
        FavoriteId id = new FavoriteId(userId, targetType, targetId);
        if (!favoriteRepository.existsById(id)) {
            throw new FavoriteNotFoundException();
        }
        favoriteRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return favoriteRepository.count();
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        favoriteRepository.deleteByUserId(event.userId());
    }

    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        favoriteRepository.deleteByTarget(FavoriteTarget.DESTINATION, event.destinationId());
    }

    @EventListener
    public void onStayDeleting(StayDeletingEvent event) {
        favoriteRepository.deleteByTarget(FavoriteTarget.STAY, event.stayId());
    }

    @EventListener
    public void onFoodDeleting(FoodDeletingEvent event) {
        favoriteRepository.deleteByTarget(FavoriteTarget.FOOD, event.foodId());
    }

    @EventListener
    public void onActivityDeleting(ActivityDeletingEvent event) {
        favoriteRepository.deleteByTarget(FavoriteTarget.ACTIVITY, event.activityId());
    }

    @EventListener
    public void onCorridorDeleting(CorridorDeletingEvent event) {
        favoriteRepository.deleteByTarget(FavoriteTarget.CORRIDOR, event.corridorId());
    }

    @EventListener
    public void onRegionDeleting(RegionDeletingEvent event) {
        favoriteRepository.deleteByTarget(FavoriteTarget.REGION, event.regionId());
    }

    @EventListener
    public void onProvinceDeleting(ProvinceDeletingEvent event) {
        favoriteRepository.deleteByTarget(FavoriteTarget.PROVINCE, event.provinceId());
    }

    private boolean exists(FavoriteTarget targetType, UUID targetId) {
        return switch (targetType) {
            case DESTINATION -> destinationService.isPublished(targetId);
            case STAY -> stayService.isPublished(targetId);
            case FOOD -> foodService.isPublished(targetId);
            case ACTIVITY -> activityService.isPublished(targetId);
            case CORRIDOR -> corridorService.isPublished(targetId);
            case REGION -> regionService.exists(targetId);
            case PROVINCE -> provinceService.exists(targetId);
        };
    }
}
