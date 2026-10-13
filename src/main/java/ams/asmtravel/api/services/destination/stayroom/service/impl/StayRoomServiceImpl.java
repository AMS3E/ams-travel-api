package ams.asmtravel.api.services.destination.stayroom.service.impl;

import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomQuote;
import ams.asmtravel.api.services.destination.stayroom.dto.request.StayQuoteRequest;
import ams.asmtravel.api.services.destination.stayroom.dto.response.RoomQuoteResponse;
import ams.asmtravel.api.services.destination.stayroom.dto.response.StayQuoteResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.destination.stay.domain.StaySavedEvent;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomBathroom;
import ams.asmtravel.api.services.destination.stayroom.domain.RoomPaymentTerms;
import ams.asmtravel.api.services.destination.stayroom.domain.StayRoom;
import ams.asmtravel.api.services.destination.stayroom.dto.request.StayRoomRequest;
import ams.asmtravel.api.services.destination.stayroom.dto.response.StayRoomResponse;
import ams.asmtravel.api.services.destination.stayroom.exception.StayRoomNotFoundException;
import ams.asmtravel.api.services.destination.stayroom.mapper.StayRoomMapper;
import ams.asmtravel.api.services.destination.stayroom.repository.StayRoomRepository;
import ams.asmtravel.api.services.destination.stayroom.service.StayRoomService;
import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityDeletingEvent;
import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityLevel;
import ams.asmtravel.api.services.taxonomy.amenity.service.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StayRoomServiceImpl implements StayRoomService {

    private static final ZoneId CAMBODIA = ZoneId.of("Asia/Phnom_Penh");
    private static final int MAX_QUOTE_NIGHTS = 30;

    private final StayRoomRepository roomRepository;
    private final StayRoomMapper mapper;
    private final StayService stayService;
    private final AmenityService amenityService;

    @Override
    @Transactional(readOnly = true)
    public List<StayRoomResponse> getAll(UUID stayId) {
        boolean everything = Authorities.isBackoffice(Authorities.current());
        List<StayRoom> rooms = stayId == null
                ? roomRepository.findAllByOrderBySortOrderAscCreatedAtAsc()
                : roomRepository.findByStayIdOrderBySortOrderAscCreatedAtAsc(stayId);
        return rooms.stream().filter(room -> everything || onSale(room)).map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StayRoomResponse findById(UUID id) {
        StayRoom room = find(id);
        if (!onSale(room) && !Authorities.isBackoffice(Authorities.current())) {
            throw new StayRoomNotFoundException();
        }
        return mapper.toResponse(room);
    }

    @Override
    @Transactional
    public StayRoomResponse create(StayRoomRequest request) {
        Stay stay = writable(request.stayId());
        StayRoom room = new StayRoom();
        room.setStay(stay);
        room.setSortOrder(roomRepository.findByStayIdOrderBySortOrderAscCreatedAtAsc(stay.getId()).size());
        apply(room, request);
        room = roomRepository.saveAndFlush(room);
        refreshPrice(stay.getId());
        return mapper.toResponse(room);
    }

    @Override
    @Transactional
    public StayRoomResponse update(UUID id, StayRoomRequest request) {
        StayRoom room = find(id);
        writable(room.getStay().getId());
        if (!room.getStay().getId().equals(request.stayId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A room stays with the listing it was made for");
        }
        apply(room, request);
        room = roomRepository.saveAndFlush(room);
        refreshPrice(room.getStay().getId());
        return mapper.toResponse(room);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        StayRoom room = find(id);
        UUID stayId = writable(room.getStay().getId()).getId();
        roomRepository.delete(room);
        roomRepository.flush();
        refreshPrice(stayId);
    }

    /** After a stay edit, the rooms' "from" price wins over what the form sent. */
    /** An amenity rooms still list cannot be deleted: retire it instead. */
    @EventListener
    public void onAmenityDeleting(AmenityDeletingEvent event) {
        long listing = roomRepository.countByAmenity("%\"" + event.slug() + "\"%");
        if (listing > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    listing + (listing == 1 ? " room still lists it" : " rooms still list it") + ": retire the amenity instead of deleting it");
        }
    }

    @EventListener
    public void onStaySaved(StaySavedEvent event) {
        refreshPrice(event.stayId());
    }

    private void apply(StayRoom room, StayRoomRequest request) {
        room.setName(request.name().trim());
        room.setRoomType(request.roomType());
        room.setRoomCount(orDefault(request.roomCount(), 1));
        room.setSingleBeds(orDefault(request.singleBeds(), 0));
        room.setDoubleBeds(orDefault(request.doubleBeds(), 0));
        room.setQueenBeds(orDefault(request.queenBeds(), 0));
        room.setKingBeds(orDefault(request.kingBeds(), 0));
        room.setBunkBeds(orDefault(request.bunkBeds(), 0));
        room.setSofaBeds(orDefault(request.sofaBeds(), 0));
        room.setFutonBeds(orDefault(request.futonBeds(), 0));
        if (room.bedCount() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add at least one bed");
        }
        room.setMaxAdults(orDefault(request.maxAdults(), 2));
        room.setMaxChildren(orDefault(request.maxChildren(), 0));
        room.setSizeSqm(request.sizeSqm());
        room.setBedrooms(orDefault(request.bedrooms(), 1));
        room.setLivingRooms(orDefault(request.livingRooms(), 0));
        room.setBathroomCount(orDefault(request.bathroomCount(), 1));
        room.setFloor(request.floor());
        room.setLiftAccess(Boolean.TRUE.equals(request.liftAccess()));
        room.setHousekeeping(request.housekeeping());
        room.setRoomView(request.view());
        room.setBathroom(request.bathroom() == null ? RoomBathroom.PRIVATE : request.bathroom());

        /* Amenities are the taxonomy's, offered in the room; unknown slugs are refused. */
        List<String> amenities = request.amenities() == null ? List.of()
                : request.amenities().stream().map(String::trim).filter(one -> !one.isEmpty()).distinct().toList();
        List<String> unknown = amenityService.unknown(amenities, AmenityLevel.ROOM);
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such room amenity: " + String.join(", ", unknown));
        }
        room.setAmenities(new ArrayList<>(amenities));

        room.setSmokingAllowed(Boolean.TRUE.equals(request.smokingAllowed()));
        room.setPricePerNight(request.pricePerNight());
        room.setBreakfastIncluded(Boolean.TRUE.equals(request.breakfastIncluded()));
        room.setFreeCancellationDays(request.freeCancellationDays());
        room.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        room.setHighlight(request.highlight() == null || request.highlight().isBlank() ? null : request.highlight().trim());
        room.setHasWindow(!Boolean.FALSE.equals(request.hasWindow()));
        room.setExtraBeds(orDefault(request.extraBeds(), 0));
        room.setExtraBedPrice(room.getExtraBeds() > 0 ? request.extraBedPrice() : null);
        room.setCots(orDefault(request.cots(), 0));
        room.setCotPrice(room.getCots() > 0 ? request.cotPrice() : null);
        room.setChildrenFreeUnderAge(request.childrenFreeUnderAge());
        room.setHighSeasonPrice(request.highSeasonPrice());
        room.setWeekendPrice(request.weekendPrice());
        room.setTaxesIncluded(!Boolean.FALSE.equals(request.taxesIncluded()));
        room.setMinNights(orDefault(request.minNights(), 1));
        room.setWeeklyPrice(request.weeklyPrice());
        room.setMonthlyPrice(request.monthlyPrice());
        room.setPaymentTerms(request.paymentTerms());
        /* Deposit percent only applies with DEPOSIT terms. */
        room.setDepositPercent(request.paymentTerms() == RoomPaymentTerms.DEPOSIT ? request.depositPercent() : null);
        room.setPhotoKeys(new ArrayList<>(request.photoKeys() == null ? List.of()
                : request.photoKeys().stream().map(String::trim).distinct().toList()));
        if (request.sortOrder() != null) {
            room.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            room.setIsActive(request.isActive());
        }
    }

    /** Cheapest active room sets the "from" price; with no priced room the stay keeps its own. */
    private void refreshPrice(UUID stayId) {
        roomRepository.findByStayIdOrderBySortOrderAscCreatedAtAsc(stayId).stream()
                .filter(room -> Boolean.TRUE.equals(room.getIsActive()))
                .map(StayRoom::getPricePerNight)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .ifPresent(cheapest -> stayService.setPriceFrom(stayId, cheapest));
    }

    private static boolean onSale(StayRoom room) {
        return Boolean.TRUE.equals(room.getIsActive()) && room.getStay().getStatus() == ListingStatus.PUBLISHED;
    }

    private static int orDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private Stay writable(UUID stayId) {
        if (!stayService.exists(stayId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such stay listing");
        }
        if (!stayService.canWrite(stayId, Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change this listing's rooms");
        }
        return stayService.getEntity(stayId);
    }

    @Override
    @Transactional(readOnly = true)
    public StayQuoteResponse quote(StayQuoteRequest request) {
        if (request.stayId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name the stay: ?stayId=");
        }
        StayResponse stay = stayService.findById(request.stayId());
        Dates dates = dates(request);
        Set<DayOfWeek> weekend = weekendNights(stay.weekendNights());
        List<RoomQuoteResponse> quotes = roomRepository.findByStayIdOrderBySortOrderAscCreatedAtAsc(stay.id()).stream()
                .filter(room -> Boolean.TRUE.equals(room.getIsActive()))
                .map(room -> quoted(RoomQuote.of(room, weekend, dates.checkIn(), dates.checkOut(), dates.rooms(), dates.adults(), dates.children()), stay.currency()))
                .toList();
        BigDecimal from = quotes.stream().filter(RoomQuoteResponse::bookable).map(RoomQuoteResponse::total)
                .min(Comparator.naturalOrder()).orElse(null);
        return new StayQuoteResponse(stay.id(), dates.checkIn(), dates.checkOut(), dates.nights(), dates.rooms(), dates.adults(),
                dates.children(), stay.currency(), stay.weekendNights(), from, quotes);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomQuoteResponse quoteRoom(UUID roomId, StayQuoteRequest request) {
        StayRoom room = find(roomId);
        StayResponse stay = stayService.findById(room.getStay().getId());
        Dates dates = dates(request);
        return quoted(RoomQuote.of(room, weekendNights(stay.weekendNights()), dates.checkIn(), dates.checkOut(),
                dates.rooms(), dates.adults(), dates.children()), stay.currency());
    }

    /** The nights and guests asked for, with the defaults filled in and the limits kept. */
    private record Dates(LocalDate checkIn, LocalDate checkOut, int nights, int rooms, int adults, int children) {
    }

    private static Dates dates(StayQuoteRequest request) {
        LocalDate checkIn = request.checkIn() == null ? LocalDate.now(CAMBODIA) : request.checkIn();
        LocalDate checkOut = request.checkOut() == null ? checkIn.plusDays(1) : request.checkOut();
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check-out must come after check-in");
        }
        if (nights > MAX_QUOTE_NIGHTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A price preview covers at most " + MAX_QUOTE_NIGHTS + " nights");
        }
        return new Dates(checkIn, checkOut, (int) nights, orDefault(request.rooms(), 1), orDefault(request.adults(), 2),
                orDefault(request.children(), 0));
    }

    private static Set<DayOfWeek> weekendNights(List<String> names) {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (names != null) {
            for (String name : names) {
                try {
                    days.add(DayOfWeek.valueOf(name.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    // A night the host named wrongly counts as an ordinary one.
                }
            }
        }
        return days;
    }

    private static RoomQuoteResponse quoted(RoomQuote quote, String currency) {
        return new RoomQuoteResponse(quote.roomId(), quote.name(), currency, quote.pricePerNight(), quote.weekendPrice(),
                quote.nights().size(), quote.weekendNightCount(), quote.nights(), quote.total(), quote.fitsGuests(),
                quote.enoughRooms(), quote.meetsMinNights(), quote.minNights(), quote.breakfastIncluded(), quote.taxesIncluded(),
                quote.bookable());
    }

    private StayRoom find(UUID id) {
        return roomRepository.findById(id).orElseThrow(StayRoomNotFoundException::new);
    }
}
