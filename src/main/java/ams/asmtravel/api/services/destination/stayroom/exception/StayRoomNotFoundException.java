package ams.asmtravel.api.services.destination.stayroom.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such room — or one on a listing the caller may not see yet. */
public class StayRoomNotFoundException extends ResponseStatusException {

    public StayRoomNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Room not found");
    }
}
