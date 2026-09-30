package ams.asmtravel.api.services.point.mapper;

import ams.asmtravel.api.services.point.domain.PointTransaction;
import ams.asmtravel.api.services.point.dto.response.PointTransactionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PointMapper {

    PointTransactionResponse toResponse(PointTransaction transaction);
}
