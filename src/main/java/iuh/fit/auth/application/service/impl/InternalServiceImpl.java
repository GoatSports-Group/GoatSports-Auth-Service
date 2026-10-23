package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.report.UserReportResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
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
    private final EmailNotificationPort emailNotificationPort;

    public InternalServiceImpl(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper,
                               EmailNotificationPort emailNotificationPort) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.emailNotificationPort = emailNotificationPort;
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

        // Vai trò tạm ngưng thì không mang quyền nào ("Tạm ngưng để tước quyền mà không mất cấu hình").
        Set<String> permissions = (user.getRole() != null && user.getRole().isActive()
                && user.getRole().getPermissions() != null)
                ? user.getRole().getPermissions().stream().map(Permission::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        String role = user.getRole() != null ? user.getRole().getName() : "";

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

        if (user.getRole() != null && RoleEnum.VENUE_OWNER.name().equals(user.getRole().getName())) {
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
        return userRepository.findAllForReport();
    }

    @Override
    public UserResponseCommand getUserById(String userId) throws ServiceException {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        return userMapper.toUserResponseCommand(user);
    }

    private static final int MAX_RECIPIENT_PAGE = 1000;

    @Override
    public List<UUID> findRecipientIds(Set<String> roleNames, UUID after, int limit) throws ServiceException {
        return userRepository.findActiveIdsByRoles(validRoles(roleNames), after,
                Math.max(1, Math.min(limit, MAX_RECIPIENT_PAGE)));
    }

    @Override
    public long countRecipients(Set<String> roleNames) throws ServiceException {
        return userRepository.countActiveByRoles(validRoles(roleNames));
    }

    @Override
    public void sendBookingTicket(UUID userId, EmailNotificationPort.BookingTicketMail ticket) throws ServiceException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));
        emailNotificationPort.sendBookingTicket(user.getEmail(), user.getFullName(), ticket);
    }

    private static Set<String> validRoles(Set<String> roleNames) throws ServiceException {
        if (roleNames == null || roleNames.isEmpty()) throw new ServiceException("Cần ít nhất một vai trò nhận.");
        for (String roleName : roleNames) {
            try {
                RoleEnum.valueOf(roleName);
            } catch (IllegalArgumentException exception) {
                throw new ServiceException("Vai trò không hợp lệ: " + roleName);
            }
        }
        return roleNames;
    }
}
