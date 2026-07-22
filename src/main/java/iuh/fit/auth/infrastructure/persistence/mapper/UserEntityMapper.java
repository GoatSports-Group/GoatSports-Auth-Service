package iuh.fit.auth.infrastructure.persistence.mapper;

import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {RoleEntityMapper.class})
public interface UserEntityMapper {

    UserEntity createEntity(User domain);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(User domain, @MappingTarget UserEntity entity);

    User toDomain(UserEntity entity);
}
