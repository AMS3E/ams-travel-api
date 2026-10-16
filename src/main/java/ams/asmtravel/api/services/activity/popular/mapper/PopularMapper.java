package ams.asmtravel.api.services.activity.popular.mapper;

import ams.asmtravel.api.services.activity.popular.domain.BrowseEvent;
import ams.asmtravel.api.services.activity.popular.domain.BrowseTarget;
import ams.asmtravel.api.services.activity.popular.domain.PopularItem;
import ams.asmtravel.api.services.activity.popular.dto.request.BrowseEventRequest;
import ams.asmtravel.api.services.activity.popular.dto.response.PopularResponse;
import org.mapstruct.Mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PopularMapper {

    default PopularResponse.Item toItem(PopularItem item) {
        return new PopularResponse.Item(item.type(), item.id(), item.browses(), item.reason());
    }

    default PopularResponse toResponse(int days, UUID regionId, BrowseTarget type, List<PopularItem> items) {
        return new PopularResponse(days, regionId, type, items.stream().map(this::toItem).toList(),
                type == BrowseTarget.DESTINATION
                        ? items.stream().map(item -> new PopularResponse.Place(item.id(), item.browses(), item.reason())).toList()
                        : List.of());
    }

    default BrowseEvent toEvent(BrowseEventRequest request, UUID userId) {
        BrowseEvent event = new BrowseEvent();
        event.setUserId(userId);
        event.setTargetType(request.resolvedType());
        event.setTargetId(request.resolvedTarget());
        event.setEventType(request.eventType());
        event.setSessionId(request.sessionId());
        event.setMetadata(request.metadata() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(request.metadata()));
        return event;
    }
}
