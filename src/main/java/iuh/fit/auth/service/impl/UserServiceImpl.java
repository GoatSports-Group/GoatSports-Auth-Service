package iuh.fit.auth.service.impl;

import iuh.fit.auth.dto.base.BaseListResponse;
import iuh.fit.auth.dto.response.authentication.UserResponse;
import iuh.fit.auth.entity.Role;
import iuh.fit.auth.entity.User;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.repository.RoleRepository;
import iuh.fit.auth.repository.UserRepository;
import iuh.fit.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public BaseListResponse getAllUsers(Specification<User> spec, Pageable pageable) {
        Page<User> page = this.userRepository.findAll(spec, pageable);

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<UserResponse> userResponses = page.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return new BaseListResponse(meta, userResponses);
    }

    @Override
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

        return convertToResponse(user);
    }

    @Override
    public UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .role(new UserResponse.UserRole(
                        user.getRole().getRoleId(),
                        user.getRole().getName()))
                .build();
    }
}
