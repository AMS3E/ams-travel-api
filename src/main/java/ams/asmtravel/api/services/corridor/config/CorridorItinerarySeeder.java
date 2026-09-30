package ams.asmtravel.api.services.corridor.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorDay;
import ams.asmtravel.api.services.corridor.domain.CorridorStop;
import ams.asmtravel.api.services.corridor.domain.CorridorTransportType;
import ams.asmtravel.api.services.corridor.repository.CorridorDayRepository;
import ams.asmtravel.api.services.corridor.repository.CorridorRepository;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Seeds corridor days and stops. Roads are not routed here; the route service does that on startup. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CorridorItinerarySeeder implements CatalogSeedContributor {

    private final CorridorRepository corridorRepository;
    private final CorridorDayRepository dayRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return DETAILS;
    }

    @Override
    public boolean alreadySeeded() {
        return dayRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        int days = 0;
        int stops = 0;
        for (JsonNode itinerary : SeedJson.rows(objectMapper, CorridorItinerarySeeder.class, "corridor-itineraries.json")) {
            Corridor corridor = context.require(Corridor.class, SeedJson.text(itinerary, "corridorSlug"), "corridor");
            List<CorridorStop> inOrder = new ArrayList<>();
            int dayNumber = 0;
            for (JsonNode dayNode : SeedJson.array(itinerary, "days")) {
                CorridorDay day = new CorridorDay();
                day.setCorridor(corridor);
                day.setDayNumber(++dayNumber);
                day.setTitle(SeedJson.text(dayNode, "title"));
                day.setDescription(SeedJson.text(dayNode, "description"));
                day.setStartTime(time(dayNode, "startTime"));
                day.setNotes(SeedJson.text(dayNode, "notes"));
                int stopOrder = 0;
                for (JsonNode stopNode : SeedJson.array(dayNode, "stops")) {
                    Destination destination = context.require(Destination.class, SeedJson.text(stopNode, "destinationSlug"), "destination");
                    CorridorStop stop = new CorridorStop();
                    stop.setDay(day);
                    stop.setStopOrder(++stopOrder);
                    stop.setDestination(destination);
                    stop.setArrivalTime(time(stopNode, "arrivalTime"));
                    stop.setDepartureTime(time(stopNode, "departureTime"));
                    stop.setRecommendedDurationMinutes(stopNode.hasNonNull("recommendedDurationMinutes")
                            ? stopNode.get("recommendedDurationMinutes").asInt() : null);
                    String transport = SeedJson.text(stopNode, "transportType");
                    stop.setTransportType(transport == null ? null : CorridorTransportType.valueOf(transport));
                    stop.setTitle(SeedJson.text(stopNode, "title"));
                    stop.setDescription(SeedJson.text(stopNode, "description"));
                    stop.setNotes(SeedJson.text(stopNode, "notes"));
                    day.getStops().add(stop);
                    inOrder.add(stop);
                    stops += 1;
                }
                corridor.getDays().add(day);
                days += 1;
            }
            corridor.summarise(inOrder);
            corridorRepository.save(corridor);
        }
        log.info("Seeded {} corridor days with {} stops", days, stops);
    }

    private static LocalTime time(JsonNode node, String field) {
        String text = SeedJson.text(node, field);
        return text == null ? null : LocalTime.parse(text);
    }
}
