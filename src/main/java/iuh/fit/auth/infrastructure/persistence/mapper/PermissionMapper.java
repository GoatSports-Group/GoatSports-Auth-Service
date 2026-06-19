package iuh.fit.auth.infrastructure.persistence.mapper;

import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class PermissionMapper {

    public static PermissionEntity toEntity(Permission permission) {
        if (permission == null) return null;
        return PermissionEntity.builder()
                .permissionId(permission.getPermissionId())
                .name(permission.getName())
                .apiPath(permission.getApiPath())
                .method(permission.getMethod())
                .module(permission.getModule())
                .build();
    }

    public static Permission toDomain(PermissionEntity permissionEntity) {
        if (permissionEntity == null) return null;
        return Permission.builder()
                .permissionId(permissionEntity.getPermissionId())
                .name(permissionEntity.getName())
                .apiPath(permissionEntity.getApiPath())
                .method(permissionEntity.getMethod())
                .module(permissionEntity.getModule())
                .build();
    }

    public static Set<Permission> toDomainSet(Set<PermissionEntity> entities) {
        if (entities == null) return Collections.emptySet();
        return entities.stream().map(PermissionMapper::toDomain).collect(Collectors.toSet());
    }

    public static Set<PermissionEntity> toEntitySet(Set<Permission> domains) {
        if (domains == null) return Collections.emptySet();
        return domains.stream().map(PermissionMapper::toEntity).collect(Collectors.toSet());
    }
}
