package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.user.*;
import iuh.fit.auth.application.dto.request.file.FileMoveRequest;
import iuh.fit.auth.application.dto.request.file.MoveFileRequest;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.client.StoragePort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.service.UserService;
import iuh.fit.auth.application.util.BaseUtil;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.GenderEnum;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserServiceImpl implements UserService {

    private final JwtPort jwtPort;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final UserRegistrationEventPublisher userRegistrationEventPublisher;
    private final StoragePort storagePort;
    private final CurrentUserProvider currentUserProvider;
    private final EmailNotificationPort emailNotificationPort;

    public UserServiceImpl(
            JwtPort jwtPort,
            UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper,
            UserRegistrationEventPublisher userRegistrationEventPublisher,
            StoragePort storagePort,
            CurrentUserProvider currentUserProvider,
            EmailNotificationPort emailNotificationPort
    ) {
        this.jwtPort = jwtPort;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.userRegistrationEventPublisher = userRegistrationEventPublisher;
        this.storagePort = storagePort;
        this.currentUserProvider = currentUserProvider;
        this.emailNotificationPort = emailNotificationPort;
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
    public UserResponseCommand getUserById(String userId) throws ServiceException {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        return userMapper.toUserResponseCommand(user);
    }

    @Override
    public PageResult<User> getAllUsers(PageFilter filter) {
        return userRepository.findAll(filter);
    }

    @Override
    public UserResponseCommand updateUser(String userId, UpdateUserRequestCommand command) throws ServiceException {
        UUID userUUID = BaseUtil.uuid(userId);

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        if (!user.getUsername().equals(command.username())) {
            Optional<User> existingUser = userRepository.findByUsernameOrEmailWithRole(command.username(), "");
            if (existingUser.isPresent() && !existingUser.get().getUserId().equals(userUUID)) {
                throw new ServiceException("Tên đăng nhập đã được sử dụng");
            }
        }

        user.setUsername(command.username());
        user.setFullName(command.fullName());
        user.setPhone(command.phone());
        user.setCountry(command.country());
        user.setGender(GenderEnum.valueOf(command.gender()));

        User savedUser = userRepository.save(user);
        userRegistrationEventPublisher.publishUserUpdated(savedUser);

        return userMapper.toUserResponseCommand(savedUser);
    }

    @Override
    public void updateAvatar(String userId, String tempKey) throws ServiceException {
        UUID userUUID = BaseUtil.uuid(userId);

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        FileMoveRequest confirmRequest = new FileMoveRequest(tempKey);
        List<String> permanentKeys = storagePort.confirmFile(List.of(confirmRequest));

        if (permanentKeys == null || permanentKeys.isEmpty()) {
            throw new ServiceException("Xác nhận tải ảnh đại diện thất bại");
        }

        storagePort.moveFile(new MoveFileRequest(List.of(confirmRequest), 0L));

        String avatarUrl = storagePort.getFileUrl(permanentKeys.getFirst());

        user.setAvatarUrl(avatarUrl);

        userRepository.save(user);
    }

    @Override
    public void updatePassword(UpdatePasswordRequestCommand command) throws ServiceException {
        User currentUser = currentUserProvider.getCurrentUser();

        if (currentUser.getPassword() == null) {
            throw new ServiceException("Tài khoản được đăng nhập bằng mạng xã hội, không thể cập nhật mật khẩu");
        }

        if (command.currentPassword() == null || command.currentPassword().isBlank()) {
            throw new ServiceException("Mật khẩu hiện tại không được để trống");
        }

        if (!jwtPort.matchesPassword(command.currentPassword(), currentUser.getPassword())) {
            throw new ServiceException("Mật khẩu hiện tại không chính xác");
        }

        String hashedNewPassword = jwtPort.hashPassword(command.newPassword());
        currentUser.setPassword(hashedNewPassword);

        User savedUser = userRepository.save(currentUser);

        userRegistrationEventPublisher.publishUserPasswordUpdated(savedUser, command.newPassword());
    }

    @Override
    public void createPassword(CreatePasswordRequestCommand command) throws ServiceException {
        User currentUser = currentUserProvider.getCurrentUser();

        if (currentUser.getPassword() != null && !currentUser.getPassword().isBlank()) {
            throw new ServiceException("Tài khoản đã có mật khẩu, vui lòng sử dụng chức năng đổi mật khẩu");
        }

        if (command.newPassword() == null || command.newPassword().isBlank()) {
            throw new ServiceException("Mật khẩu mới không được để trống");
        }

        String hashedNewPassword = jwtPort.hashPassword(command.newPassword());
        currentUser.setPassword(hashedNewPassword);

        User savedUser = userRepository.save(currentUser);

        userRegistrationEventPublisher.publishUserPasswordUpdated(savedUser, command.newPassword());
    }

    @Override
    public void updatePasswordByAdmin(AdminUpdatePasswordRequestCommand command) throws ServiceException {
        UUID userUUID = BaseUtil.uuid(command.userId());

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        String hashedNewPassword = jwtPort.hashPassword(command.newPassword());
        user.setPassword(hashedNewPassword);

        User savedUser = userRepository.save(user);

        emailNotificationPort.sendPasswordUpdateEmail(savedUser.getEmail(), savedUser.getFullName(), command.newPassword());

        userRegistrationEventPublisher.publishUserPasswordUpdated(savedUser, command.newPassword());
    }

    @Override
    public void verifyUser(String userId, boolean verified) throws ServiceException {
        UUID userUUID = BaseUtil.uuid(userId);

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        user.setStatus(verified ? UserStatusEnum.ACTIVE : UserStatusEnum.INACTIVE);
        User savedUser = userRepository.save(user);

        emailNotificationPort.sendDeactivateEmail(savedUser.getEmail(), savedUser.getFullName(), verified);

        userRegistrationEventPublisher.publishUserVerified(savedUser);
    }
}
