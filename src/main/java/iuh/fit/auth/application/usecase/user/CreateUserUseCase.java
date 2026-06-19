package iuh.fit.auth.application.usecase.user;

import iuh.fit.auth.infrastructure.persistence.mapper.UserMapper;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.request.user.CreateUserRequest;
import iuh.fit.auth.presentation.response.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request) throws ServiceException {
        boolean existed = userRepository.existsByUsernameOrEmail(request.getUsername(), request.getEmail());
        if (existed) {
            throw new ServiceException("Người dùng đã tồn tại");
        }

        String hashedPassword = this.passwordEncoder.encode(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(hashedPassword);
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setStatus(UserStatusEnum.ACTIVE);
        user.setAuthProvider(AuthProviderEnum.LOCAL);

        if (request.getRoleId() != null && !request.getRoleId().isBlank()) {
            Role role = roleRepository.findById(UUID.fromString(request.getRoleId()))
                    .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
            user.setRole(role);
        }

        User createdUser = userRepository.save(user);
        return UserMapper.toResponse(createdUser);
    }
}
