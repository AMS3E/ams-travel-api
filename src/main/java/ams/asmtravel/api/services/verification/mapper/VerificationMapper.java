package ams.asmtravel.api.services.verification.mapper;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.publishing.service.PublishingService.Desk;
import ams.asmtravel.api.services.verification.domain.DestinationVerification;
import ams.asmtravel.api.services.verification.dto.response.VerificationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class VerificationMapper {

    @Autowired
    protected MediaService mediaService;

    /**
     * @param desk          the place's desk for the queue; null in a history
     * @param decidedByName null if the decider no longer exists
     */
    @Mapping(target = "id", source = "decision.id")
    @Mapping(target = "destinationId", source = "decision.destination.id")
    @Mapping(target = "destinationName", source = "decision.destination.name")
    @Mapping(target = "destinationSlug", source = "decision.destination.slug")
    @Mapping(target = "destinationCoverUrl", expression = "java(mediaService.url(decision.getDestination().getCoverKey()))")
    @Mapping(target = "destinationStatus", source = "decision.destination.status")
    @Mapping(target = "desk", source = "desk")
    @Mapping(target = "verified", source = "decision.verified")
    @Mapping(target = "reference", source = "decision.reference")
    @Mapping(target = "note", source = "decision.note")
    @Mapping(target = "evidenceKey", source = "decision.evidenceKey")
    @Mapping(target = "evidenceUrl", expression = "java(mediaService.url(decision.getEvidenceKey()))")
    @Mapping(target = "decidedBy", source = "decision.decidedBy")
    @Mapping(target = "decidedByName", source = "decidedByName")
    @Mapping(target = "createdAt", source = "decision.createdAt")
    public abstract VerificationResponse toResponse(DestinationVerification decision, Desk desk, String decidedByName);

    /** Queue row for a place with no decision yet. */
    public VerificationResponse undecided(DestinationSummary place, Desk desk) {
        return new VerificationResponse(null, place.id(), place.name(), place.slug(), mediaService.url(place.coverKey()),
                place.status(), desk, false, null, null, null, null, null, null, null);
    }

    /** Public status only: verified and since when. */
    public VerificationResponse publicStatus(UUID destinationId, DestinationVerification latest) {
        boolean verified = latest != null && Boolean.TRUE.equals(latest.getVerified());
        return new VerificationResponse(null, destinationId, null, null, null, null, null, verified,
                null, null, null, null, null, null, latest == null ? null : latest.getCreatedAt());
    }
}
