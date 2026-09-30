package ams.asmtravel.api.services.media.client;

import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.exception.InvalidMediaException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** A file's real type, read from its first bytes: the client's Content-Type and extension cannot be trusted. */
public record FileFormat(String contentType, String extension) {

    private static final String UNSUPPORTED = "Only JPEG, PNG, WebP, GIF, and AVIF images are supported";
    private static final String UNSUPPORTED_EVIDENCE = "Evidence must be a PDF or a JPEG, PNG, WebP, GIF, or AVIF image";

    /** As {@link #detect(byte[])}; a PDF is also accepted for private (evidence) files. */
    public static FileFormat detect(byte[] content, MediaType type) {
        if (!type.isPrivate()) {
            return detect(content);
        }
        if (startsWithText(content, 0, "%PDF-")) {
            return new FileFormat("application/pdf", "pdf");
        }
        try {
            return detect(content);
        } catch (InvalidMediaException notAnImage) {
            throw new InvalidMediaException(UNSUPPORTED_EVIDENCE);
        }
    }

    /** 400 when the bytes are not one of the five accepted image formats. */
    public static FileFormat detect(byte[] content) {
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return new FileFormat("image/jpeg", "jpg");
        }
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return new FileFormat("image/png", "png");
        }
        if (startsWithText(content, 0, "GIF87a") || startsWithText(content, 0, "GIF89a")) {
            return new FileFormat("image/gif", "gif");
        }
        if (startsWithText(content, 0, "RIFF") && startsWithText(content, 8, "WEBP")) {
            return new FileFormat("image/webp", "webp");
        }
        // AVIF carries its brand in the ISO-BMFF box, not at offset zero.
        if (startsWithText(content, 4, "ftyp") && (startsWithText(content, 8, "avif") || startsWithText(content, 8, "avis"))) {
            return new FileFormat("image/avif", "avif");
        }
        throw new InvalidMediaException(UNSUPPORTED);
    }

    private static boolean startsWith(byte[] content, int... expected) {
        if (content == null || content.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((content[index] & 0xFF) != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWithText(byte[] content, int offset, String expected) {
        byte[] bytes = expected.getBytes(StandardCharsets.US_ASCII);
        return content != null && content.length >= offset + bytes.length
                && Arrays.equals(content, offset, offset + bytes.length, bytes, 0, bytes.length);
    }
}
