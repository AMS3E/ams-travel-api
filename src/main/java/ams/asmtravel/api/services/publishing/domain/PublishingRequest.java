package ams.asmtravel.api.services.publishing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** One trip of a listing through the workflow; a listing's requests, newest first, are its publishing history. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "publishing", name = "publish_requests")
public class PublishingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublishingTargetType targetType;

    /** No foreign key: the listing's deleting event removes its requests. */
    @Column(nullable = false)
    private UUID targetId;

    /** The listing's name when it was last put forward, for queues and notifications. */
    @Column(nullable = false)
    private String targetName;

    /** Whoever put it forward last. */
    @Column(nullable = false)
    private UUID requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PublishingStatus status = PublishingStatus.PENDING_REVIEW;

    private UUID reviewerId;

    @Column(columnDefinition = "text")
    private String reviewNote;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant reviewedAt;

    private Instant publishedAt;

    private Instant unpublishedAt;
}
