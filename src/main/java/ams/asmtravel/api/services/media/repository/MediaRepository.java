package ams.asmtravel.api.services.media.repository;

import ams.asmtravel.api.services.media.domain.Media;
import ams.asmtravel.api.services.media.domain.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    List<Media> findByOwnerTypeOrderBySortOrderAscCreatedAtAsc(MediaType ownerType);

    List<Media> findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(MediaType ownerType, UUID ownerId);

    List<Media> findByOwnerTypeAndOwnerIdInOrderBySortOrderAscCreatedAtAsc(MediaType ownerType, Collection<UUID> ownerIds);

    @Query("select coalesce(max(m.sortOrder), 0) from Media m where m.ownerType = :ownerType and m.ownerId = :ownerId")
    int findMaxSortOrder(@Param("ownerType") MediaType ownerType, @Param("ownerId") UUID ownerId);

    @Modifying
    @Query("delete from Media m where m.ownerType = :ownerType and m.ownerId in :ownerIds")
    void deleteByOwners(@Param("ownerType") MediaType ownerType, @Param("ownerIds") Collection<UUID> ownerIds);
}
