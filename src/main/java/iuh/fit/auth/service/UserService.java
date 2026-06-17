package iuh.fit.auth.service;

import iuh.fit.auth.dto.base.BaseListResponse;
import iuh.fit.auth.dto.response.authentication.UserPermissionResponse;
import iuh.fit.auth.dto.response.authentication.UserResponse;
import iuh.fit.auth.entity.User;
import iuh.fit.auth.exception.ServiceException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface UserService {
    BaseListResponse getAllUsers(Specification<User> spec, Pageable pageable);

    UserResponse assignRoleForUser(String userId, String roleId) throws ServiceException;

    UserPermissionResponse getUserPermissions(String userId) throws ServiceException;

    UserResponse convertToResponse(User user);
}
