package ams.asmtravel.api.services.audit.mapper;

import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.audit.domain.AuditLog;
import ams.asmtravel.api.services.audit.dto.response.AuditLogResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    /** @param user who made the change, or null when unknown */
    @Mapping(target = "id", source = "entry.id")
    @Mapping(target = "userId", source = "entry.userId")
    @Mapping(target = "userName", source = "user.fullName")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "action", source = "entry.action")
    @Mapping(target = "entityType", source = "entry.entityType")
    @Mapping(target = "entityId", source = "entry.entityId")
    @Mapping(target = "oldData", source = "entry.oldData")
    @Mapping(target = "newData", source = "entry.newData")
    @Mapping(target = "ipAddress", source = "entry.ipAddress")
    @Mapping(target = "userAgent", source = "entry.userAgent")
    @Mapping(target = "createdAt", source = "entry.createdAt")
    AuditLogResponse toResponse(AuditLog entry, UserSummary user);
}
