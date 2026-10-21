package ams.asmtravel.api.services.destination.stay.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.stay.domain.BreakfastOption;
import ams.asmtravel.api.services.destination.stay.domain.ParkingOption;
import ams.asmtravel.api.services.destination.stay.domain.PetsPolicy;
import ams.asmtravel.api.services.destination.stay.domain.SmokingPolicy;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.destination.stay.domain.StayType;
import ams.asmtravel.api.services.destination.stay.repository.StayRepository;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Seeds the sample stays of the staging catalogue, published; their rooms come with the details. */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaySeeder implements CatalogSeedContributor {

    private final StayRepository stayRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return LISTINGS;
    }

    @Override
    public boolean alreadySeeded() {
        return stayRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, StaySeeder.class, "stays.json");
        for (JsonNode node : rows) {
            Stay stay = new Stay();
            stay.setName(SeedJson.text(node, "name"));
            stay.setType(SeedJson.enumValue(node, "type", StayType.class));
            stay.setStarRating(SeedJson.money(node, "starRating"));
            stay.setShortDescription(SeedJson.text(node, "shortDescription"));
            stay.setDescription(SeedJson.text(node, "description"));
            stay.setSlogan(SeedJson.text(node, "slogan"));
            stay.setSloganBy(SeedJson.text(node, "sloganBy"));
            stay.setNameKm(SeedJson.text(node, "nameKm"));
            stay.setDescriptionKm(SeedJson.text(node, "descriptionKm"));
            stay.setSloganKm(SeedJson.text(node, "sloganKm"));
            stay.setSloganByKm(SeedJson.text(node, "sloganByKm"));
            stay.setAddressKm(SeedJson.text(node, "addressKm"));
            stay.setPolicies(SeedJson.text(node, "policies"));
            stay.setPriceFrom(SeedJson.money(node, "priceFrom"));
            stay.setCurrency(SeedJson.textOr(node, "currency", "USD"));
            stay.setAmenities(new ArrayList<>(SeedJson.strings(node, "amenities")));
            stay.setSpecialties(new ArrayList<>(SeedJson.strings(node, "specialties")));
            stay.setCoverKey(context.imageKey(node));
            stay.setCheckInFrom(SeedJson.time(node, "checkInFrom"));
            stay.setCheckInUntil(SeedJson.time(node, "checkInUntil"));
            stay.setCheckOutFrom(SeedJson.time(node, "checkOutFrom"));
            stay.setCheckOutUntil(SeedJson.time(node, "checkOutUntil"));
            stay.setMinCheckInAge(SeedJson.integer(node, "minCheckInAge"));
            stay.setChildrenAllowed(SeedJson.bool(node, "childrenAllowed", true));
            stay.setPetsPolicy(SeedJson.enumValue(node, "petsPolicy", PetsPolicy.class));
            stay.setSmokingPolicy(SeedJson.enumValue(node, "smokingPolicy", SmokingPolicy.class));
            stay.setPartiesAllowed(SeedJson.bool(node, "partiesAllowed", false));
            stay.setQuietHours(SeedJson.text(node, "quietHours"));
            stay.setBreakfastOption(SeedJson.enumValue(node, "breakfastOption", BreakfastOption.class));
            stay.setBreakfastTypes(new ArrayList<>(SeedJson.strings(node, "breakfastTypes")));
            stay.setBreakfastPrice(SeedJson.money(node, "breakfastPrice"));
            stay.setParkingOption(SeedJson.enumValue(node, "parkingOption", ParkingOption.class));
            stay.setPaymentMethods(new ArrayList<>(SeedJson.strings(node, "paymentMethods")));
            stay.setLanguagesSpoken(new ArrayList<>(SeedJson.strings(node, "languagesSpoken")));
            stay.setPhone(SeedJson.text(node, "phone"));
            stay.setContactEmail(SeedJson.text(node, "contactEmail"));
            stay.setWebsite(SeedJson.text(node, "website"));
            stay.setTelegram(SeedJson.text(node, "telegram"));
            stay.setWhatsapp(SeedJson.text(node, "whatsapp"));
            stay.setFacebookUrl(SeedJson.text(node, "facebookUrl"));
            stay.setYearOpened(SeedJson.integer(node, "yearOpened"));
            stay.setAddress(SeedJson.text(node, "address"));
            stay.setProvince(context.require(Province.class, SeedJson.text(node, "provinceSlug"), "province"));
            stay.setLatitude(SeedJson.money(node, "latitude"));
            stay.setLongitude(SeedJson.money(node, "longitude"));
            String near = SeedJson.text(node, "destinationSlug");
            stay.setDestination(near == null ? null : context.find(Destination.class, near));
            stay.setInterests(new HashSet<>(context.resolve(Interest.class, SeedJson.strings(node, "interestSlugs"))));
            stay.setStatus(ListingStatus.PUBLISHED);
            context.publish(Stay.class, SeedJson.text(node, "key"), stayRepository.save(stay));
        }
        log.info("Seeded {} stays", rows.size());
    }

    @Override
    public void wipe() {
        stayRepository.deleteAll();
        stayRepository.flush();
    }
}
