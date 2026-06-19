package iuh.fit.auth.application.usecase.user;

import iuh.fit.auth.infrastructure.persistence.mapper.UserMapper;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.response.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageUserUseCase {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserResponse assignRoleForUser(String userId, String roleId) throws ServiceException {
        UUID userUUID;
        UUID roleUUID;
        try {
            userUUID = UUID.fromString(userId);
            roleUUID = UUID.fromString(roleId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("Id User hoặc Role không hợp lệ: không phải UUID");
        }

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        Role role = roleRepository.findById(roleUUID)
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));

        user.setRole(role);
        userRepository.save(user);

        return UserMapper.toResponse(user);
    }
}
