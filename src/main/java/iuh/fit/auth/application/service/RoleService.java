package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.role.CreateRoleRequestCommand;
import iuh.fit.auth.application.dto.request.role.UpdateRoleRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.Role;

public interface RoleService {
    Role createRole(CreateRoleRequestCommand command) throws ServiceException;

    Role updateRole(UpdateRoleRequestCommand command) throws ServiceException;

    void deleteRole(String roleId) throws ServiceException;

    Role activateRole(String roleId) throws ServiceException;

    Role deactivateRole(String roleId) throws ServiceException;

    Role getRoleById(String roleId) throws ServiceException;

    Role getRoleByName(String name) throws ServiceException;

    PageResult<Role> getAllRoles(PageFilter filter);
}
