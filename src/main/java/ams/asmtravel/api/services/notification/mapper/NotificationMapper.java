package ams.asmtravel.api.services.notification.mapper;

import ams.asmtravel.api.services.notification.domain.Notification;
import ams.asmtravel.api.services.notification.dto.response.NotificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
