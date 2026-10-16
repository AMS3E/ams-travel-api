package ams.asmtravel.api.services.destination.destination.domain;

import ams.asmtravel.api.services.destination.destination.dto.response.DestinationResponse;

/** A published place around a point, and how far it is from that point in metres. */
public record NearbyDestination(DestinationResponse place, double metres) {
}
