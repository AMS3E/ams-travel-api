package ams.asmtravel.api.services.location.province.dto.request;

import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param coverKey  the uploaded picture's key (folder {@code province}); null keeps the current one, empty removes it
 * @param latitude  its centre, with {@code longitude}: both or neither; both missing keeps the current centre
 */
public record ProvinceRequest(
        UUID regionId,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 5) String countryCode,
        Boolean isActive,
        @MediaKey(allowEmpty = true, message = "Upload the picture first, then send its key") String coverKey,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        /** Khmer twins: null keeps what is stored, blank clears it. */
        @Size(max = 150) String nameKm
) {
}
