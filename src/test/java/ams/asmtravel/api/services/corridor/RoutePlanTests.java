package ams.asmtravel.api.services.corridor;

import ams.asmtravel.api.services.corridor.domain.CorridorLeg;
import ams.asmtravel.api.services.corridor.domain.CorridorTransportType;
import ams.asmtravel.api.services.corridor.domain.GeoPoint;
import ams.asmtravel.api.services.corridor.domain.RoadRoute;
import ams.asmtravel.api.services.corridor.domain.RoadTravelMode;
import ams.asmtravel.api.services.corridor.domain.RoutePlan;
import ams.asmtravel.api.services.corridor.domain.RoutePlan.OffRoad;
import ams.asmtravel.api.services.corridor.domain.RoutePlan.RoadRun;
import ams.asmtravel.api.services.corridor.domain.RoutePlan.Waypoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static ams.asmtravel.api.services.corridor.domain.CorridorTransportType.BOAT;
import static ams.asmtravel.api.services.corridor.domain.CorridorTransportType.CAR;
import static ams.asmtravel.api.services.corridor.domain.CorridorTransportType.MOTORBIKE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** How a corridor's legs are grouped before Google is asked, and how a leg with no road is measured. */
class RoutePlanTests {

    private static final Waypoint PHNOM_PENH = new Waypoint("Phnom Penh", new GeoPoint(11.5686, 104.9319), CAR);
    private static final Waypoint KOH_DACH = new Waypoint("Koh Dach", new GeoPoint(11.6289, 104.9622), CAR);
    private static final Waypoint KAMPONG_CHAM = new Waypoint("Kampong Cham", new GeoPoint(11.9925, 105.4638), CAR);
    private static final Waypoint KOH_TRONG = new Waypoint("Koh Trong", new GeoPoint(12.4744, 106.0119), BOAT);
    private static final Waypoint KAMPI = new Waypoint("Kampi", new GeoPoint(12.5583, 106.0333), BOAT);

    @Test
    void roadLegsByTheSameMeansGoTogetherAndBoatsFollowTheRoadsOnTheirOwn() {
        RoutePlan plan = plan(PHNOM_PENH, KOH_DACH, KAMPONG_CHAM, KOH_TRONG, KOH_TRONG, KAMPI);

        assertThat(plan.segments(10)).containsExactly(
                new RoadRun(1, RoadTravelMode.DRIVE, List.of(PHNOM_PENH, KOH_DACH, KAMPONG_CHAM)),
                // By boat: drawn along the roads beside the river, in a request of its own.
                new RoadRun(3, RoadTravelMode.DRIVE, List.of(KAMPONG_CHAM, KOH_TRONG)),
                // A day that begins where the last one ended: no road to ask for.
                new OffRoad(4, KOH_TRONG, KOH_TRONG),
                new RoadRun(5, RoadTravelMode.DRIVE, List.of(KOH_TRONG, KAMPI)));
        assertThat(((RoadRun) plan.segments(10).get(1)).ownPace()).isTrue();
        assertThat(((RoadRun) plan.segments(10).get(0)).ownPace()).isFalse();
    }

    @Test
    void aChangeOfMeansOrTooManyStopoversStartsANewRequest() {
        Waypoint byMotorbike = new Waypoint("Kampong Cham", KAMPONG_CHAM.point(), MOTORBIKE);
        assertThat(plan(PHNOM_PENH, KOH_DACH, byMotorbike).segments(10)).containsExactly(
                new RoadRun(1, RoadTravelMode.DRIVE, List.of(PHNOM_PENH, KOH_DACH)),
                new RoadRun(2, RoadTravelMode.TWO_WHEELER, List.of(KOH_DACH, byMotorbike)));

        // One stopover allowed: three legs by car become two requests that share a point.
        Waypoint back = new Waypoint("Phnom Penh", PHNOM_PENH.point(), CAR);
        assertThat(plan(PHNOM_PENH, KOH_DACH, KAMPONG_CHAM, back).segments(1)).containsExactly(
                new RoadRun(1, RoadTravelMode.DRIVE, List.of(PHNOM_PENH, KOH_DACH, KAMPONG_CHAM)),
                new RoadRun(3, RoadTravelMode.DRIVE, List.of(KAMPONG_CHAM, back)));
    }

    @Test
    void aStopWithNoPositionIsNamedBeforeAnythingIsPlanned() {
        RoutePlan plan = plan(PHNOM_PENH, new Waypoint("Unmapped Pagoda", null, CAR));

        assertThat(plan.firstUnplaced()).isEqualTo("Unmapped Pagoda");
        assertThatThrownBy(() -> plan.segments(10)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aCrossingIsTimedBySpeedAndBoardingAndARoadByGoogle() {
        CorridorLeg crossing = CorridorLeg.offRoad(KOH_TRONG.point(), KAMPI.point(), BOAT);
        assertThat(crossing.distanceKm()).isEqualByComparingTo("9.61");
        assertThat(crossing.minutes()).isEqualTo(39);
        assertThat(CorridorLeg.offRoad(KOH_TRONG.point(), KOH_TRONG.point(), BOAT).minutes()).isZero();

        RoadRoute.Leg road = new RoadRoute.Leg(12_400, 1_500, "line");
        assertThat(CorridorLeg.road(road, CAR, false)).isEqualTo(new CorridorLeg(new BigDecimal("12.40"), 25, "line"));
        // Routed for a car because Google has no bicycle roads there: timed at a cyclist's speed.
        assertThat(CorridorLeg.road(road, CorridorTransportType.BICYCLE, true).minutes()).isEqualTo(50);
        // Beside the road by boat: the road's distance, at a boat's pace, plus boarding.
        assertThat(CorridorLeg.road(road, BOAT, false)).isEqualTo(new CorridorLeg(new BigDecimal("12.40"), 30 + 15, "line"));
    }

    private static RoutePlan plan(Waypoint... waypoints) {
        return new RoutePlan("signature", List.of(waypoints));
    }
}
