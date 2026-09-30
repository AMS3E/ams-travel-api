package ams.asmtravel.api.services.media.client;

import ams.asmtravel.api.services.media.config.MinioProperties;
import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.exception.MediaNotFoundException;
import ams.asmtravel.api.services.media.exception.MediaStorageException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/** Every call to MinIO. A missing object is 404; MinIO failing or refusing is 503. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageClient {

    private static final Set<String> NOT_FOUND_CODES = Set.of("NoSuchKey", "NoSuchObject", "NoSuchBucket");

    private final MinioClient minio;
    private final MinioProperties properties;

    private final Object bucketLock = new Object();
    private volatile boolean bucketReady;

    public void put(StorageKey key, byte[] content, String contentType) {
        put(key.value(), content, contentType);
    }

    public StoredFile read(StorageKey key) {
        ensureBucket();
        try {
            StatObjectResponse stat = minio.statObject(StatObjectArgs.builder().bucket(bucket()).object(key.value()).build());
            return new StoredFile(
                    minio.getObject(GetObjectArgs.builder().bucket(bucket()).object(key.value()).build()),
                    stat.contentType(), stat.size(), key.fileName());
        } catch (ErrorResponseException exception) {
            if (isNotFound(exception)) {
                throw new MediaNotFoundException();
            }
            throw unavailable("Media storage rejected the request", exception);
        } catch (MinioException | IllegalStateException exception) {
            throw unavailable("Unable to read media", exception);
        }
    }

    public boolean exists(StorageKey key) {
        return exists(key.value());
    }

    /** Removing a file that is not there is not an error. */
    public void remove(String objectKey) {
        ensureBucket();
        try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(bucket()).object(objectKey).build());
        } catch (MinioException | IllegalStateException exception) {
            throw unavailable("Unable to delete media", exception);
        }
    }

    /** Best-effort cleanup after a failure: whatever cannot be removed is attached to that failure. */
    public void removeAll(Collection<String> objectKeys, RuntimeException failure) {
        for (String objectKey : objectKeys) {
            try {
                remove(objectKey);
            } catch (MediaStorageException cleanup) {
                failure.addSuppressed(cleanup);
            }
        }
    }

    /** One zero-byte folder marker per type, so consoles show the folders; returns how many it made. */
    public int ensureFolders() {
        int created = 0;
        for (MediaType type : MediaType.values()) {
            String marker = type.folder() + "/";
            if (!exists(marker)) {
                put(marker, new byte[0], "application/x-directory");
                created += 1;
            }
        }
        return created;
    }

    private boolean exists(String objectKey) {
        ensureBucket();
        try {
            minio.statObject(StatObjectArgs.builder().bucket(bucket()).object(objectKey).build());
            return true;
        } catch (ErrorResponseException exception) {
            // Only a real "not found" means absent; access errors must not pass as absence.
            if (isNotFound(exception)) {
                return false;
            }
            throw unavailable("Media storage rejected the request", exception);
        } catch (MinioException | IllegalStateException exception) {
            throw unavailable("Unable to read media", exception);
        }
    }

    private void put(String objectKey, byte[] content, String contentType) {
        ensureBucket();
        try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(bucket())
                    .object(objectKey)
                    .stream(input, (long) content.length, null)
                    .contentType(contentType)
                    .build());
        } catch (MinioException | IllegalStateException | IOException exception) {
            throw unavailable("Unable to store media", exception);
        }
    }

    /** Creates the bucket if it is missing, once per process. */
    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (bucketLock) {
            if (bucketReady) {
                return;
            }
            try {
                if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket()).build())) {
                    minio.makeBucket(MakeBucketArgs.builder().bucket(bucket()).build());
                    log.info("MinIO bucket created: {}", bucket());
                }
                bucketReady = true;
            } catch (MinioException | IllegalStateException exception) {
                throw unavailable("Unable to initialize media storage", exception);
            }
        }
    }

    private String bucket() {
        return properties.bucket();
    }

    private static boolean isNotFound(ErrorResponseException exception) {
        return exception.errorResponse() != null && NOT_FOUND_CODES.contains(exception.errorResponse().code());
    }

    /** MinIO's synchronous client wraps transport failures in IllegalStateException. */
    private static MediaStorageException unavailable(String message, Exception exception) {
        log.error("{}: {}", message, exception.getMessage());
        return new MediaStorageException(message, exception);
    }
}
