package iuh.fit.auth.infrastructure.decorator;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.role.CreateRoleRequestCommand;
import iuh.fit.auth.application.dto.request.role.UpdateRoleRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.RoleService;
import iuh.fit.auth.application.service.impl.RoleServiceImpl;
import iuh.fit.auth.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleDecorator implements RoleService {
    private final RoleServiceImpl roleServiceImpl;

    @Override
    public Role createRole(CreateRoleRequestCommand command) throws ServiceException {
        return roleServiceImpl.createRole(command);
    }

    @Override
    public Role updateRole(UpdateRoleRequestCommand command) throws ServiceException {
        return roleServiceImpl.updateRole(command);
    }

    @Override
    public void deleteRole(String roleId) throws ServiceException {
        roleServiceImpl.deleteRole(roleId);
    }

    @Override
    public Role activateRole(String roleId) throws ServiceException {
        return roleServiceImpl.activateRole(roleId);
    }

    @Override
    public Role deactivateRole(String roleId) throws ServiceException {
        return roleServiceImpl.deactivateRole(roleId);
    }

    @Override
    public Role getRoleById(String roleId) throws ServiceException {
        return roleServiceImpl.getRoleById(roleId);
    }

    @Override
    public Role getRoleByName(String name) throws ServiceException {
        return roleServiceImpl.getRoleByName(name);
    }

    @Override
    public PageResult<Role> getAllRoles(PageFilter filter) {
        return roleServiceImpl.getAllRoles(filter);
    }
}
