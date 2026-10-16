package ams.asmtravel.api.services.search;

import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.domain.NearbyDestination;
import ams.asmtravel.api.services.destination.destination.dto.response.DestinationResponse;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.review.review.service.ReviewService;
import ams.asmtravel.api.services.search.dto.request.SearchRequest;
import ams.asmtravel.api.services.search.dto.response.SearchHitResponse;
import ams.asmtravel.api.services.search.mapper.SearchMapper;
import ams.asmtravel.api.services.search.repository.NameLookupRepository;
import ams.asmtravel.api.services.search.service.impl.SearchServiceImpl;
import ams.asmtravel.api.services.verification.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Around a destination: its neighbours within the radius asked, nearest first, each with its distance; never itself. */
class NearbyDestinationsTests {

    private static final double LATITUDE = 11.5636;
    private static final double LONGITUDE = 104.9310;

    private final DestinationService destinations = mock(DestinationService.class);
    private SearchServiceImpl search;
    private UUID centre;

    @BeforeEach
    void setUp() {
        search = new SearchServiceImpl(destinations, mock(StayService.class), mock(FoodService.class), mock(CorridorService.class),
                mock(RegionService.class), mock(ProvinceService.class), mock(ReviewService.class), mock(VerificationService.class),
                mock(NameLookupRepository.class), new SearchMapper() { });
        centre = UUID.randomUUID();
        when(destinations.findSummary(centre)).thenReturn(Optional.of(new DestinationSummary(centre, "Royal Palace", "royal-palace",
                ListingStatus.PUBLISHED, null, null, null, BigDecimal.valueOf(LATITUDE), BigDecimal.valueOf(LONGITUDE), null, 0, null,
                Set.of(), Set.of(), Set.of())));
    }

    @Test
    void listsTheNeighboursNearestFirstWithTheirDistanceAndLeavesTheCentreOut() {
        List<NearbyDestination> around = List.of(
                near(centre, "Royal Palace", ListingStatus.PUBLISHED, 0),
                near(UUID.randomUUID(), "National Museum", ListingStatus.PUBLISHED, 314.4),
                near(UUID.randomUUID(), "Unfinished Draft", ListingStatus.DRAFT, 400),
                near(UUID.randomUUID(), "Russian Market", ListingStatus.PUBLISHED, 2_245.6));
        when(destinations.findNearbyWithDistance(eq(LATITUDE), eq(LONGITUDE), eq(5.0), anyInt())).thenReturn(around);

        Page<SearchHitResponse> page = search.search(nearby(5.0), PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(SearchHitResponse::name).containsExactly("National Museum", "Russian Market");
        assertThat(page.getContent()).extracting(SearchHitResponse::distanceMeters).containsExactly(314, 2_246);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void reachesTwentyFiveKilometresWhenNoRadiusIsAsked() {
        when(destinations.findNearbyWithDistance(anyDouble(), anyDouble(), anyDouble(), anyInt())).thenReturn(List.of());

        assertThat(search.search(nearby(null), PageRequest.of(0, 20)).getContent()).isEmpty();
        verify(destinations).findNearbyWithDistance(eq(LATITUDE), eq(LONGITUDE), eq(25.0), anyInt());
    }

    @Test
    void anOrdinarySearchCarriesNoDistance() {
        Page<DestinationResponse> found = new PageImpl<>(List.of(place(UUID.randomUUID(), "Royal Palace", ListingStatus.PUBLISHED)));
        when(destinations.searchPublished(any(), any())).thenReturn(found);

        List<SearchHitResponse> hits = search.search(request(null, null), PageRequest.of(0, 20)).getContent();

        assertThat(hits).singleElement().extracting(SearchHitResponse::distanceMeters).isNull();
    }

    private SearchRequest nearby(Double radiusKm) {
        return request(centre, radiusKm);
    }

    private static SearchRequest request(UUID near, Double radiusKm) {
        return new SearchRequest(null, null, null, null, null, null, null, null, null, null, near, radiusKm);
    }

    private static NearbyDestination near(UUID id, String name, ListingStatus status, double metres) {
        return new NearbyDestination(place(id, name, status), metres);
    }

    private static DestinationResponse place(UUID id, String name, ListingStatus status) {
        DestinationResponse place = mock(DestinationResponse.class);
        when(place.id()).thenReturn(id);
        when(place.name()).thenReturn(name);
        when(place.status()).thenReturn(status);
        return place;
    }
}
