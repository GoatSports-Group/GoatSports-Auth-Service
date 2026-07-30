package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.AssignRoleRequestCommand;
import iuh.fit.auth.application.dto.request.user.CreateUserRequestCommand;
import iuh.fit.auth.application.dto.request.user.UpdateUserRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.User;

public interface UserService {
    UserResponseCommand createUser(CreateUserRequestCommand command) throws ServiceException;

    UserResponseCommand assignRoleForUser(AssignRoleRequestCommand command) throws ServiceException;

    UserResponseCommand getUserById(String userId) throws ServiceException;

    UserResponseCommand updateUser(String userId, UpdateUserRequestCommand command) throws ServiceException;

    void updateAvatar(String userId, String tempKey) throws ServiceException;

    PageResult<User> getAllUsers(PageFilter filter);
}
