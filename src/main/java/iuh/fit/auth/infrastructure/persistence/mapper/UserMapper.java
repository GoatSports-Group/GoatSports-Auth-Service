package iuh.fit.auth.infrastructure.persistence.mapper;

import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;

public class UserMapper {

    public static UserEntity toEntity(User user) {
        if (user == null) return null;
        UserEntity entity = UserEntity.builder()
                .userId(user.getUserId())
                .keycloakId(user.getKeycloakId())
                .email(user.getEmail())
                .username(user.getUsername())
                .password(user.getPassword())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .authProvider(user.getAuthProvider())
                .role(RoleMapper.toEntity(user.getRole()))
                .build();
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .userId(entity.getUserId())
                .keycloakId(entity.getKeycloakId())
                .email(entity.getEmail())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .fullName(entity.getFullName())
                .avatarUrl(entity.getAvatarUrl())
                .status(entity.getStatus())
                .authProvider(entity.getAuthProvider())
                .role(RoleMapper.toDomain(entity.getRole()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
