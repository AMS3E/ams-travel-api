package ams.asmtravel.api.services.destination.food.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.food.domain.AlcoholOption;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.domain.FoodParking;
import ams.asmtravel.api.services.destination.food.domain.FoodType;
import ams.asmtravel.api.services.destination.food.domain.ReservationPolicy;
import ams.asmtravel.api.services.destination.food.repository.FoodRepository;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

/** Seeds the sample food places of the staging catalogue, published, with their week of hours. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FoodSeeder implements CatalogSeedContributor {

    private final FoodRepository foodRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return LISTINGS;
    }

    @Override
    public boolean alreadySeeded() {
        return foodRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, FoodSeeder.class, "foods.json");
        Instant now = Instant.now();
        for (JsonNode node : rows) {
            Food food = new Food();
            food.setSlug(SeedJson.text(node, "slug"));
            food.setName(SeedJson.text(node, "name"));
            food.setType(SeedJson.enumValue(node, "type", FoodType.class));
            food.setShortDescription(SeedJson.text(node, "shortDescription"));
            food.setDescription(SeedJson.text(node, "description"));
            food.setSlogan(SeedJson.text(node, "slogan"));
            food.setSloganBy(SeedJson.text(node, "sloganBy"));
            food.setAddress(SeedJson.text(node, "address"));
            food.setProvince(context.require(Province.class, SeedJson.text(node, "provinceSlug"), "province"));
            food.setLatitude(SeedJson.money(node, "latitude"));
            food.setLongitude(SeedJson.money(node, "longitude"));
            food.setPriceRange(SeedJson.text(node, "priceRange"));
            food.setAverageMealPrice(SeedJson.money(node, "averageMealPrice"));
            food.setPriceFrom(SeedJson.money(node, "priceFrom"));
            food.setCurrency(SeedJson.textOr(node, "currency", "USD"));
            food.setOpeningHours(new LinkedHashMap<>(SeedJson.stringMap(node, "openingHours")));
            food.setPhone(SeedJson.text(node, "phone"));
            food.setContactEmail(SeedJson.text(node, "contactEmail"));
            food.setWebsite(SeedJson.text(node, "website"));
            food.setCuisines(new ArrayList<>(SeedJson.strings(node, "cuisines")));
            food.setMealsServed(new ArrayList<>(SeedJson.strings(node, "mealsServed")));
            food.setDietary(new ArrayList<>(SeedJson.strings(node, "dietary")));
            food.setServiceOptions(new ArrayList<>(SeedJson.strings(node, "serviceOptions")));
            food.setAmenities(new ArrayList<>(SeedJson.strings(node, "amenities")));
            food.setSpecialties(new ArrayList<>(SeedJson.strings(node, "specialties")));
            food.setGoodFor(new ArrayList<>(SeedJson.strings(node, "goodFor")));
            food.setMenuLanguages(new ArrayList<>(SeedJson.strings(node, "menuLanguages")));
            food.setDeliveryPartners(new ArrayList<>(SeedJson.strings(node, "deliveryPartners")));
            food.setPaymentMethods(new ArrayList<>(SeedJson.strings(node, "paymentMethods")));
            food.setLanguagesSpoken(new ArrayList<>(SeedJson.strings(node, "languagesSpoken")));
            food.setAlcohol(SeedJson.enumValue(node, "alcohol", AlcoholOption.class));
            food.setParkingOption(SeedJson.enumValue(node, "parkingOption", FoodParking.class));
            food.setReservations(SeedJson.enumValue(node, "reservations", ReservationPolicy.class));
            food.setSeatingCapacity(SeedJson.integer(node, "seatingCapacity"));
            food.setTelegram(SeedJson.text(node, "telegram"));
            food.setWhatsapp(SeedJson.text(node, "whatsapp"));
            food.setFacebookUrl(SeedJson.text(node, "facebookUrl"));
            food.setYearOpened(SeedJson.integer(node, "yearOpened"));
            food.setIsLocalSpecialty(SeedJson.bool(node, "isLocalSpecialty", false));
            food.setCoverKey(context.imageKey(node));
            String near = SeedJson.text(node, "destinationSlug");
            food.setDestination(near == null ? null : context.find(Destination.class, near));
            food.setCategories(new HashSet<>(context.resolve(Category.class, SeedJson.strings(node, "categorySlugs"))));
            food.setTags(new HashSet<>(context.resolve(Tag.class, SeedJson.strings(node, "tagSlugs"))));
            food.setInterests(new HashSet<>(context.resolve(Interest.class, SeedJson.strings(node, "interestSlugs"))));
            food.setStatus(ListingStatus.PUBLISHED);
            food.setPublishedAt(now);
            context.publish(Food.class, food.getSlug(), foodRepository.save(food));
        }
        log.info("Seeded {} food places", rows.size());
    }

    @Override
    public void wipe() {
        foodRepository.deleteAll();
        foodRepository.flush();
    }
}
