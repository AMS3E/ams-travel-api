package ams.asmtravel.api.services.media.service;

import ams.asmtravel.api.services.media.client.StoredFile;
import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.dto.request.MediaUpdateRequest;
import ams.asmtravel.api.services.media.dto.request.MediaUploadRequest;
import ams.asmtravel.api.services.media.dto.response.MediaResponse;
import ams.asmtravel.api.services.media.dto.response.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Files in MinIO and the galleries that list them. Rows keep keys; {@link #url} turns a key into a link. */
public interface MediaService {

    // Links and files

    /** The link for a stored key ({@code media.base-uri} + key); null when there is no key. */
    String url(String key);

    /** One file; its type is read from its bytes and it is named by a new UUID. */
    MediaUploadResponse upload(MultipartFile file, MediaType type, UUID uploaderId);

    /** 1–10 files, all checked before any is stored; results follow the input order. */
    List<MediaUploadResponse> uploadMultiple(List<MultipartFile> files, MediaType type, UUID uploaderId);

    /** The file behind a link; 404 when there is none. */
    StoredFile readFile(String folder, String uploader, String fileName);

    /** Deletes a file; the uploader or full control only. Rows keeping the key show no picture. */
    void deleteFile(String folder, String uploader, String fileName, UUID requesterId);

    /** Whether this key is a stored file of that type. */
    boolean isStored(String key, MediaType type);

    // Galleries

    /** One owner type's items, or one owner's; hidden items and unpublished owners only for the back office. */
    List<MediaResponse> getAll(MediaType ownerType, UUID ownerId);

    MediaResponse findById(UUID id);

    /** Stores 1–10 files (or takes an earlier upload's key) and adds one gallery photo each; needs the right to edit the owner. */
    List<MediaResponse> create(MediaUploadRequest request, List<MultipartFile> files, UUID uploaderId);

    /** Caption, order, cover and status; a hidden item stops being the cover. */
    MediaResponse update(UUID id, MediaUpdateRequest request);

    void delete(UUID id);

    // Used by the review service.

    /** These reviews' photos, each review's in order; a review with none is left out. */
    Map<UUID, List<MediaResponse>> findByReviews(Collection<UUID> reviewIds);

    /** Makes the review's photos exactly these keys, in this order. */
    void replaceReviewPhotos(UUID reviewId, List<String> photoKeys);

    /** Removes these reviews' photos, before the reviews themselves go. */
    void deleteByReviews(Collection<UUID> reviewIds);
}
