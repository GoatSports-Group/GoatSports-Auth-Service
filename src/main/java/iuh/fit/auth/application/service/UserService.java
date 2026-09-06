package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.CreatePasswordRequestCommand;
import iuh.fit.auth.application.dto.request.user.UpdatePasswordRequestCommand;
import iuh.fit.auth.application.dto.request.user.AdminUpdatePasswordRequestCommand;
import iuh.fit.auth.application.dto.request.user.AssignRoleRequestCommand;
import iuh.fit.auth.application.dto.request.user.CreateUserRequestCommand;
import iuh.fit.auth.application.dto.request.user.UpdateUserRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponseCommand createUser(CreateUserRequestCommand command) throws ServiceException;

    UserResponseCommand assignRoleForUser(AssignRoleRequestCommand command) throws ServiceException;

    UserResponseCommand getUserById(String userId) throws ServiceException;

    UserResponseCommand updateUser(String userId, UpdateUserRequestCommand command) throws ServiceException;

    void updateAvatar(String userId, String tempKey) throws ServiceException;

    void updatePassword(UpdatePasswordRequestCommand command) throws ServiceException;

    void createPassword(CreatePasswordRequestCommand command) throws ServiceException;

    void updatePasswordByAdmin(AdminUpdatePasswordRequestCommand command) throws ServiceException;

    PageResult<User> getAllUsers(PageFilter filter);

    List<UserResponseCommand> searchPlayers(String keyword, UUID excludedUserId, int limit);

    void verifyUser(String userId, boolean verified) throws ServiceException;
}
