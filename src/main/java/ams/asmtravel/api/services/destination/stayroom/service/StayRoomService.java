package ams.asmtravel.api.services.destination.stayroom.service;

import ams.asmtravel.api.services.destination.stayroom.dto.request.StayQuoteRequest;
import ams.asmtravel.api.services.destination.stayroom.dto.response.RoomQuoteResponse;
import ams.asmtravel.api.services.destination.stayroom.dto.response.StayQuoteResponse;
import ams.asmtravel.api.services.destination.stayroom.dto.request.StayRoomRequest;
import ams.asmtravel.api.services.destination.stayroom.dto.response.StayRoomResponse;

import java.util.List;
import java.util.UUID;

/** A stay's rooms. Stay writers write them; the cheapest active room sets the "from" price. */
public interface StayRoomService {

    /** In order. The public sees active rooms of published stays; the back office sees all. */
    List<StayRoomResponse> getAll(UUID stayId);

    StayRoomResponse findById(UUID id);

    StayRoomResponse create(StayRoomRequest request);

    StayRoomResponse update(UUID id, StayRoomRequest request);

    void delete(UUID id);

    /** A price preview: every room on sale at the stay, priced for the dates and guests; the dates default to tonight. */
    StayQuoteResponse quote(StayQuoteRequest request);

    /** One room, priced the same way. */
    RoomQuoteResponse quoteRoom(UUID roomId, StayQuoteRequest request);
}
