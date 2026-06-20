package iuh.fit.auth.presentation.converter;

import iuh.fit.auth.application.dto.response.role.RoleResponseCommand;
import iuh.fit.auth.presentation.response.role.RoleResponse;

import java.util.stream.Collectors;

public class RoleConverter {

    public static RoleResponse toResponse(RoleResponseCommand role) {
        if (role == null) return null;
        return RoleResponse.builder()
            .roleId(role.roleId())
            .name(role.name())
            .description(role.description())
            .active(role.active())
            .permissions(role.permissions() != null
                ? role.permissions().stream().map(PermissionConverter::toResponse).collect(Collectors.toSet())
                : null
            )
            .build();
    }

}
