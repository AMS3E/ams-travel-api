package ams.asmtravel.api.services.destination.activity.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.activity.domain.Activity;
import ams.asmtravel.api.services.destination.activity.domain.ActivityType;
import ams.asmtravel.api.services.destination.activity.repository.ActivityRepository;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

/**
 * Seeds the sample activities and experiences of the staging catalogue, published, each with its type,
 * its contact details and its Khmer. Tourism regions are named by code, as the region seed publishes them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivitySeeder implements CatalogSeedContributor {

    private final ActivityRepository activityRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return LISTINGS;
    }

    @Override
    public boolean alreadySeeded() {
        return activityRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, ActivitySeeder.class, "activities.json");
        Instant now = Instant.now();
        for (JsonNode node : rows) {
            Activity activity = new Activity();
            activity.setSlug(SeedJson.text(node, "slug"));
            activity.setName(SeedJson.text(node, "name"));
            activity.setType(SeedJson.enumValue(node, "type", ActivityType.class));
            activity.setShortDescription(SeedJson.text(node, "shortDescription"));
            activity.setDescription(SeedJson.text(node, "description"));
            activity.setSlogan(SeedJson.text(node, "slogan"));
            activity.setSloganBy(SeedJson.text(node, "sloganBy"));
            activity.setNameKm(SeedJson.text(node, "nameKm"));
            activity.setDescriptionKm(SeedJson.text(node, "descriptionKm"));
            activity.setSloganKm(SeedJson.text(node, "sloganKm"));
            activity.setSloganByKm(SeedJson.text(node, "sloganByKm"));
            activity.setAddressKm(SeedJson.text(node, "addressKm"));
            activity.setAddress(SeedJson.text(node, "address"));
            activity.setProvince(context.require(Province.class, SeedJson.text(node, "provinceSlug"), "province"));
            activity.setLatitude(SeedJson.money(node, "latitude"));
            activity.setLongitude(SeedJson.money(node, "longitude"));
            activity.setPhone(SeedJson.text(node, "phone"));
            activity.setContactEmail(SeedJson.text(node, "contactEmail"));
            activity.setWebsite(SeedJson.text(node, "website"));
            activity.setTelegram(SeedJson.text(node, "telegram"));
            activity.setWhatsapp(SeedJson.text(node, "whatsapp"));
            activity.setFacebookUrl(SeedJson.text(node, "facebookUrl"));
            activity.setCoverKey(context.imageKey(node));
            String near = SeedJson.text(node, "destinationSlug");
            activity.setDestination(near == null ? null : context.find(Destination.class, near));
            activity.setCategories(new HashSet<>(context.resolve(Category.class, SeedJson.strings(node, "categorySlugs"))));
            activity.setTags(new HashSet<>(context.resolve(Tag.class, SeedJson.strings(node, "tagSlugs"))));
            activity.setInterests(new HashSet<>(context.resolve(Interest.class, SeedJson.strings(node, "interestSlugs"))));
            activity.setTourismRegions(new HashSet<>(context.resolve(Region.class, SeedJson.strings(node, "tourismRegionCodes"))));
            activity.setStatus(ListingStatus.PUBLISHED);
            activity.setPublishedAt(now);
            context.publish(Activity.class, activity.getSlug(), activityRepository.save(activity));
        }
        log.info("Seeded {} activities", rows.size());
    }

    @Override
    public void wipe() {
        activityRepository.deleteAll();
        activityRepository.flush();
    }
}
