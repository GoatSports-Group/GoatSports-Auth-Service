package iuh.fit.auth.infrastructure.persistence.mapper;

import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PermissionEntityMapper.class})
public interface RoleEntityMapper {

    @Mapping(target = "users", ignore = true)
    RoleEntity createEntity(Role domain);

    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "users", ignore = true)
    void updateEntity(Role domain, @MappingTarget RoleEntity entity);

    Role toDomain(RoleEntity entity);

    RoleEntity toEntity(Role domain);
}
