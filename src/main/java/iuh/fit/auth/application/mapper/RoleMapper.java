package iuh.fit.auth.application.mapper;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.role.RoleResponseCommand;
import iuh.fit.auth.domain.entity.Role;

import java.util.stream.Collectors;

public class RoleMapper {

    public static RoleResponseCommand toRoleResponseCommand(Role role) {
        return new RoleResponseCommand(
                role.getRoleId(),
                role.getName(),
                role.getDescription(),
                role.isActive(),
                role.getPermissions().stream()
                        .map(PermissionMapper::toPermissionResponseCommand)
                        .collect(Collectors.toSet()
                )
        );
    }

    public static PageResult<RoleResponseCommand> toRoleResponseCommand(PageResult<Role> roles){
        return new PageResult<>(
                roles.items().stream().map(RoleMapper::toRoleResponseCommand).toList(),
                roles.total(), roles.page(), roles.pageSize(), roles.totalPages()
        );
    }

}
