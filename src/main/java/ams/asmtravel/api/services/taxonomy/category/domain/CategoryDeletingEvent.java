package ams.asmtravel.api.services.taxonomy.category.domain;

import java.util.UUID;

/** Published before a category is deleted, in the same transaction; destinations and food drop their links. */
public record CategoryDeletingEvent(UUID categoryId) {
}
