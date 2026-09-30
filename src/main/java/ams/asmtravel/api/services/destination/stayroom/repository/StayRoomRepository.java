package ams.asmtravel.api.services.destination.stayroom.repository;

import ams.asmtravel.api.services.destination.stayroom.domain.StayRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StayRoomRepository extends JpaRepository<StayRoom, UUID> {

    List<StayRoom> findByStayIdOrderBySortOrderAscCreatedAtAsc(UUID stayId);

    List<StayRoom> findAllByOrderBySortOrderAscCreatedAtAsc();
}
