package ams.asmtravel.api.support;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.repository.DestinationRepository;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.province.repository.ProvinceRepository;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.location.region.repository.RegionRepository;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.category.repository.CategoryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The few catalogue rows an integration test needs, written straight through
 * the repositories inside the test's own transaction.
 *
 * <p>RECONSTRUCTED on 2026-09-30 (see {@link Callers}). Signatures follow the
 * call sites in the nine tests that autowire it. Slugs carry a counter so a
 * test may ask for the same name, or a province, more than once.
 */
@Component
public class TestCatalogue {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final RegionRepository regions;
    private final ProvinceRepository provinces;
    private final CategoryRepository categories;
    private final DestinationRepository destinations;

    public TestCatalogue(RegionRepository regions, ProvinceRepository provinces, CategoryRepository categories,
                         DestinationRepository destinations) {
        this.regions = regions;
        this.provinces = provinces;
        this.categories = categories;
        this.destinations = destinations;
    }

    /** A province in a zone of its own (tests delete the zone by {@code province.getRegion().getId()}). */
    public Province province() {
        int n = COUNTER.incrementAndGet();
        Region zone = new Region();
        zone.setName("Test Zone " + n);
        zone.setCode("TZ-" + n);
        zone.setKind(RegionKind.ZONE);
        zone = regions.save(zone);
        Province province = new Province();
        province.setName("Test Province " + n);
        province.setSlug("test-province-" + n);
        province.setRegion(zone);
        return provinces.save(province);
    }

    public Category category(String name) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug(name));
        return categories.save(category);
    }

    /** A published attraction at these coordinates, owned by nobody. */
    public Destination published(Province province, String name, double latitude, double longitude) {
        return place(province, name, latitude, longitude, ListingStatus.PUBLISHED, null, Set.of());
    }

    public Destination place(Province province, String name, double latitude, double longitude,
                             ListingStatus status, UUID createdBy, Set<Category> categories) {
        Destination destination = new Destination();
        destination.setName(name);
        destination.setSlug(slug(name));
        destination.setShortDescription(name + " in " + province.getName());
        destination.setLatitude(BigDecimal.valueOf(latitude));
        destination.setLongitude(BigDecimal.valueOf(longitude));
        destination.setProvince(province);
        destination.setStatus(status);
        destination.setCreatedBy(createdBy);
        if (status == ListingStatus.PUBLISHED) {
            destination.setPublishedAt(Instant.now());
        }
        destination.getCategories().addAll(categories);
        return destinations.save(destination);
    }

    private static String slug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return base + "-" + COUNTER.incrementAndGet();
    }
}
