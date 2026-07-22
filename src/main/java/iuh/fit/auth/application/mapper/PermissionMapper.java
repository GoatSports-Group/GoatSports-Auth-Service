package iuh.fit.auth.application.mapper;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.permission.PermissionResponseCommand;
import iuh.fit.auth.domain.entity.Permission;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponseCommand toPermissionResponseCommand(Permission permission);

    List<PermissionResponseCommand> toPermissionResponseCommandList(List<Permission> permissions);

    default PageResult<PermissionResponseCommand> toPermissionResponseCommand(PageResult<Permission> permissions) {
        if (permissions == null) return null;
        return new PageResult<>(
                toPermissionResponseCommandList(permissions.items()),
                permissions.total(),
                permissions.page(),
                permissions.pageSize(),
                permissions.totalPages()
        );
    }
}
