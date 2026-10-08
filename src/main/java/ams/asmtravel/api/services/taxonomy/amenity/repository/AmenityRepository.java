package ams.asmtravel.api.services.taxonomy.amenity.repository;

import ams.asmtravel.api.services.taxonomy.amenity.domain.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {

    /** Group by group, each in its own order. */
    List<Amenity> findAllByOrderByGroupNameAscSortOrderAscNameAsc();

    List<Amenity> findBySlugIn(Collection<String> slugs);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    /** The last place taken in a group, or -1 for an empty one. */
    @Query("select coalesce(max(a.sortOrder), -1) from Amenity a where a.groupName = :groupName")
    int lastSortOrder(@Param("groupName") String groupName);
}
