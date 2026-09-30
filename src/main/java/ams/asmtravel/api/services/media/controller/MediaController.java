package ams.asmtravel.api.services.media.controller;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.authorization.domain.Permissions;
import ams.asmtravel.api.services.media.client.StorageKey;
import ams.asmtravel.api.services.media.client.StoredFile;
import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.dto.request.MediaUpdateRequest;
import ams.asmtravel.api.services.media.dto.request.MediaUploadRequest;
import ams.asmtravel.api.services.media.dto.response.MediaResponse;
import ams.asmtravel.api.services.media.dto.response.MediaUploadResponse;
import ams.asmtravel.api.services.media.exception.InvalidMediaException;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.user.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Files ({@code /files/...}, what every xxxUrl points at) and the galleries that list them. */
@Tag(name = "Media", description = "Uploads, file links, and the galleries of destinations, stays and food listings.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;
    private final UserService userService;

    // Files

    @PostMapping(value = "/files", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaUploadResponse upload(Authentication authentication,
                                      @RequestPart MultipartFile file,
                                      @RequestParam(defaultValue = "general") String type) {
        UUID uploaderId = userService.requireUserId(authentication);
        MediaType mediaType = uploadType(type);
        checkUploadPermission(authentication, mediaType);
        return mediaService.upload(file, mediaType, uploaderId);
    }

    @PostMapping(value = "/files/batch", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<MediaUploadResponse> uploadMultiple(Authentication authentication,
                                                    @RequestPart List<MultipartFile> files,
                                                    @RequestParam(defaultValue = "general") String type) {
        UUID uploaderId = userService.requireUserId(authentication);
        MediaType mediaType = uploadType(type);
        checkUploadPermission(authentication, mediaType);
        return mediaService.uploadMultiple(files, mediaType, uploaderId);
    }

    /** Public and cached for a year, except verification evidence, which is also checked here. */
    @GetMapping("/files/{folder}/{uploader}/{fileName}")
    public ResponseEntity<StreamingResponseBody> readFile(Authentication authentication,
                                                          @PathVariable String folder,
                                                          @PathVariable String uploader,
                                                          @PathVariable String fileName) {
        boolean isPrivate = StorageKey.typeOf(folder).isPrivate();
        if (isPrivate) {
            checkPrivateRead(authentication);
        }
        StoredFile file = mediaService.readFile(folder, uploader, fileName);
        StreamingResponseBody body = output -> {
            try (var input = file.content()) {
                input.transferTo(output);
            }
        };
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(file.contentType()))
                .contentLength(file.size())
                .cacheControl(isPrivate
                        ? CacheControl.noStore().cachePrivate()
                        : CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(body);
    }

    @DeleteMapping("/files/{folder}/{uploader}/{fileName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(Authentication authentication,
                           @PathVariable String folder,
                           @PathVariable String uploader,
                           @PathVariable String fileName) {
        mediaService.deleteFile(folder, uploader, fileName, userService.requireUserId(authentication));
    }

    // Galleries

    @GetMapping
    public List<MediaResponse> getAll(@RequestParam MediaType ownerType, @RequestParam(required = false) UUID ownerId) {
        return mediaService.getAll(ownerType, ownerId);
    }

    @GetMapping("/{id}")
    public MediaResponse getById(@PathVariable UUID id) {
        return mediaService.findById(id);
    }

    /** Uploads 1–10 files (repeated part {@code files}) and adds each to the owner's gallery, in order. */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<MediaResponse> upload(Authentication authentication,
                                      @RequestPart List<MultipartFile> files,
                                      @Valid @ModelAttribute MediaUploadRequest request) {
        return mediaService.create(request, files, userService.requireUserId(authentication));
    }

    /** The same without files: {@code mediaKey} names an earlier upload. */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public MediaResponse create(Authentication authentication, @Valid @RequestBody MediaUploadRequest request) {
        return mediaService.create(request, List.of(), userService.requireUserId(authentication)).getFirst();
    }

    @PutMapping("/{id}")
    public MediaResponse update(@PathVariable UUID id, @Valid @RequestBody MediaUpdateRequest request) {
        return mediaService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        mediaService.delete(id);
    }

    /** The upload form's {@code type}, any case; blank means GENERAL, unknown is 400. */
    private static MediaType uploadType(String type) {
        if (type == null || type.isBlank()) {
            return MediaType.GENERAL;
        }
        try {
            return MediaType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidMediaException("Unsupported media type: " + type);
        }
    }

    private static void checkPrivateRead(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to read verification evidence");
        }
        if (!Authorities.hasAny(authentication, Permissions.DESTINATION_VERIFY, Permissions.FULL_CONTROL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the verifying desks read verification evidence");
        }
    }

    /** Personal pictures: anyone signed in; catalogue media and evidence need a permission. */
    private static void checkUploadPermission(Authentication authentication, MediaType type) {
        if (type.isPersonal()) {
            return;
        }
        String needed = type.isPrivate() ? Permissions.DESTINATION_VERIFY : Permissions.MEDIA_UPLOAD;
        boolean granted = authentication.getAuthorities().stream()
                .anyMatch(grant -> needed.equals(grant.getAuthority())
                        || Permissions.FULL_CONTROL.equals(grant.getAuthority()));
        if (!granted) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, type.isPrivate()
                    ? "This account cannot upload verification evidence"
                    : "This account cannot upload catalogue media");
        }
    }
}
