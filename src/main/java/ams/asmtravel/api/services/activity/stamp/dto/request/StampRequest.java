package ams.asmtravel.api.services.activity.stamp.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A check-in: the code scanned on site and the device's position (used once, not stored). Both are needed:
 * the code says which place, the position says the phone is there.
 *
 * @param code the scanned code; the whole scanned text is accepted too, the code being its last part
 */
public record StampRequest(
        @NotBlank @Size(max = 300) String code,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {
}
