package ams.asmtravel.api.services.favorite.repository;

import ams.asmtravel.api.services.favorite.domain.Favorite;
import ams.asmtravel.api.services.favorite.domain.FavoriteId;
import ams.asmtravel.api.services.favorite.domain.FavoriteTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    @Query("select f from Favorite f where f.id.userId = :userId order by f.createdAt desc")
    List<Favorite> findByUserId(UUID userId);

    @Modifying
    @Query("delete from Favorite f where f.id.userId = :userId")
    int deleteByUserId(UUID userId);

    @Modifying
    @Query("delete from Favorite f where f.id.targetType = :targetType and f.id.targetId = :targetId")
    int deleteByTarget(FavoriteTarget targetType, UUID targetId);
}
