package iuh.fit.auth.application.mapper;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.permission.PermissionResponseCommand;
import iuh.fit.auth.domain.entity.Permission;

public class PermissionMapper {

    public static PermissionResponseCommand toPermissionResponseCommand(Permission permission) {
        return new PermissionResponseCommand(
                permission.getPermissionId(),
                permission.getName(),
                permission.getApiPath(),
                permission.getMethod(),
                permission.getModule()
        );
    }

    public static PageResult<PermissionResponseCommand> toPermissionResponseCommand(PageResult<Permission> permissions) {
        return new PageResult<>(
                permissions.items().stream().map(PermissionMapper::toPermissionResponseCommand).toList(),
                permissions.total(), permissions.page(), permissions.pageSize(), permissions.totalPages()
        );
    }

}
