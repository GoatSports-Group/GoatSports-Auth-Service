package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.AssignRoleRequestCommand;
import iuh.fit.auth.application.dto.request.user.CreateUserRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.User;

public interface UserService {
    UserResponseCommand createUser(CreateUserRequestCommand command) throws ServiceException;

    UserResponseCommand assignRoleForUser(AssignRoleRequestCommand command) throws ServiceException;

    User getUserById(String userId) throws ServiceException;

    PageResult<User> getAllUsers(PageFilter filter);
}
