package iuh.fit.auth.infrastructure.persistence.mapper;

import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface PermissionEntityMapper {

    @Mapping(target = "roles", ignore = true)
    PermissionEntity createEntity(Permission domain);

    @Mapping(target = "permissionId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updateEntity(Permission domain, @MappingTarget PermissionEntity entity);

    Permission toDomain(PermissionEntity entity);

    Set<Permission> toDomainSet(Set<PermissionEntity> entities);

    Set<PermissionEntity> toEntitySet(Set<Permission> domains);
}
