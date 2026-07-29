package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.AssignRoleRequestCommand;
import iuh.fit.auth.application.dto.request.user.CreateUserRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.service.UserService;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.GenderEnum;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;

import java.util.UUID;

public class UserServiceImpl implements UserService {

    private final JwtPort jwtPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final UserRegistrationEventPublisher userRegistrationEventPublisher;

    public UserServiceImpl(
            JwtPort jwtPort,
            UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper,
            UserRegistrationEventPublisher userRegistrationEventPublisher
    ) {
        this.jwtPort = jwtPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.userRegistrationEventPublisher = userRegistrationEventPublisher;
    }

    @Override
    public UserResponseCommand createUser(CreateUserRequestCommand command) throws ServiceException {
        boolean existed = userRepository.existsByUsernameOrEmail(command.username(), command.email());
        if (existed) throw new ServiceException("Người dùng đã tồn tại");

        String hashedPassword = jwtPort.hashPassword(command.password());

        User user = new User();
        user.setUsername(command.username());
        user.setFullName(command.fullName());
        user.setEmail(command.email());
        user.setGender(GenderEnum.valueOf(command.gender()));
        user.setStatus(UserStatusEnum.ACTIVE);
        user.setPassword(hashedPassword);
        user.getAuthProviders().add(AuthProviderEnum.LOCAL);

        Role role = roleRepository.findByName(RoleEnum.PLAYER.name())
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
        user.setRole(role);

        User createdUser = userRepository.save(user);

        userRegistrationEventPublisher.publishUserRegistered(createdUser, command.password());
        userRegistrationEventPublisher.publishUserVerified(createdUser);

        return userMapper.toUserResponseCommand(createdUser);
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

        return userMapper.toUserResponseCommand(userRepository.save(user));
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
