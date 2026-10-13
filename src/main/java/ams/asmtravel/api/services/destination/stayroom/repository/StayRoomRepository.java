package ams.asmtravel.api.services.destination.stayroom.repository;

import ams.asmtravel.api.services.destination.stayroom.domain.StayRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StayRoomRepository extends JpaRepository<StayRoom, UUID> {

    List<StayRoom> findByStayIdOrderBySortOrderAscCreatedAtAsc(UUID stayId);

    List<StayRoom> findAllByOrderBySortOrderAscCreatedAtAsc();

    /** How many rooms list an amenity slug; the needle is the quoted slug between wildcards. */
    @Query(value = "SELECT count(*) FROM destination.destination_stay_rooms WHERE CAST(amenities AS varchar) LIKE :needle", nativeQuery = true)
    long countByAmenity(@Param("needle") String needle);
}
