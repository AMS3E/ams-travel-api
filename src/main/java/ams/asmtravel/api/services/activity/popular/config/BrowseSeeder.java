package ams.asmtravel.api.services.activity.popular.config;

import ams.asmtravel.api.services.activity.popular.domain.BrowseEvent;
import ams.asmtravel.api.services.activity.popular.domain.BrowseTarget;
import ams.asmtravel.api.services.activity.popular.repository.BrowseEventRepository;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.activity.domain.Activity;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.user.user.domain.User;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Seeds browses over the last thirty days, so Popular and the Destination Stats have something to
 * show: six distinct travellers per point of a place's popularity, spread over the days.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BrowseSeeder implements CatalogSeedContributor {

    private static final int VISITORS_PER_POINT = 6;

    private final BrowseEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return ENGAGEMENT;
    }

    @Override
    public boolean alreadySeeded() {
        return eventRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        /* The travellers browse; the seed's editorial desk does not. */
        User desk = context.find(User.class, "ams-desk");
        List<User> travellers = context.all(User.class).stream().filter(user -> user != desk).toList();
        if (travellers.isEmpty()) {
            log.info("No travellers seeded, so no browses");
            return;
        }
        /* The same spread every run, so a staging reset reads the same. */
        Random random = new Random(20261014L);
        Instant now = Instant.now();
        int written = 0;
        int offset = 0;
        for (JsonNode node : SeedJson.rows(objectMapper, BrowseSeeder.class, "browses.json")) {
            BrowseTarget type = SeedJson.enumValue(node, "targetType", BrowseTarget.class);
            UUID targetId = targetOf(context, type, SeedJson.text(node, "target"));
            Integer popularity = SeedJson.integer(node, "popularity");
            if (targetId == null || popularity == null || popularity < 1) {
                continue;
            }
            int visitors = Math.min(popularity * VISITORS_PER_POINT, travellers.size());
            for (int index = 0; index < visitors; index++) {
                User traveller = travellers.get((offset + index) % travellers.size());
                BrowseEvent event = new BrowseEvent();
                event.setUserId(traveller.getId());
                event.setTargetType(type);
                event.setTargetId(targetId);
                event.setEventType(type.name() + "_VIEW");
                event.setSessionId("seed-" + traveller.getId().toString().substring(0, 8));
                event = eventRepository.saveAndFlush(event);
                eventRepository.backdate(event.getId(), now.minus(Duration.ofHours(random.nextInt(29 * 24) + 1)));
                written += 1;
            }
            offset += 7;
        }
        log.info("Seeded {} browses", written);
    }

    private static UUID targetOf(CatalogSeedContext context, BrowseTarget type, String slug) {
        return switch (type) {
            case DESTINATION -> {
                Destination place = context.find(Destination.class, slug);
                yield place == null ? null : place.getId();
            }
            case STAY -> {
                Stay stay = context.find(Stay.class, slug);
                yield stay == null ? null : stay.getId();
            }
            case FOOD -> {
                Food food = context.find(Food.class, slug);
                yield food == null ? null : food.getId();
            }
            case ACTIVITY -> {
                Activity activity = context.find(Activity.class, slug);
                yield activity == null ? null : activity.getId();
            }
            case CORRIDOR -> {
                Corridor corridor = context.find(Corridor.class, slug);
                yield corridor == null ? null : corridor.getId();
            }
        };
    }

    @Override
    public void wipe() {
        eventRepository.deleteAllInBatch();
    }
}
