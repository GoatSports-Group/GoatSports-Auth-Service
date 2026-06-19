package iuh.fit.auth.application.usecase.user;

import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.presentation.response.base.BaseListResponse;
import iuh.fit.auth.presentation.response.user.UserPermissionResponse;
import iuh.fit.auth.presentation.response.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetUserUseCase {
    private final UserRepository userRepository;

    public BaseListResponse getAllUsers(Specification<UserEntity> spec, Pageable pageable) {
        Page<User> page = this.userRepository.findAll(spec, pageable);

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<UserResponse> userResponses = page.getContent().stream()
                .map(UserMapper::toResponse)
                .toList();

        return new BaseListResponse(meta, userResponses);
    }

    public UserPermissionResponse getUserPermissions(String userId) throws ServiceException {
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

        return new UserPermissionResponse(user.getEmail(), role, permissions);
    }

    public UserResponse getUserById(String userId) throws ServiceException {
        UUID userUUID;
        try {
            userUUID = UUID.fromString(userId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("Id User không hợp lệ: không phải UUID");
        }

        User user = userRepository.findById(userUUID)
                .orElseThrow(() -> new ServiceException("Người dùng không tồn tại"));

        return UserMapper.toResponse(user);
    }
}
