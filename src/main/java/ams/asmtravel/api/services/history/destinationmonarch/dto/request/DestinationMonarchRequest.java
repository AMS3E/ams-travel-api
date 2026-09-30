package ams.asmtravel.api.services.history.destinationmonarch.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Every monarch a destination is tied to, replaced whole: one left out is untied. */
public record DestinationMonarchRequest(List<@Valid Tie> monarchs) {

    public DestinationMonarchRequest {
        monarchs = monarchs == null ? List.of() : List.copyOf(monarchs);
    }

    /** @param description how this monarch shaped the place; optional */
    public record Tie(@NotNull UUID monarchId, String description) {
    }
}
