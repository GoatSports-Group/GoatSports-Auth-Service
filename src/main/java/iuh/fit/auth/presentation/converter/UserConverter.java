package iuh.fit.auth.presentation.converter;

import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.presentation.response.user.UserResponse;

public class UserConverter {

    public static UserResponse toResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
            .userId(user.getUserId())
            .email(user.getEmail())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .avatarUrl(user.getAvatarUrl())
            .status(user.getStatus() != null ? user.getStatus().name() : null)
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .role(user.getRole() != null ? UserResponse.UserRole.builder()
                .roleId(user.getRole().getRoleId())
                .name(user.getRole().getName())
                .build() : null)
            .keycloakId(user.getKeycloakId())
            .authProvider(user.getAuthProvider())
            .build();
    }

}
