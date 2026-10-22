package iuh.fit.auth.infrastructure.decorator;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.*;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserSummaryResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.UserService;
import iuh.fit.auth.application.service.impl.UserServiceImpl;
import iuh.fit.auth.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class UserDecorator implements UserService {
    private final UserServiceImpl userServiceImpl;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public UserResponseCommand createUser(CreateUserRequestCommand command) throws ServiceException {
        return userServiceImpl.createUser(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public UserResponseCommand assignRoleForUser(AssignRoleRequestCommand command) throws ServiceException {
        return userServiceImpl.assignRoleForUser(command);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseCommand getUserById(String userId) throws ServiceException {
        return userServiceImpl.getUserById(userId);
    }

    @Override
    public UserResponseCommand getVisibleUser(String userId) throws ServiceException {
        return userServiceImpl.getVisibleUser(userId);
    }

    @Override
    public UserResponseCommand updateUser(String userId, UpdateUserRequestCommand command) throws ServiceException {
        return userServiceImpl.updateUser(userId, command);
    }

    @Override
    @Transactional
    public void updateAvatar(String userId, String tempKey) throws ServiceException {
        userServiceImpl.updateAvatar(userId, tempKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UpdatePasswordRequestCommand command) throws ServiceException {
        userServiceImpl.updatePassword(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPassword(CreatePasswordRequestCommand command) throws ServiceException {
        userServiceImpl.createPassword(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePasswordByAdmin(AdminUpdatePasswordRequestCommand command) throws ServiceException {
        userServiceImpl.updatePasswordByAdmin(command);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> getAllUsers(PageFilter filter) {
        return userServiceImpl.getAllUsers(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseCommand> searchPlayers(String keyword, UUID excludedUserId, int limit) {
        return userServiceImpl.searchPlayers(keyword, excludedUserId, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyUser(String userId, boolean verified) throws ServiceException {
        userServiceImpl.verifyUser(userId, verified);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponseCommand> getUserSummaries(List<UUID> userIds) throws ServiceException {
        return userServiceImpl.getUserSummaries(userIds);
    }
}
