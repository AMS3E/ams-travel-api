package ams.asmtravel.api.services.destination.stayroom.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomHousekeeping;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomPaymentTerms;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomType;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomView;
import ams.asmtravel.api.services.destination.stayroom.domain.StayRoom;
import ams.asmtravel.api.services.destination.stayroom.repository.StayRoomRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Seeds each sample stay's rooms, and sets the stay's lowest price from them as the API does. */
@Component
@RequiredArgsConstructor
@Slf4j
public class StayRoomSeeder implements CatalogSeedContributor {

    private final StayRoomRepository roomRepository;
    private final StayService stayService;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return DETAILS;
    }

    @Override
    public boolean alreadySeeded() {
        return roomRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        int written = 0;
        for (JsonNode node : SeedJson.rows(objectMapper, StayRoomSeeder.class, "stay-rooms.json")) {
            Stay stay = context.require(Stay.class, SeedJson.text(node, "stay"), "stay");
            BigDecimal cheapest = null;
            int sortOrder = 0;
            for (JsonNode roomNode : SeedJson.array(node, "rooms")) {
                StayRoom room = new StayRoom();
                room.setStay(stay);
                room.setName(SeedJson.text(roomNode, "name"));
                room.setHighlight(SeedJson.text(roomNode, "highlight"));
                room.setRoomType(SeedJson.enumValue(roomNode, "roomType", RoomType.class));
                room.setRoomCount(Objects.requireNonNullElse(SeedJson.integer(roomNode, "roomCount"), 1));
                room.setSingleBeds(Objects.requireNonNullElse(SeedJson.integer(roomNode, "singleBeds"), 0));
                room.setDoubleBeds(Objects.requireNonNullElse(SeedJson.integer(roomNode, "doubleBeds"), 0));
                room.setQueenBeds(Objects.requireNonNullElse(SeedJson.integer(roomNode, "queenBeds"), 0));
                room.setKingBeds(Objects.requireNonNullElse(SeedJson.integer(roomNode, "kingBeds"), 0));
                room.setBunkBeds(Objects.requireNonNullElse(SeedJson.integer(roomNode, "bunkBeds"), 0));
                room.setSofaBeds(Objects.requireNonNullElse(SeedJson.integer(roomNode, "sofaBeds"), 0));
                room.setMaxAdults(Objects.requireNonNullElse(SeedJson.integer(roomNode, "maxAdults"), 2));
                room.setMaxChildren(Objects.requireNonNullElse(SeedJson.integer(roomNode, "maxChildren"), 0));
                room.setSizeSqm(SeedJson.money(roomNode, "sizeSqm"));
                room.setBedrooms(Objects.requireNonNullElse(SeedJson.integer(roomNode, "bedrooms"), 1));
                room.setLivingRooms(Objects.requireNonNullElse(SeedJson.integer(roomNode, "livingRooms"), 0));
                room.setBathroomCount(Objects.requireNonNullElse(SeedJson.integer(roomNode, "bathroomCount"), 1));
                room.setFloor(SeedJson.integer(roomNode, "floor"));
                room.setLiftAccess(SeedJson.bool(roomNode, "liftAccess", false));
                room.setHousekeeping(SeedJson.enumValue(roomNode, "housekeeping", RoomHousekeeping.class));
                room.setRoomView(SeedJson.enumValue(roomNode, "roomView", RoomView.class));
                room.setAmenities(new ArrayList<>(SeedJson.strings(roomNode, "amenities")));
                room.setSmokingAllowed(SeedJson.bool(roomNode, "smokingAllowed", false));
                room.setHasWindow(SeedJson.bool(roomNode, "hasWindow", true));
                room.setExtraBeds(Objects.requireNonNullElse(SeedJson.integer(roomNode, "extraBeds"), 0));
                room.setExtraBedPrice(SeedJson.money(roomNode, "extraBedPrice"));
                room.setCots(Objects.requireNonNullElse(SeedJson.integer(roomNode, "cots"), 0));
                room.setCotPrice(SeedJson.money(roomNode, "cotPrice"));
                room.setChildrenFreeUnderAge(SeedJson.integer(roomNode, "childrenFreeUnderAge"));
                room.setPricePerNight(SeedJson.money(roomNode, "pricePerNight"));
                room.setBreakfastIncluded(SeedJson.bool(roomNode, "breakfastIncluded", false));
                room.setHighSeasonPrice(SeedJson.money(roomNode, "highSeasonPrice"));
                room.setWeekendPrice(SeedJson.money(roomNode, "weekendPrice"));
                room.setTaxesIncluded(SeedJson.bool(roomNode, "taxesIncluded", true));
                room.setMinNights(Objects.requireNonNullElse(SeedJson.integer(roomNode, "minNights"), 1));
                room.setDepositPercent(SeedJson.integer(roomNode, "depositPercent"));
                room.setPaymentTerms(SeedJson.enumValue(roomNode, "paymentTerms", RoomPaymentTerms.class));
                room.setFreeCancellationDays(SeedJson.integer(roomNode, "freeCancellationDays"));
                room.setDescription(SeedJson.text(roomNode, "description"));
                List<String> photos = new ArrayList<>();
                for (String fileName : SeedJson.strings(roomNode, "photos")) {
                    String key = context.imageKey(fileName);
                    if (key != null) {
                        photos.add(key);
                    }
                }
                room.setPhotoKeys(photos);
                room.setSortOrder(sortOrder++);
                room.setIsActive(true);
                roomRepository.save(room);
                written += 1;
                if (room.getPricePerNight() != null && (cheapest == null || room.getPricePerNight().compareTo(cheapest) < 0)) {
                    cheapest = room.getPricePerNight();
                }
            }
            if (cheapest != null) {
                stayService.setPriceFrom(stay.getId(), cheapest);
            }
        }
        log.info("Seeded {} stay rooms", written);
    }

    @Override
    public void wipe() {
        roomRepository.deleteAllInBatch();
    }
}
