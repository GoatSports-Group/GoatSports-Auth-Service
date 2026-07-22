package iuh.fit.auth.presentation.converter;

import iuh.fit.auth.application.dto.response.permission.PermissionResponseCommand;
import iuh.fit.auth.presentation.response.permission.PermissionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionConverter {
    PermissionResponse toResponse(PermissionResponseCommand permission);
}
