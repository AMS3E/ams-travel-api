package ams.asmtravel.api.services.review.review.mapper;

import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.review.review.domain.Review;
import ams.asmtravel.api.services.media.dto.response.MediaResponse;
import ams.asmtravel.api.services.review.review.dto.response.ReviewResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ReviewMapper {

    @Autowired
    protected MediaService mediaService;

    /** Author, photos and stars come from their own services; any may be null. */
    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "userId", source = "review.userId")
    @Mapping(target = "authorName", source = "author.fullName")
    @Mapping(target = "authorAvatarUrl", expression = "java(author == null ? null : mediaService.url(author.avatarKey()))")
    @Mapping(target = "photos", source = "photos")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "createdAt", source = "review.createdAt")
    @Mapping(target = "updatedAt", source = "review.updatedAt")
    public abstract ReviewResponse toResponse(Review review, UserSummary author, List<MediaResponse> photos, Integer rating);
}
