package ams.asmtravel.api.services.location.province.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @param regionId optional zone; null keeps the current one
 * @param countryCode null means Cambodia ({@code KH})
 */
public record ProvinceRequest(
        UUID regionId,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 5) String countryCode,
        Boolean isActive
) {
}
