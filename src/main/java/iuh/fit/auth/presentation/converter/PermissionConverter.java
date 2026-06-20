package iuh.fit.auth.presentation.converter;

import iuh.fit.auth.application.dto.response.permission.PermissionResponseCommand;
import iuh.fit.auth.presentation.response.permission.PermissionResponse;

public class PermissionConverter {

    public static PermissionResponse toResponse(PermissionResponseCommand permission) {
        if (permission == null) return null;
        return PermissionResponse.builder()
            .permissionId(permission.permissionId())
            .name(permission.name())
            .apiPath(permission.apiPath())
            .method(permission.method())
            .module(permission.module())
            .build();
    }

}
