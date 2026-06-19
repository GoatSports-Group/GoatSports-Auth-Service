package iuh.fit.auth.application.mapper;

import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;

public class RoleMapper {

    public static RoleEntity toEntity(Role role) {
        if (role == null) return null;
        return RoleEntity.builder()
                .roleId(role.getRoleId())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.isActive())
                .permissions(PermissionMapper.toEntitySet(role.getPermissions()))
                .build();
    }

    public static Role toDomain(RoleEntity roleEntity) {
        if (roleEntity == null) return null;
        return Role.builder()
                .roleId(roleEntity.getRoleId())
                .name(roleEntity.getName())
                .description(roleEntity.getDescription())
                .active(roleEntity.isActive())
                .permissions(PermissionMapper.toDomainSet(roleEntity.getPermissions()))
                .build();
    }
}
