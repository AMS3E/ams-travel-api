package ams.asmtravel.api.services.taxonomy.tag.domain;

import java.util.UUID;

/** Published before a tag is deleted, in the same transaction; destinations and food drop their links. */
public record TagDeletingEvent(UUID tagId) {
}
