package ams.asmtravel.api.services.destination.config;

import ams.asmtravel.api.services.destination.destination.domain.DestinationThemes;
import ams.asmtravel.api.services.destination.food.domain.FoodAmenities;
import ams.asmtravel.api.services.destination.food.domain.FoodSpecialties;
import ams.asmtravel.api.services.destination.food.domain.FoodVocabularies;
import ams.asmtravel.api.services.destination.stay.domain.StaySpecialties;
import ams.asmtravel.api.services.destination.stay.domain.StayVocabularies;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Writes the fixed word lists (amenities, cuisines, themes…) into the OpenAPI schemas as allowed values. */
@Configuration(proxyBeanMethods = false)
public class VocabularySchemas {

    /** Schema, then field, to its allowed values. */
    private static final Map<String, Map<String, Collection<String>>> LISTS = Map.of(
            "StayRequest", Map.of(
                    "specialties", StaySpecialties.ALL,
                    "breakfastTypes", StayVocabularies.BREAKFAST_TYPES,
                    "paymentMethods", StayVocabularies.PAYMENT_METHODS,
                    "languagesSpoken", StayVocabularies.LANGUAGES),
            "FoodRequest", Map.ofEntries(
                    Map.entry("amenities", FoodAmenities.ALL),
                    Map.entry("specialties", FoodSpecialties.ALL),
                    Map.entry("cuisines", FoodVocabularies.CUISINES),
                    Map.entry("mealsServed", FoodVocabularies.MEALS),
                    Map.entry("dietary", FoodVocabularies.DIETARY),
                    Map.entry("serviceOptions", FoodVocabularies.SERVICE_OPTIONS),
                    Map.entry("goodFor", FoodVocabularies.GOOD_FOR),
                    Map.entry("menuLanguages", FoodVocabularies.LANGUAGES),
                    Map.entry("deliveryPartners", FoodVocabularies.DELIVERY_PARTNERS),
                    Map.entry("paymentMethods", FoodVocabularies.PAYMENT_METHODS),
                    Map.entry("languagesSpoken", FoodVocabularies.LANGUAGES)),
            "FoodMenuItemRequest", Map.of("labels", FoodVocabularies.DISH_LABELS),
            /* No theme list: any tourism region's code is one. */
            "DestinationRequest", Map.of("specialties", DestinationThemes.ALL));

    @Bean
    OpenApiCustomizer listingVocabularies() {
        return document -> {
            if (document.getComponents() == null || document.getComponents().getSchemas() == null) {
                return;
            }
            LISTS.forEach((schemaName, fields) -> {
                Schema<?> schema = document.getComponents().getSchemas().get(schemaName);
                if (schema == null || schema.getProperties() == null) {
                    return;
                }
                fields.forEach((field, allowed) -> {
                    Schema<Object> property = asSchema(schema.getProperties().get(field));
                    if (property == null) {
                        return;
                    }
                    /* An array keeps the values on its items; a single word on itself. */
                    Schema<Object> holder = property.getItems() == null ? property : asSchema(property.getItems());
                    holder.setEnum(allowed.stream().sorted().map(Object.class::cast).toList());
                });
            });
        };
    }

    /** Swagger schemas are wildcard-typed; the values are strings. */
    @SuppressWarnings("unchecked")
    private static Schema<Object> asSchema(Object property) {
        return property instanceof Schema<?> schema ? (Schema<Object>) schema : null;
    }

    /** For tests. */
    static List<String> documented(String schema) {
        return LISTS.getOrDefault(schema, Map.of()).keySet().stream().sorted().toList();
    }
}
