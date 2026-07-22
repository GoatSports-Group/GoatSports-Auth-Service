package iuh.fit.auth.application.mapper;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.role.RoleResponseCommand;
import iuh.fit.auth.domain.entity.Role;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {

    RoleResponseCommand toRoleResponseCommand(Role role);

    List<RoleResponseCommand> toRoleResponseCommandList(List<Role> roles);

    default PageResult<RoleResponseCommand> toRoleResponseCommand(PageResult<Role> roles) {
        if (roles == null) return null;
        return new PageResult<>(
                toRoleResponseCommandList(roles.items()),
                roles.total(),
                roles.page(),
                roles.pageSize(),
                roles.totalPages()
        );
    }
}
