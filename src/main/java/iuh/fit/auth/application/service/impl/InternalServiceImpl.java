package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.service.InternalService;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.RoleEnum;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class InternalServiceImpl implements InternalService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public InternalServiceImpl(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserPermissionsResponseCommand getUserPermissions(String userId) throws ServiceException {
        UUID userUUID;
        try {
            userUUID = UUID.fromString(userId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("Id User không hợp lệ: không phải UUID");
        }

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        Set<String> permissions = (user.getRole() != null && user.getRole().getPermissions() != null)
                ? user.getRole().getPermissions().stream().map(Permission::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        String role = user.getRole() != null ? user.getRole().getName().name() : "";

        return new UserPermissionsResponseCommand(user.getEmail(), role, permissions);
    }

    @Override
    public void assignVenueOwnerRole(String userId) throws ServiceException {
        UUID userUUID;
        try {
            userUUID = UUID.fromString(userId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("Id User không hợp lệ: không phải UUID");
        }

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        if (user.getRole() != null && user.getRole().getName() == RoleEnum.VENUE_OWNER) {
            return;
        }

        Role venueOwnerRole = roleRepository.findByName(RoleEnum.VENUE_OWNER.name())
                .orElseThrow(() -> new ServiceException(
                        String.format("Vai trò %s không tồn tại trong hệ thống", RoleEnum.VENUE_OWNER.getValue())
                    )
                );

        user.setRole(venueOwnerRole);
        userRepository.save(user);
    }

    @Override
    public String getAdmin() throws ServiceException {
        User user = userRepository.findByRole_Name(RoleEnum.ADMIN.name())
                .orElseThrow(() -> new ServiceException("Không tìm thấy người dùng với vai trò ADMIN"));

        return user.getUserId().toString();
    }

    @Override
    public List<UserReportResponseCommand> getAllUserReports() {
        List<User> users = userRepository.findAllUsers();

        return users.stream()
                .map(user -> new UserReportResponseCommand(
                        user.getUserId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole().getName().name(),
                        user.getStatus().getValue(),
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseCommand getUserById(String userId) throws ServiceException {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        return userMapper.toUserResponseCommand(user);
    }
}
