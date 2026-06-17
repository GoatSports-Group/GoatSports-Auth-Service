package iuh.fit.auth.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import iuh.fit.auth.dto.base.BaseListResponse;
import iuh.fit.auth.dto.request.role.RoleCreateRequest;
import iuh.fit.auth.dto.request.role.RoleUpdateRequest;
import iuh.fit.auth.entity.Role;
import iuh.fit.auth.exception.ServiceException;

public interface RoleService {
    Role createRole(RoleCreateRequest request);

    Role updateRole(RoleUpdateRequest request) throws ServiceException;

    void deleteRole(String roleId) throws ServiceException;

    Role activateRole(String roleId) throws ServiceException;

    Role deactivateRole(String roleId) throws ServiceException;

    Role getRoleById(String roleId) throws ServiceException;

    Role getRoleByName(String name) throws ServiceException;

    BaseListResponse getAllRoles(Specification<Role> spec, Pageable pageable);
}
