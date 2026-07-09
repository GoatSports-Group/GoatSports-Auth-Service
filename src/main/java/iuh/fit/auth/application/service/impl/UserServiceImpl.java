package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.AssignRoleRequestCommand;
import iuh.fit.auth.application.dto.request.user.CreateUserRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.service.UserService;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;

import java.util.UUID;

public class UserServiceImpl implements UserService {

    private final JwtPort jwtPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserServiceImpl(
            JwtPort jwtPort,
            UserRepository userRepository, RoleRepository roleRepository
    ) {
        this.jwtPort = jwtPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public User createUser(CreateUserRequestCommand command) throws ServiceException {
        boolean existed = userRepository.existsByUsernameOrEmail(command.username(), command.email());
        if (existed) throw new ServiceException("Người dùng đã tồn tại");

        String hashedPassword = jwtPort.hashPassword(command.password());

        User user = new User();
        user.setUsername(command.username());
        user.setPassword(hashedPassword);
        user.setFullName(command.fullName());
        user.setEmail(command.email());
        user.setStatus(UserStatusEnum.ACTIVE);
        user.getAuthProviders().add(AuthProviderEnum.LOCAL);

        if (command.roleId() != null && !command.roleId().isBlank()) {
            Role role = roleRepository.findById(UUID.fromString(command.roleId()))
                    .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
            user.setRole(role);
        }

        return userRepository.save(user);
    }

    @Override
    public UserResponseCommand assignRoleForUser(AssignRoleRequestCommand command) throws ServiceException {
        UUID userUUID;
        UUID roleUUID;
        try {
            userUUID = UUID.fromString(command.userId());
            roleUUID = UUID.fromString(command.roleId());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("Id User hoặc Role không hợp lệ: không phải UUID");
        }

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        Role role = roleRepository.findById(roleUUID)
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));

        user.setRole(role);

        return UserMapper.toUserResponseCommand(userRepository.save(user));
    }

    @Override
    public User getUserById(String userId) throws ServiceException {
        UUID userUUID;
        try {
            userUUID = UUID.fromString(userId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("Id User không hợp lệ: không phải UUID");
        }

        return userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));
    }

    @Override
    public PageResult<User> getAllUsers(PageFilter filter) {
        return userRepository.findAll(filter);
    }
}
