package ams.asmtravel.api.services.media.client;

import java.io.InputStream;

/** A file read back from storage; the caller closes {@code content}. */
public record StoredFile(InputStream content, String contentType, long size, String fileName) {
}
