package ams.asmtravel.api.services.activity.stamp.dto.request;

import ams.asmtravel.api.services.activity.stamp.domain.StampTarget;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Issue a destination's check-in code; the current one, if any, is replaced. */
public record StampCodeRequest(@NotNull StampTarget targetType, @NotNull UUID targetId) {
}
