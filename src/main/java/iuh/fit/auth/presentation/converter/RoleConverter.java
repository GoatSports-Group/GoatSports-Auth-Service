package iuh.fit.auth.presentation.converter;

import iuh.fit.auth.application.dto.response.role.RoleResponseCommand;
import iuh.fit.auth.presentation.response.role.RoleResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PermissionConverter.class})
public interface RoleConverter {
    RoleResponse toResponse(RoleResponseCommand role);
}
