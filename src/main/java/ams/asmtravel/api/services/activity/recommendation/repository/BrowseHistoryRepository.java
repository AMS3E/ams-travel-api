package ams.asmtravel.api.services.activity.recommendation.repository;

import ams.asmtravel.api.services.activity.popular.domain.BrowseTotal;
import ams.asmtravel.api.services.activity.popular.service.PopularService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Browsing data read through the popular service; recommendations store no rows of their own. */
@Repository
@RequiredArgsConstructor
public class BrowseHistoryRepository {

    private final PopularService popularService;

    /** Different signed-in visitors per place lately, most browsed first. */
    public Map<UUID, Long> recentBrowses() {
        Map<UUID, Long> browses = new LinkedHashMap<>();
        for (BrowseTotal total : popularService.recentTotals()) {
            browses.put(total.targetId(), total.browses());
        }
        return browses;
    }

    public Set<UUID> browsedBy(UUID userId) {
        return popularService.browsedDestinationIds(userId);
    }
}
