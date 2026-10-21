package ams.asmtravel.api.services.media.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.authorization.domain.Permissions;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.activity.domain.ActivityDeletingEvent;
import ams.asmtravel.api.services.destination.activity.service.ActivityService;
import ams.asmtravel.api.services.destination.food.domain.FoodDeletingEvent;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.domain.StayDeletingEvent;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.media.client.FileFormat;
import ams.asmtravel.api.services.media.client.MinioStorageClient;
import ams.asmtravel.api.services.media.client.StorageKey;
import ams.asmtravel.api.services.media.client.StoredFile;
import ams.asmtravel.api.services.media.config.MinioProperties;
import ams.asmtravel.api.services.media.domain.Media;
import ams.asmtravel.api.services.media.domain.MediaStatus;
import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.dto.request.MediaUpdateRequest;
import ams.asmtravel.api.services.media.dto.request.MediaUploadRequest;
import ams.asmtravel.api.services.media.dto.response.MediaResponse;
import ams.asmtravel.api.services.media.dto.response.MediaUploadResponse;
import ams.asmtravel.api.services.media.exception.InvalidMediaException;
import ams.asmtravel.api.services.media.exception.MediaNotFoundException;
import ams.asmtravel.api.services.media.exception.MediaStorageException;
import ams.asmtravel.api.services.media.mapper.MediaMapper;
import ams.asmtravel.api.services.media.repository.MediaRepository;
import ams.asmtravel.api.services.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Upload rules and gallery rules; the bytes themselves go through {@link MinioStorageClient}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private static final int MAX_BATCH_FILES = 10;

    private final MinioStorageClient storage;
    private final MinioProperties properties;
    private final MediaRepository mediaRepository;
    private final MediaMapper mapper;
    // Providers, because the owners' mappers use this service to build links.
    private final ObjectProvider<DestinationService> destinationService;
    private final ObjectProvider<StayService> stayService;
    private final ObjectProvider<FoodService> foodService;
    private final ObjectProvider<ActivityService> activityService;
    private final ObjectProvider<CorridorService> corridorService;

    // Files

    @Override
    public String url(String key) {
        return key == null || key.isBlank() ? null : properties.baseUri() + "/" + key;
    }

    @Override
    public MediaUploadResponse upload(MultipartFile file, MediaType type, UUID uploaderId) {
        return uploadMultiple(List.of(file), type, uploaderId).getFirst();
    }

    @Override
    public List<MediaUploadResponse> uploadMultiple(List<MultipartFile> files, MediaType type, UUID uploaderId) {
        if (files == null || files.isEmpty()) {
            throw new InvalidMediaException("At least one image file is required");
        }
        if (files.size() > MAX_BATCH_FILES) {
            throw new InvalidMediaException("A maximum of " + MAX_BATCH_FILES + " images can be uploaded at once");
        }
        // Every file is checked before the first one is stored.
        List<Upload> uploads = files.stream().map(file -> prepare(file, type, uploaderId)).toList();
        List<String> stored = new ArrayList<>();
        try {
            for (Upload upload : uploads) {
                // A failed answer may still mean the object was stored, so it is cleaned up too.
                stored.add(upload.key().value());
                storage.put(upload.key(), upload.content(), upload.format().contentType());
            }
        } catch (MediaStorageException exception) {
            storage.removeAll(stored, exception);
            throw exception;
        }
        log.info("Stored {} file(s) in {}", uploads.size(), type.folder());
        return uploads.stream().map(this::toUploadResponse).toList();
    }

    @Override
    public StoredFile readFile(String folder, String uploader, String fileName) {
        return storage.read(StorageKey.parse(folder, uploader, fileName));
    }

    /** The uploader may delete their file, and full control anyone's. */
    @Override
    public void deleteFile(String folder, String uploader, String fileName, UUID requesterId) {
        StorageKey key = StorageKey.parse(folder, uploader, fileName);
        if (!key.uploader().equals(requesterId) && !Authorities.hasAny(Authorities.current(), Permissions.FULL_CONTROL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete media owned by another user");
        }
        storage.remove(key.value());
        log.info("Media {} deleted", key);
    }

    @Override
    public boolean isStored(String key, MediaType type) {
        String[] parts = key == null ? new String[0] : key.split("/", -1);
        if (parts.length != 3 || !parts[0].equals(type.folder())) {
            return false;
        }
        try {
            return storage.exists(StorageKey.parse(parts[0], parts[1], parts[2]));
        } catch (ResponseStatusException notAKey) {
            return false;
        }
    }

    /** Stores a seed photo under its own name in {@code general/}; skips it if already there. */
    public String storeSeedImage(String fileName, byte[] content) {
        StorageKey key = new StorageKey(MediaType.GENERAL, StorageKey.SYSTEM, fileName);
        if (!storage.exists(key)) {
            storage.put(key, content, FileFormat.detect(content).contentType());
        }
        return key.value();
    }

    // Galleries

    @Override
    @Transactional(readOnly = true)
    public List<MediaResponse> getAll(MediaType ownerType, UUID ownerId) {
        Predicate<Media> visible = visible(ownerType);
        List<Media> items = ownerId == null
                ? mediaRepository.findByOwnerTypeOrderBySortOrderAscCreatedAtAsc(ownerType)
                : mediaRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(ownerType, ownerId);
        return items.stream().filter(visible).map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MediaResponse findById(UUID id) {
        Media item = find(id);
        if (!item.getOwnerType().hasGallery() || !visible(item.getOwnerType()).test(item)) {
            throw new MediaNotFoundException();
        }
        return toResponse(item);
    }

    @Override
    @Transactional
    public List<MediaResponse> create(MediaUploadRequest request, List<MultipartFile> files, UUID uploaderId) {
        editable(request.ownerType(), request.ownerId());
        List<MultipartFile> sent = files == null ? List.of()
                : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        List<String> keys = sent.isEmpty()
                ? List.of(earlierUpload(request))
                : uploadMultiple(sent, request.ownerType(), uploaderId).stream().map(MediaUploadResponse::key).toList();
        int first = request.sortOrder() != null
                ? request.sortOrder()
                : mediaRepository.findMaxSortOrder(request.ownerType(), request.ownerId()) + 1;
        try {
            List<Media> created = new ArrayList<>();
            for (int index = 0; index < keys.size(); index++) {
                Media item = new Media(request.ownerType(), request.ownerId(), keys.get(index), first + index);
                item.setCaption(clean(request.caption()));
                item.setIsCover(index == 0 && Boolean.TRUE.equals(request.isCover()));
                created.add(mediaRepository.save(item));
            }
            syncCover(created.getFirst());
            return created.stream().map(this::toResponse).toList();
        } catch (RuntimeException exception) {
            // The rows did not make it, so the files stored for them go too.
            if (!sent.isEmpty()) {
                storage.removeAll(keys, exception);
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public MediaResponse update(UUID id, MediaUpdateRequest request) {
        Media item = find(id);
        editable(item.getOwnerType(), item.getOwnerId());
        if (request.caption() != null) {
            item.setCaption(clean(request.caption()));
        }
        if (request.sortOrder() != null) {
            item.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            item.setStatus(request.status());
        }
        if (request.isCover() != null) {
            item.setIsCover(request.isCover());
        }
        if (item.getStatus() == MediaStatus.HIDDEN && Boolean.TRUE.equals(item.getIsCover())) {
            if (Boolean.TRUE.equals(request.isCover())) {
                throw new InvalidMediaException("A hidden photo cannot be the cover");
            }
            item.setIsCover(false);
            clearCoverIfItIs(item);
        }
        syncCover(item);
        return toResponse(item);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Media item = find(id);
        editable(item.getOwnerType(), item.getOwnerId());
        if (Boolean.TRUE.equals(item.getIsCover())) {
            clearCoverIfItIs(item);
        }
        mediaRepository.delete(item);
    }

    // Review photos

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, List<MediaResponse>> findByReviews(Collection<UUID> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return Map.of();
        }
        return mediaRepository.findByOwnerTypeAndOwnerIdInOrderBySortOrderAscCreatedAtAsc(MediaType.REVIEW, Set.copyOf(reviewIds))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.groupingBy(MediaResponse::ownerId, LinkedHashMap::new, Collectors.toList()));
    }

    @Override
    @Transactional
    public void replaceReviewPhotos(UUID reviewId, List<String> photoKeys) {
        mediaRepository.deleteByOwners(MediaType.REVIEW, List.of(reviewId));
        mediaRepository.flush();
        for (int index = 0; index < photoKeys.size(); index++) {
            mediaRepository.save(new Media(MediaType.REVIEW, reviewId, photoKeys.get(index), index + 1));
        }
        mediaRepository.flush();
    }

    @Override
    @Transactional
    public void deleteByReviews(Collection<UUID> reviewIds) {
        if (reviewIds != null && !reviewIds.isEmpty()) {
            mediaRepository.deleteByOwners(MediaType.REVIEW, Set.copyOf(reviewIds));
        }
    }

    // An owner's photos go with it.

    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        mediaRepository.deleteByOwners(MediaType.DESTINATION, List.of(event.destinationId()));
    }

    @EventListener
    public void onStayDeleting(StayDeletingEvent event) {
        mediaRepository.deleteByOwners(MediaType.STAY, List.of(event.stayId()));
    }

    @EventListener
    public void onFoodDeleting(FoodDeletingEvent event) {
        mediaRepository.deleteByOwners(MediaType.FOOD, List.of(event.foodId()));
    }

    @EventListener
    public void onActivityDeleting(ActivityDeletingEvent event) {
        mediaRepository.deleteByOwners(MediaType.ACTIVITY, List.of(event.activityId()));
    }

    // Helpers

    private record Upload(StorageKey key, byte[] content, FileFormat format) {
    }

    /** Checks size and real type; nothing is stored yet. */
    private Upload prepare(MultipartFile file, MediaType type, UUID uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidMediaException("Media file is required");
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new InvalidMediaException("Media exceeds the maximum size of " + properties.maxFileSize());
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new InvalidMediaException("Unable to read the uploaded file");
        }
        FileFormat format = FileFormat.detect(content, type);
        return new Upload(StorageKey.generate(type, uploaderId, format), content, format);
    }

    private MediaUploadResponse toUploadResponse(Upload upload) {
        StorageKey key = upload.key();
        return MediaUploadResponse.builder()
                .key(key.value())
                .uri(url(key.value()))
                .name(key.name())
                .extension(key.extension())
                .mimeTypeFile(upload.format().contentType())
                .size((long) upload.content().length)
                .category(key.type().folder())
                .build();
    }

    private static String earlierUpload(MediaUploadRequest request) {
        if (request.mediaKey() == null || request.mediaKey().isBlank()) {
            throw new InvalidMediaException("Send one or more files, or the mediaKey of an earlier upload");
        }
        return request.mediaKey().trim();
    }

    /** What a gallery needs from its owner. */
    private record Owner(
            Predicate<UUID> exists,
            BiPredicate<UUID, Authentication> canEdit,
            Predicate<UUID> isPublished,
            Function<UUID, String> coverKey,
            BiConsumer<UUID, String> setCover
    ) {
    }

    /** The one place that knows destination, stay, food and activity. */
    private Owner owner(MediaType type) {
        return switch (type) {
            case DESTINATION -> {
                DestinationService owners = destinationService.getObject();
                yield new Owner(owners::exists, owners::canEdit, owners::isPublished,
                        id -> owners.getEntity(id).getCoverKey(), owners::setCover);
            }
            case STAY -> {
                StayService owners = stayService.getObject();
                yield new Owner(owners::exists, owners::canWrite, owners::isPublished,
                        id -> owners.getEntity(id).getCoverKey(), owners::setCover);
            }
            case FOOD -> {
                FoodService owners = foodService.getObject();
                yield new Owner(owners::exists, owners::canWrite, owners::isPublished,
                        id -> owners.getEntity(id).getCoverKey(), owners::setCover);
            }
            case ACTIVITY -> {
                ActivityService owners = activityService.getObject();
                yield new Owner(owners::exists, owners::canWrite, owners::isPublished,
                        id -> owners.getEntity(id).getCoverKey(), owners::setCover);
            }
            case CORRIDOR -> {
                CorridorService owners = corridorService.getObject();
                yield new Owner(owners::exists, owners::canWrite, owners::isPublished,
                        id -> owners.getEntity(id).getCoverKey(), owners::setCover);
            }
            default -> throw new InvalidMediaException(
                    "Galleries are DESTINATION, STAY, FOOD, ACTIVITY and CORRIDOR; review photos come with their review");
        };
    }

    /** 400 when the owner does not exist, 403 when the caller may not edit it. */
    private void editable(MediaType ownerType, UUID ownerId) {
        Owner owner = owner(ownerType);
        if (!owner.exists().test(ownerId)) {
            throw new InvalidMediaException("No such " + ownerType.folder());
        }
        if (!owner.canEdit().test(ownerId, Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change this " + ownerType.folder() + "'s photos");
        }
    }

    /** Back office sees everything; others only ACTIVE photos of published owners (checked once per owner). */
    private Predicate<Media> visible(MediaType ownerType) {
        Owner owner = owner(ownerType);
        if (Authorities.isBackoffice(Authorities.current())) {
            return item -> true;
        }
        Map<UUID, Boolean> published = new HashMap<>();
        return item -> item.getStatus() == MediaStatus.ACTIVE
                && published.computeIfAbsent(item.getOwnerId(), owner.isPublished()::test);
    }

    /** One cover per owner, and it is the owner's cover too. */
    private void syncCover(Media item) {
        if (!Boolean.TRUE.equals(item.getIsCover())) {
            return;
        }
        mediaRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(item.getOwnerType(), item.getOwnerId())
                .stream()
                .filter(other -> !other.getId().equals(item.getId()))
                .forEach(other -> other.setIsCover(false));
        owner(item.getOwnerType()).setCover().accept(item.getOwnerId(), item.getMediaKey());
    }

    /** Clears the owner's cover only if it is still this photo; it can be set on the listing directly. */
    private void clearCoverIfItIs(Media item) {
        Owner owner = owner(item.getOwnerType());
        if (item.getMediaKey().equals(owner.coverKey().apply(item.getOwnerId()))) {
            owner.setCover().accept(item.getOwnerId(), null);
        }
    }

    private MediaResponse toResponse(Media item) {
        return mapper.toResponse(item, url(item.getMediaKey()));
    }

    private Media find(UUID id) {
        return mediaRepository.findById(id).orElseThrow(MediaNotFoundException::new);
    }

    private static String clean(String caption) {
        return caption == null || caption.isBlank() ? null : caption.trim();
    }
}
