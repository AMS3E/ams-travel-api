package ams.asmtravel.api.services.media.client;

import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.exception.InvalidMediaException;
import ams.asmtravel.api.services.media.exception.MediaNotFoundException;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Where a file lives in the bucket: {@code folder/uploaderUuid/file.ext}.
 * Every key is built here, and the file-name pattern blocks {@code ../} tricks.
 *
 * @param uploader who uploaded it, and so who may delete it
 */
public record StorageKey(MediaType type, UUID uploader, String fileName) {

    /** The uploader of the seed photos (nobody). */
    public static final UUID SYSTEM = new UUID(0L, 0L);

    private static final Pattern FILE_NAME = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*\\.(jpg|png|webp|gif|avif|pdf)$");

    public StorageKey {
        if (type == null || uploader == null) {
            throw new InvalidMediaException("A media type and uploader are required");
        }
        fileName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        // A PDF is evidence, so it lives in the private folder or nowhere.
        if (!FILE_NAME.matcher(fileName).matches() || (fileName.endsWith(".pdf") && !type.isPrivate())) {
            throw new InvalidMediaException("Invalid media file name");
        }
    }

    /** A new, randomly named file. */
    public static StorageKey generate(MediaType type, UUID uploader, FileFormat format) {
        return new StorageKey(type, uploader, UUID.randomUUID() + "." + format.extension());
    }

    /** A key from a link's three parts: 404 for an unknown folder, 400 for anything else wrong. */
    public static StorageKey parse(String folder, String uploader, String fileName) {
        MediaType type = typeOf(folder);
        try {
            return new StorageKey(type, UUID.fromString(uploader), fileName);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidMediaException("Invalid media uploader");
        }
    }

    /** The folder a link names, lower case only (so {@code VERIFICATION/} cannot dodge the rules); 404 otherwise. */
    public static MediaType typeOf(String folder) {
        for (MediaType type : MediaType.values()) {
            if (type.folder().equals(folder)) {
                return type;
            }
        }
        throw new MediaNotFoundException();
    }

    /** The key as MinIO stores it and rows keep it. */
    public String value() {
        return type.folder() + "/" + uploader + "/" + fileName;
    }

    public String name() {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public String extension() {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    @Override
    public String toString() {
        return value();
    }
}
