package iuh.fit.auth.application.usecase;

import iuh.fit.auth.application.dto.response.user.UserPermissionsResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.domain.entity.User;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class InternalUseCase {
    private final UserRepository userRepository;

    public InternalUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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

        String role = user.getRole() != null ? user.getRole().getName() : "";

        return new UserPermissionsResponseCommand(user.getEmail(), role, permissions);
    }

}
