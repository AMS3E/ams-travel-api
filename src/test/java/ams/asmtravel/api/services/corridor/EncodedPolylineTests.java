package ams.asmtravel.api.services.corridor;

import ams.asmtravel.api.services.corridor.domain.EncodedPolyline;
import ams.asmtravel.api.services.corridor.domain.GeoPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EncodedPolylineTests {

    /** The example in Google's own description of the format. */
    @Test
    void googlesExampleReadsAndWritesTheSame() {
        List<GeoPoint> points = EncodedPolyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@");

        assertThat(points).containsExactly(new GeoPoint(38.5, -120.2), new GeoPoint(40.7, -120.95), new GeoPoint(43.252, -126.453));
        assertThat(EncodedPolyline.encode(points)).isEqualTo("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
    }

    /** Legs share their joining point; the joined line has it once, and stays a valid line. */
    @Test
    void legsJoinIntoOneLine() {
        GeoPoint phnomPenh = new GeoPoint(11.5686, 104.9319);
        GeoPoint kohDach = new GeoPoint(11.6289, 104.9622);
        GeoPoint kampongCham = new GeoPoint(11.9925, 105.4638);
        String first = EncodedPolyline.encode(List.of(phnomPenh, kohDach));
        String second = EncodedPolyline.encode(List.of(kohDach, kampongCham));

        assertThat(EncodedPolyline.decode(EncodedPolyline.join(List.of(first, second))))
                .containsExactly(phnomPenh, kohDach, kampongCham);
    }

    @Test
    void aCrossingIsMeasuredOverTheEarthsSurface() {
        GeoPoint kratie = new GeoPoint(12.4744, 106.0119);
        GeoPoint kampi = new GeoPoint(12.5583, 106.0333);

        assertThat(kratie.kilometresTo(kampi)).isCloseTo(9.6, within(0.2));
        assertThat(kratie.kilometresTo(kratie)).isZero();
    }
}
