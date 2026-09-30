package ams.asmtravel.api.services.destination.foodmenu.repository;

import ams.asmtravel.api.services.destination.foodmenu.domain.FoodMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FoodMenuItemRepository extends JpaRepository<FoodMenuItem, UUID> {

    List<FoodMenuItem> findByFoodIdOrderBySortOrderAscCreatedAtAsc(UUID foodId);

    List<FoodMenuItem> findAllByOrderBySortOrderAscCreatedAtAsc();
}
