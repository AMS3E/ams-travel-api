package ams.asmtravel.api.services.destination.destination.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** Publishing refused while English text has no Khmer beside it (409); {@code missingKhmer} names the fields. */
public class KhmerTextMissingException extends ResponseStatusException {

    private static final Map<String, String> WORDS = Map.of(
            "nameKm", "name",
            "descriptionKm", "description",
            "sloganKm", "slogan",
            "sloganByKm", "slogan's author",
            "addressKm", "address");

    public KhmerTextMissingException(List<String> missing) {
        super(HttpStatus.CONFLICT, "Write the Khmer text before publishing: "
                + String.join(", ", missing.stream().map(field -> WORDS.getOrDefault(field, field)).toList()));
        getBody().setProperty("missingKhmer", missing);
    }
}
