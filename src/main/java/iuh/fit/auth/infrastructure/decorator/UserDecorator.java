package iuh.fit.auth.infrastructure.decorator;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.AssignRoleRequestCommand;
import iuh.fit.auth.application.dto.request.user.CreateUserRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.UserService;
import iuh.fit.auth.application.service.impl.UserServiceImpl;
import iuh.fit.auth.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
    @Transactional(readOnly = true)
    public PageResult<User> getAllUsers(PageFilter filter) {
        return userServiceImpl.getAllUsers(filter);
    }
}
